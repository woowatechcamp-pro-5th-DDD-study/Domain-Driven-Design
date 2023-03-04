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
### 밸류컬렉션: 한 개 컬럼 매핑
### 밸류를 이용한 ID 매핑
### 별도 에티블에 저장하는 밸류 매핑
### 밸류 컬렉션을 @Entity로 매핑하기
### ID 참조와 조인 테이블을 이용한 단방향 M-N 매핑

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
