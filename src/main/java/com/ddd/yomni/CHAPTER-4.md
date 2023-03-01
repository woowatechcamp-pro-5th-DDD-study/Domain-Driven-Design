# Chapter 4. 리포지터리와 모델 구현

---

<!-- TOC -->

* [Chapter 4. 리포지터리와 모델 구현](#chapter-4-리포지터리와-모델-구현)
    * [4.1 JPA를 이용한 리포지터리 구현](#41-jpa를-이용한-리포지터리-구현)
        * [4.1.1 모듈 위치](#411-모듈-위치)
        * [4.1.2 리포지터리 기본 기능 구현](#412-리포지터리-기본-기능-구현)
    * [4.2 스프링 데이터 JPA를 이용한 리포지터리 구현](#42-스프링-데이터-jpa를-이용한-리포지터리-구현)
    * [4.3 매핑 구현](#43-매핑-구현)
        * [4.3.1 엔티티와 밸류 기본 매핑 구현](#431-엔티티와-밸류-기본-매핑-구현)
        * [4.3.2 기본 생성자](#432-기본-생성자)
        * [4.3.3 필드 접근 방식 사용](#433-필드-접근-방식-사용)
        * [4.3.4 AttributeConverter를 이용한 밸류 매핑 처리](#434-attributeconverter를-이용한-밸류-매핑-처리)

<!-- TOC -->

---

## 4.1 JPA를 이용한 리포지터리 구현

- 도메인 모델과 리포지터리를 구현할 때 선호하는 기술은 `JPA`
- 특히 RDBMS 를 사용하는 경우, ORM 만한 것이 없다.
- 따라서, ORM 표준인 JPA 를 이용한 리포지터리와 애그리거트를 구현하는 방법에 대해 살펴보자

### 4.1.1 모듈 위치

- 리포지터리 인터페이스는 도메인 영역에 속함
- 리포지터리를 구현한 클래스는 인프라스터럭처 영역에 속한다.

![DIP.png](images/DIP.png)

- but, 팀 표준에 따라 구현 클래스를 domain.impl 과 같은 패키지에 위치시킬 수 있음
    - 좋은 설계 원칙은 아니므로, 가능하면 인프라스터럭처 영역에 위치시켜 의존을 낮춰야 한다

### 4.1.2 리포지터리 기본 기능 구현

- ID로 애그리거트 조회하기
- 애그리거트 저장하기

```java
public interface OrderRepository {
    Optional<Order> findById(OrderNo id);

    void save(Order order);
}
```

```java
import com.ddd.programmersjk.domain.order.OrderRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class JpaOrderRepository implements OrderRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Order> findById(OrderNo id) {
        return entityManager.find(Order.class, id);
    }

    @Override
    public void save(Order order) {
        entityManager.persist(order);
    }
}
```

- `스프링 데이터 JPA` vs `스프링 + JPA` 에서 실제 구현은 주로 `스프링 데이터 JPA`를 주로 사용하므로  
  결과적으론, JPA 에서는 infrastructure 영역의 구현은 거의 없다.

---

## 4.2 스프링 데이터 JPA를 이용한 리포지터리 구현

- 스프링과 JPA를 같이 사용하는 경우 --> 스프링 데이터 JPA를 주로 사용
- 스프링 데이터 JPA 는 인터페이스 명만 잘 정의해주면, 스프링이 인터페이스를 구현한 객체를 만들어 스프링 빈으로 자동으로 등록해준다.  
  (infrastructure 작성 번거로움을 줄여줌)  
  --> 가만히 생각해보면, 결과적으로 repository 구현체는 entityManager의 적절한 메소드(find, save, persist ...) 호출하는 역할만 수행한다.  
  --> 추상화가 명확하고, 구현체도 비교적 패턴이 있으니 스프링에서도 이를 쉽게 지원할 수 있었던 듯

---

## 4.3 매핑 구현

### 4.3.1 엔티티와 밸류 기본 매핑 구현

**기본규칙**

- 애그리거트 루트는 엔티티이므로 `@Entity`로 매핑 설정한다.

**엔티티와 밸류가 있다면?**

- 밸류는 `@Embeddable`로 매핑
- 밸류 타입 프로퍼티는 `@Enbedded`로 매핑

### 4.3.2 기본 생성자

- 이론적으로 **불변타입의 객체에는 기본 생성자를 선언할 필요가 없다.**
- 다만, JPA는 기술적인 제약(프록시 객체 생성을 위해) 기본 생성자가 반드시 필요하다.
    - 기본 생성자를 protected 로 지정하여, 다른 코드에서 불완전한 객체를 생성하지 못하도록 한다.
    - [어라? 기본생성자가 없어도 코드가 돌아가던데요?](https://www.inflearn.com/questions/105043/%EA%B8%B0%EB%B3%B8-%EC%83%9D%EC%84%B1%EC%9E%90%EC%97%90-%EA%B4%80%ED%95%B4-%EC%A7%88%EB%AC%B8%EB%93%9C%EB%A6%BD%EB%8B%88%EB%8B%A4)
        - 하이버네이트(HibernatePersistenceProvider) 는 동적으로 PersistenceProvider 구현체를 찾는 로직이 있어서 그렇다.
        - 다만, JPA 스펙에선 기본 생성자를 요구하니 그냥 요구사항대로 구현하라..
    - [최근 cglib / objenesis 같은 프록시 라이브러리들이 많을텐데, 왜 안쓸까요?](https://www.inflearn.com/questions/482663/no-args-constructor%EB%A5%BC-%EA%B0%9C%EB%B0%9C%EC%9E%90%EC%97%90%EA%B2%8C-%EA%B0%95%EC%A0%9C%ED%95%98%EB%8A%94-%EC%9D%B4%EC%9C%A0)
        - JPA 만들땐 그런거 없었음

### 4.3.3 필드 접근 방식 사용

- 메서드 방식으로 접근하려면 JavaBeans 관례대로 getter/setter 로 접근한다.
- get / set 을 이용한 방식의 단점
    - 도메인의 의도가 사라짐
    - 객체가 아닌 데이터 기반으로 엔티티를 구현할 가능성 높음
    - setter는 캡슐화를 깨는 원인이 됨
- 의도가 드러나는 기능을 제공해야 한다 set --> change
- **밸류타입을 불변으로 구현하면 setter 자체가 필요 없음**
- 엔티티도 웬만하면 불변으로 구현하려고 노력할 것..! (필요한 경우에만 객체에 알맞는 기능을 추가)
- 객체가 제공할 기능 중심으로 엔티티를 구현하게끔 유도하려면 JPA 매핑 처리를 프로퍼티 방식이 아닌 필드 방식으로 선택해서 불필요한 get/set 메서드를 구현하지 말아야 한다.

### 4.3.4 AttributeConverter를 이용한 밸류 매핑 처리

- 여러 개의 프로퍼티를 DB의 한 개 칼럼에 매핑해야 할 때 `AttributeConverter` 사용

```java
public class EmailSetConverter implements AttributeConverter<EmailSet, String> {
    @Override
    public String convertToDatabaseColumn(EmailSet attribute) {
        if (attribute == null) return null;
        return attribute.getEmails().stream()
                .map(Email::getAddress)
                .collect(Collectors.joining(","));
    }

    @Override
    public EmailSet convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String[] emails = dbData.split(",");
        Set<Email> emailSet = Arrays.stream(emails)
                .map(Email::new)
                .collect(toSet());
        return new EmailSet(emailSet);
    }
}

    // In Member
    @Column(name = "emails")
    @Convert(converter = EmailSetConverter.class)
    private EmailSet emails;
```

### 4.3.5 밸류 컬렉션: 별도 테이블 매핑

- Order에서는 OrderLine 의 List를 프로퍼티로 갖고 있다.

- [@ElementCollection vs @*ToMany](https://medium.com/nerd-for-tech/elementcollection-vs-onetomany-in-hibernate-7fb7d2ac00ea)
    - 도메인은 id를 가지고 있습니다. (--> 이게 별말 아닌것처럼 느껴지지만, id를 갖고 있다는 것은 엔티티가 될 자격이 있고, 애그리거트 루트가 될 수도 있다는 것을 의미합니다.)
    - 밸류타입은 id를 갖고 있지 않습니다.(--> line_idx가 마치 id처럼 보이기는 하지만, 인덱싱를 위한 컬럼일뿐 id는 아닙니다.)

```java

@Entity
@Table(name = "purchase_order")
@Access(AccessType.FIELD)
public class Order {
    @EmbeddedId
    private OrderNo number;

    @Version
    private long version;

    @Embedded
    private Orderer orderer;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_number"))
    @OrderColumn(name = "line_idx")
    private List<OrderLine> orderLines;
    ...
}

@Embeddable
public class OrderLine {
    @Embedded
    private ProductId productId;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "price")
    private Money price;

    @Column(name = "quantity")
    private int quantity;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "amounts")
    private Money amounts;

    protected OrderLine() {
    }

    public OrderLine(ProductId productId, Money price, int quantity) {
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;
        this.amounts = calculateAmounts();
    }

    private Money calculateAmounts() {
        return price.multiply(quantity);
    }

    public ProductId getProductId() {
        return productId;
    }

    public Money getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getAmounts() {
        return amounts;
    }
}
```

### 4.3.6 밸류 컬렉션: 한 개 칼럼 매핑

밸류 컬렉션을 별도 테이블이 아닌 한 개 칼럼에 저장해야할 때가 있다.  
ex) 도메인 모델에는 이메일 주소 목록을 Set으로 보관하고, DB에는 한 개 칼럼에 콤마로 구분해서 저장해야 할 때

```java
// EmailSet class
public class EmailSet {
    private Set<Email> emails = new HashSet<>();

    public EmailSet(Set<Email> emails) {
        this.emails.addAll(emails);
    }

    public Set<Email> getEmails() {
        return Collections.unmodifiableSet(emails);
    }
}
```

```java
public class EmailSetConverter implements AttributeConverter<EmailSet, String> {
    @Override
    public String convertToDatabaseColumn(EmailSet attribute) {
        if (attribute == null) return null;
        return attribute.getEmails().stream()
                .map(Email::getAddress)
                .collect(Collectors.joining(","));
    }

    @Override
    public EmailSet convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String[] emails = dbData.split(",");
        Set<Email> emailSet = Arrays.stream(emails)
                .map(Email::new)
                .collect(toSet());
        return new EmailSet(emailSet);
    }
}
```

### 4.3.7 밸류를 이용한 ID 매핑

**식별자라는 의미를 부각시키기 위해 식별자 자체를 밸류 타입으로 만들 수도 있다.**  
ex) `OrderNo`, `MemberId` 같은 타입이 그 예시

만약 이런 ID를 밸류로 정의한다면, `@Id` 대신 `@EmbeddedId`로 사용해야 한다.

```java

@Entity
@Table(name = "purchase_order")
@Access(AccessType.FIELD)
public class Order {
    @EmbeddedId
    private OrderNo number;
    ...
}
```

```java

@Embeddable
public class OrderNo implements Serializable {
    @Column(name = "order_number")
    private String number;

    protected OrderNo() {
    }
    ...
}
```

### 4.3.8 별도 테이블에 저장하는 밸류 매핑

**애그리거트에서 루트 엔티티를 제외하면 대부분은 밸류이다.**  
**따라서 만약 루트 엔티티 외에 또 다른 엔티티가 있으면 진짜 엔티티 인지 의심해봐야한다.**

단지, 별도 테이블에 데이터를 저장한다고 해서 엔티티인 것은 아니다. (Id가 없으면 별도 테이블에 저장되어도 엔티티가 아니다)

- Product : Review 관계 ==> 각각 다른 애그리거트

`@AttributeOverride`를 사용하면, 해당 밸류 데이터가 저장된 테이블 이름을 지정한다.  
`SecondaryTable` 를 사용하면 아래 코드를 실행할 때 두 테이블을 조인해서 데이터를 조회한다.

```java
// @SecondaryTable로 매핑된 article_content 테이블을 조인
Article article=entityManager.find(Article.class,1L);
```

### 4.3.9 밸류 컬렉션을 @Entity로 매핑하기

(대부분이 이 경우에 해당하지 않을까 싶습니다.)  
개념적으론 밸류이지만구현 기술의 한계나 팀 표준 때문에 `@Entity`를 사용해야 할 때가 있다.

![entity-value-mapping.png](images/entity-value-mapping.png)

JPA는 `@Embeddable` 타입의 클래스 상속 매핑을 지원하지 않는다.  
따라서, 상속 구조를 갖는 밸류 타입을 사용하려면 @Embeddable 대신 @Entity를 이용해서 상속 매핑으로 처리해야 한다.

```java

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "image_type")
@Table(name = "image")
public abstract class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(name = "image_path")
    private String path;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    protected Image() {
    }

    public Image(String path) {
        this.path = path;
        this.uploadTime = LocalDateTime.now();
    }

    protected String getPath() {
        return path;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public abstract String getUrl();

    public abstract boolean hasThumbnail();

    public abstract String getThumbnailUrl();

}
```

> 생각해보자) 엔티티와 밸류타입 테이블의 관계가 개념적이고 논리적으론 이해 했습니다.  
> 우리는 주로 논리적으론 밸류 타입이어도 편의를 위해 @Entity 로 매핑하곤 했습니다.  
> 지극히 개념적인 차원에서 DDD를 도입한다 하면 @Entity 이 하나의 어노테이션이 어떤 의미를 갖는 지 한번 다시 생각해 볼 필요는 있을 것 같네요!

### 4.3.10 ID 참조와 조인 테이블을 이용한 단방향 M-N 매핑

3장에서는 애그리거트 간 집합 연관은 성능 상의 이유로 피해야 한다고 했다.  
하지만, 요구사항을 구현하는 데 집합 연관을 사용하는 것이 유리하다면 **ID 참조를 이용한 단방향 집합 연관을 적용해 볼 수 있다.**

- Product : Category case

```java

@Entity
@Table(name = "product")
public class Product {
    @EmbeddedId
    private ProductId id;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"))
    private Set<CategoryId> categoryIds;
    ...
}
```

- 이 코드는 Product --> Category 로의 단방향 M:N 관계를 ID 참조 방식으로 구현한 것
- Id 참조를 이용한 애그리거트 간 단방향 M:N 관계는 밸류 컬렉션 매핑과 동일한 방식으로 설정한다.
- 차이점은 집합의 값 밸류 대신 연관을 맺는 식별자가 온다
- Id를 사용하면, 영속성 전파나 로딩 전략등을 고민하지 않아도 된다.

---

## 4.4 애그리거트 로딩 전략

- 이론적으로는 애그리거트에 속한 객체가 모두 모여야 애그리거트는 완전한 하나가 된다.
- 하지만, 이를 위해 즉시로딩을 사용하는 것은 굉장히 무모한 일이다.(N+1 problem 등등....)

JPA 의 기술적인 차원에서는

- 모든 관계에서 지연 로딩(Lazy Loading) 전략을 사용
- 필요한 경우 Fetch join 을 사용

하는 방식이 있다.

이론적 차원에서 애그리거트가 완전히 하나여야 하는 이유는 아래 두 가지이다.

1. 상태를 변경하는 기능을 실행할 때 애그리거트 상태가 완전해야 하기 때문이다.
2. 표현 영역에서 애그리거트의 상태 정보를 보여줄 때 필요하기 때문이다.

이 중 두번째는 조회 전용 기능과 모델을 구현하는 방식을 사용하는 것이 더 유리하기 때문에  
애그리거트의 완전한 로딩과 관련된 문제는 상태 변경과 더 관련이 있다.

상태 변경 기능을 실행하기 위해 조회 시점에 즉시 로딩을 이용해서 애그리거트를 완전한 상태로 로딩할 필요는 없다.  
JPA는 트랜잭션 범위 내에서 지연 로딩을 허용하기 때문에 **상태를 변경하는 시점에 필요한 구성요소만 로딩해도 문제가 되지 않는다.**

정리해보면,

1. 애그리거트는 상태를 변경하는 기능을 실행할 때 완전해야 하기 때문에 조회하는 시점에 완전히 하나여야 한다.
2. JPA 에서는 트랜잭션 범위 내에선 지연 로딩을 허용하기 때문에,  
   즉! 상태를 변경하는 시점에 변경에 필요한 요소들만 로딩해도 문제 없음

다시한번 후려쳐보면,

1. 애그리거트는 상태를 변경해야 하는 시점이 있다.
2. JPA를 사용하면 상태 변경 시, 상태 변경에 필요한 요소들만 그 시점에 로딩할 수 있다.

그리고, 일반적인 애플리케이션은 상태를 변경하는 것 보단, 조회의 빈도가 훨~~~씬 많다.  
그러므로 상태 변경을 위해 지연 로딩을 사용할 때 발생하는 추가 쿼리로 인한 실행 속도 저하는 보통 문제가 되지 않는다.  
반면, 조회에선 가능하다면 지연 로딩이 되지 않도록 해야 한다 (fetch join)

---

## 4.5 애그리거트의 영속성 전파

**애그리거트가 완전한 상태여야 한다**는 것은 애그리거트 루트를 조회할 때 뿐만 아니라,  
저장하고 삭제할 때도 하나로 처리해야 함을 의미한다.

- 저장 메서드는 애그리거트 루트만 저장하면 안되고, 애그리거트에 속한 모든 객체를 저장해야 한다.
- 삭제 메서드는 애그리거트 루트뿐만 아니라 애그리거트에 속한 모든 객체를 삭제해야 한다.

`@Embeddable` 매핑 타입은 함께 저장되고 삭제되므로 cascade 속성을 설정하지 않아도 된다.  
반면, 애그리거트에 속한 `@Entity` 타입에 대한 매핑은 cascade 속성을 사용해서 저장과 삭제 시에 함께 처리되도록 설정해야 한다.

@OneTo* 는 cascade 속성 기본값이 없으므로, `PERSIST` 나 `REMOVE` 를 설정한다.

```java
@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE},
        orphanRemoval = true)
@JoinColumn(name = "product_id")
@OrderColumn(name = "list_idx")
private List<Image> images=new ArrayList<>();
```

---

## 4.6 식별자 생성 기능

식별자는 아래 3 가지 경우 중 하나로 생성한다.

1. 사용자가 직접 생성
2. 도메인 로직으로 생성
3. DB를 이용한 일련번호 사용

### 사용자가 직접 생성

이메일주소 처럼 식별자를 사용자가 생성하는 경우, 도메인 영역에서 식별자 생성 기능을 구현할 필요가 없다.

### 도메인 로직으로 생성

생성 규칙이 별도로 존재한다면, 이는 도메인 규칙이므로 도메인 서비스를 만들어 도메인 영역에 위치시킬 수 있다.  
혹은, 리포지터리에서 식별자를 생성하는 메서드를 추가하고, 리포지터리 구현 클래스에서 알맞게 구현하며 된다.

```java
public interface ProductRepository {
    ... // save() 등 다른 메서드

    // 식별자를 생성하는 메서드
    ProductId nextId();
}
```

### DB를 이용한 일련번호 사용

```java
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    ...
}
```

자동증가 칼럼은 DB의 insert 쿼리를 실행해야 식별자가 생성된다.  
따라서, 도메인 객체를 생성하는 시점에는 식별자를 알 수 없고, 도메인 객체를 저장한 뒤에 식별자를 구할 수 있다.

```java
import com.ddd.yomni.myshop.board.domain.Article;
import com.ddd.yomni.myshop.board.domain.ArticleContent;
import com.ddd.yomni.myshop.board.domain.ArticleRepository;

public class WriteArticleService {
    private ArticleRepository articleRepository;

    public Long write(NewArticleRequest req) {
        Article article = new Article("제목", new ArticleContent("content", "type"));
        article = articleRepository.save(article); // 식별자 생성
        return article.getId();
    }
}
```

---

## 4.7 도메인 구현과 DIP

이제까지 구현했던 방식은 2장에서 다루었던 DIP(의존성 역전의 원칙)을 철저하게 위배하고 있다.

Article 엔티티를 다시 보면, @Entity, @Table, @Id, @Column 등 JPA라는 인프라 영역의 데이터 접근 기술에 의존하고 있기 때문이다.

```java

@Entity
@Table(name = "article")
@SecondaryTable(
        name = "article_content",
        pkJoinColumns = @PrimaryKeyJoinColumn(name = "id")
)
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    ...
}
```

그럼 어떻게 해야할까??? --> DIP 적용

![DIP_repository.png](images/DIP_repository.png)

**이렇게 하면 더이상 도메인이 구현기술에 의존하지 않는다.**

장점

- 다른 구현 기술로 변경하기 매우 쉽다

단점

- 복잡도가 다소 높아질 수도 있다.(맨처음에 익숙하지 않으면 이해하기 어려움)
- 객체가 많이 생성될 가능성이 있다.
- 계층간 의존성을 계속해서 파악해야 하기 때문에 개발 편의성이 떨어질 수 있다.

여기선 적절한 트레이드오프가 필요하다. 대부분은 JPA 전용 애너테이션을 도메인 모델에 적용하고 있는데,  
이런 점은 개발 편의성과 실용성을 위해서이지만, DIP를 지키고 있지는 않다는 것을 항상 알고 있어야 한다.

---

## 생각해보자

- JpaRepository 인터페이스를 그대로 사용했던 저를 되돌아보게 되는 것 같습니다. + 기계적으로 @Entity 를 선언하던 모습도 반성
- '엔티티와 밸류' 로 돌아가서 생각해보면, 엔티티는 식별자를 가진다. 라는 특징이 있다고 했습니다.
    - 사실 이 부분에서 '당연한거 아니야?' 라는 안일한 생각을 가지고 있었는데, 애그리거트 관점에서 다시 생각해보니
    - 식별자를 가진다는게 어떤 의미인지 다시금 생각하게 되는 것 같네요
- 구현 방법에 대해선 뭐 볼게 있겠나 싶었는데, ORM - 객체지향 - DDD 를 연결지어서 생각해보면,   
  ElementCollection 등의 구현 기술이 생소하지만 그 의미가 명확한 것 같습니다.
- 추상화 정도와 유연성에 대해
    - 보통 추상화 정도가 높으면 유연하다곤 하지만,
    - 추상화 구현에 대한 책임이 개발자에게 있기 때문에 쓸데없는 객체가 많이 생기게 될 부작용(?)도 간과해선 안된다.

### 커피들 한잔 따라오시죠...

- 근황토크