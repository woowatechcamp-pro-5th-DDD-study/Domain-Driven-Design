# 4. 리포지터리와 모델 구현
- 애그리거트를 어떤 저장소에 어떻게 저장해야하지? 에 대한 결정이 필요한데,
- 이 책에선 RDBMS를 객체기반의 도메인 모델을 관계형 데이터베이스에 매핑을 처리하는 ORM 중 JPA 기준으로 설명을 하고 있다.
## JPA를 이용한 리포지터리 구현
- SpringDataJPA를 상속받은 Repository인터페이스는 도메인 영역에 속한다.
- Repository를 구현한 클래스는 인프라스트럭처에 속한다.
## 매핑 구현
- 애그리거트루트는 엔티티이므로 @Entity로 매핑 설정
- 한 테이블에 엔티티와 밸류 데이터가 같이 있다면
  - 밸류는 @Embeddable로 매핑 설정
  - @AttributeOverride 어노테이션 활용 -> 실무에서는 잘 안쓴다는데 ?
- JPA에서 @Entity와 @Embeddable로 클래스를 매핑하려면 기본 생성자가 필요
  - 아마 생성 후 Refelction 시를 위함으로 알고 있는데... -> 추가 공부 필요
  - 기본 생성자는 JPA 프로바이더가 객체를 생성할 때만 사용
    - 다른 코드에서 사용 시 문제가 발생할 수 있으므로 protected 로 지정
- getter/setter 추가 시에 발생하는 trade off에 대해서 고민해보자
  - 네이밍이라도 바꿔서 쓰도록 권장
- 여러 값을 하나의 컬럼에 매핑하여 처리하는 경우는 ?
  - 아래와 같이 AttributeConverter 활용하여 밸류 매핑 처리
~~~java
public class Money {

    private int value;

    public Money(int value) {
        this.value = value;
    }
}
~~~
~~~java
public class MoneyConverter implements AttributeConverter<Money, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Money money) {
        return money == null ? null : money.getValue();
    }

    @Override
    public Money convertToEntityAttribute(Integer value) {
        return value == null ? null : new Money(value);
    }
}
~~~
- 위 AttributeConverter 을 활용
~~~java
@Entity
@Table(name = "product")
public class Product {
    @EmbeddedId
    private ProductId id;

    private String name;

    @Convert(converter = MoneyConverter.class)
    private Money price;
    
    protected Product() {
    }
}
~~~
### 밸류컬렉션: 별도 테이블 매핑
- @ElementCollection 과 @CollectionTable `함께` 활용
- @OrderColumn (주문의 Order가 아님) 사용하면 지정한 컬럼에 리스트의 인덱스 값을 저장
### 밸류컬렉션: 한 개 컬럼 매핑
- AttributeConverter
### 밸류를 이용한 ID 매핑
- 식별자라는 의미를 부각하기 위해 식별자 자체를 밸류 타입으로 만듦
- @EmbeddedId 어노테이션 사용
- JPA에서 식별자 타입은 Serializable 타입이어야 함
### 별도 에티블에 저장하는 밸류 매핑
- 루트 엔티티 외에 또 다른 엔티티가 있다면 진짜 엔티티인지 의심해봐야 함
- 밸류가 아니라 엔티티가 확실하다면  다른 애그리거트가 아닌지 확인해야 함 -> 독자적인 생명주기를 가지는지? -> 상품과 리뷰
- 애그리거트에 속한 객체가 밸류인지 엔티티인지 어떻게 구분할까 ? -> `고유 식별자`를 갖는지 확인
- Article-Content 관계는 1대1로 매핑되지만, Article의 PK로 묶이니 밸류이다.
  - 이에 Entity가 아닌 @Embeddable로 매핑
  - @SecondaryTable, @AttributeOverride 적용 
    -> 삭제나 저장 시 하나의 생명주기로 관리할 수 있음 -> 조회할 때도 무조건 조인해서 가져옴
  - 하지만 하나의 테이블만 조회하고자 한다면 ? -> 조회 전용 기능을 구현 -> 5, 11장에서 살펴본다고 함 ㅎㅎ
### 밸류 컬렉션을 @Entity로 매핑하기
- 예를 Product - Image 의 관계를 설명 함 - Image는 InternalImage / ExternalImage가 존재
- 하지만 JPA의 @Embeddable은 상속을 제공해주기 않음
- 이에 @Inheritance(strategy는 single_table) / @DiscriminatorColumn 어노테이션을 이용하여 타입 구분용으로 사용할 컬럼 지정
  - Image는 Value 이므로 독자적인 생명주기를 갖고 있지 않고, Product에 와전 의존
  - Product 기준 OneToMany로 매핑 처리 Cascade로 영속성 처리, orphanRemoval=true 까지 설정하여 생명주기 관리
- 다만 이러한 엔티티간의 OneToMany 관계이면 n+1 문제 등이 발생할 수 있음 -> 설명으로는 Clear 메서드를 예시로 둠  
-> 하이버네이트는 @Embeddable 타입에 대한 컬렉션의 Clear() 메서드를 호출하면 컬렉션에 속한 객체를 로딩하지 않고 한 번의 Delete 쿼리로 삭제 처리
- 애그리거트의 특성을 유지하면서 이 문제를 해소하려면 상속을 포기하고 @Embeddable로 매핑된 단일 클래스로 구현 -> DiscriminatorColumn 대신 if/else 활용
### ID 참조와 조인 테이블을 이용한 단방향 M-N 매핑
- 집합 연관은 성능 상의 이유로 피해야 한다고 3장에서 설명했지만, 그래도 써야한다면 ?
- ID 참조를 이용한 단방향 집합 연관을 적용해보자
- 단방향 M-N 연관을 ID 참 참조 방식을 구현 -> 밸류 대신 연관을 맺는 식별자가 오는 것이 포인트
- @ElemnetCollection 을 이용하기 떄문에 Product를 삭제할 때 매핑에 사용한 조인 테이블의 데이터도 함께 삭제
- 애그리거트를 직접 참조하는 방식을 사용했다면 영속성 전파나 로딩 전략을 고민해야 하는데 ID 참조 방식을 사용함으로써 고민을 없앨 수 있음

## 회고
정리한 내용이 Push가 제대로 되지 않고 날라가서 정리하려 했던 키워드 및 회고식으로 우선 정리...
~~~text
     Chapter4의 주요 내용은 SpringDataJPA 사용하고 있으며 도메인주도설계 관점에서,
     '객체그래프를 RDBMS 구조로 어떻게 테이블과 매핑을 해야할까' 라는 주제의 챕터였다.
     
     단순 엔티티 -> Repository -> Table 구성이 아니라 복잡한 객체 관계를 어떻게 처리할 것인가에 대한 예시를 설명해주었다.
     간단한 조회는 Void, Optional를 활용한다 부터 값포장한 클래스를 엔티티의 필드로 사용하는 경우, 하나의 컬럼으로 어떻게 매핑할지,
     그에 따른 성능이슈는 무엇이 발생할 수 있는지(대다수 OneToMany 상황의 Lazy, Eager 이슈)에 대한 내용이었다. 
     
     객체(엔티티)는 동일한 생명주기를 갖고 있지만, 다수의 클래스를 참조하는 경우
     - @Entity 로 묶을지, @Embeddable 로 묶을지에 대한 이슈
     - 여러 테이블의 구조인 경우, 각각의 테이블은 단순 참조용 키인지, 유니크한 키인지에 대한 설명(Article - Content)
     - OneToMany로 묶었을 때 One에서 Many쪽을 삭제 시 성능에 대한 이슈
    
     사실 이번 스터디를 참석하지 못하지만, 질문하고 싶은 내용이 많았던 챕터였다.
     이번 Chapter에서 언급된 어노테이션을 자주 사용하는지? 이슈는 없는지?
     
     그리고 다시 회고식으로 정리해보니, 읽어봤을 때는 쉽게 읽혔지만 다시 생각해보니 설계 시 매우 중요한 개념으로 생각되어,
     어차피 정리한거 날라간 김에 다시 정리하고 공부해야겠다.     
~~~
