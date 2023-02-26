# 리포지터리와 모델 구현

## JPA를 이용한 리포지터리 구현
* 도메인 모델과 리포지터리를 구현할 떄 선호하는 기술을 꼽으면 JPA를 들 수 있다.
* 데이터 보관소로 RDBMS를 사용할 때 객체 기반의 도메인 모델과 관계형 데이터 모델 간의 매핑을 처리하는 기술로 ORM 만한 것이 없다.

### 모듈 위치
* 리포지터리 인터페이스는 애그리거트와 같이 도메인 영역에 속하고, 리포지터리를 구현한 클래스는 인프라스트럭처 영역에 속한다.
  <br/><img src="./그림 4.1.png">

### 리포지터리 기본 기능 구현
* 리포지터리가 제공하는 기본 기능
  * ID로 애그리거트 조회하기
  * 애그리거트 저장하기
    ```
    public interface OrderRespository {
        Order findById(OrderNo no);
        void save(Order order);
    }
    ```
* 인터페이스는 애그리거트 루트를 기준으로 작성한다.
* 애그리거트를 조회하는 기능의 이름을 작성할 때 널리 사용되는 규칙은 `findBy프로퍼티이름(프로퍼티 값)` 형식이다.
* 애그리거트를 조회할 때 존재하지 않으면 null을 리턴하는데 null을 사용하고 싶지 않으면 Optional을 사용한다.
```
Optional<Order> findById(OrderNo no);
```
* 인터페이스를 구현한 클래스는 JPA의 EntityManger를 이용해서 기능을 구현한다.
```
// 스프링 데이터 JPA를 사용하지 않은 코드로 실질적으로 리포지터리 인터페이스를 구현한 클래스를 직접 작성할 일은 거의 없다.
@Repository
public class JpaOrderRepository implements OrderRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Order findById(OrderNo id) {
        return entityManager.find(Order.class, id); 
    }
    
    @Override
    public void save(Order order) {
        entityManager.persist(order);
    }
}
```
* 애그리거트를 수정한 결과를 저장소에 반영하는 메서드를 추가할 필요는 없는데 JPA에서는 따로 변경 사항을 저장하지 않아도 [더티 체킹](https://jojoldu.tistory.com/415)을 통해서 `변화가 있는 모든 엔티티 객체` 데이터베이스에 자동 반영해준다.
* ID가 아닌 다른 조건으로 애그리거트를 조회할 때는 findBy 뒤에 조건 대상이 되는 프로퍼티 이름을 붙인다.
* ID 외에 다른 조건으로는 애그리거트를 죄회할 때에는 JPA의 Criteria나 JPQL을 사용할 수 있다
* 애그리거트의 삭제 메서드는 애그리거트 객체를 파라미터로 전달 받는다.

## 스프링 데이터 JPA를 이용한 리포지터리 구현
* 스프링과 JPA를 함꼐 사용할 때는 스프링 데이터 JPA를 사용한다.
* 지정한 규칙에 맞게 인터페이스를 정의하면 구현체를 만들어 스프링 Bean으로 등록해준다.
```
// 인터페이스 등록
public interface OrderRepository extends Repository<Order, OrderNo> {
	Optional<Order> findById(OrderNo id);
    
    void save(Order order);
}
```
```
// 코드 주입
@Service
public class CancelOrderService {
	private OrderRepository orderRepository;
    
    public cancelOrderService(OrderRepository orderRepository, ...) {
    	this.orderRepository = orderRepository;
        ..
    }
    
    @Transactional
    public void cancel(OrderNo orderNo, Canceller canceller) {
    	Order order = orderRepository.findById(orderNo)
        	.orElseThrow(() -> new NoOrderException());
            
       if(!cancelPolicy.hasCancellationPermission(order, canceller)) {
       		throw new NoCancellablePermission();
       }
       order.cancel();
    }
}
```
* 저장
```
Order save(Order entity)
void save(Order entity)
```
* 식별자 조회
```
Order findById(OrderNo id)
Optional<Order> findById(OrderNo id)
``` 
* 목록 조회
```
List<Order> findByOrderer(Orderer orderer)
// 중첩 프로퍼티
List<Order> findByOrderMemberId(MemberId memberId)
``` 
* 삭제
```
void delete(Order order)
void deleteById(OrderNo id)
```

## 매핑 구현
### 엔티티와 밸류 기본 매핑 구현
* 애그리거트와 JPA 매핑을 위한 기본 규칙은 다음과 같다.
  * 애그리거트 루트는 엔티티이므로 @Entity 로 매핑 설정한다.
* 한 테이블에 엔티티와 밸류 데이터가 같이 있다면
  * 밸류는 @Embeddable 로 매핑 설정한다.
  * 밸류 타입 프로퍼티는 @Embedded 로 매핑 설정한다.

<img src="./그림 4.2.png">

```
// 루트 엔티티는 JPA의 @Entity로 매핑한다.
@Entity
@Table(name="purchase_order")
public class Order {
	...
}
```
```
// 밸류는 @Embeddable로 매핑한다.
@Embeddable
public class Orderer {
	
    // MemberId에 정의된 컬럼 이름을 변경하기 위해
    // @AttributeOverride 애너테이션 
    @Embedded
    @AttributeOverrides(
    	@AttributeOverride(name = "id", Column = @Column(name="orderer_id"))
    )
	private MemberId memberId;
    
    @Column(name = "orderer_name")
    private String name;
}
```
```
// Shippinginfo 밸류는 또 다른 밸류를 포함하고 매핑 설정과 다른 칼럼 이름을 사용하기 위해서 @AttributeOverrids 애너테이션을 사용한다.
@Embeddable
public class ShippingInfo {
	@Embedded
    @AttributeOverrids({
    	@AttributeOverride(name = "zipCode", column= @Column(name="shipping_zipcode")),
        @AttributeOverride(name = "address1", column= @Column(name="shipping_addr1")),
        @AttributeOverride(name = "address2", column= @Column(name="shipping_addr2"))
    })
    private Address address;
    
    @Column(name = "shipping_message")
    private String message;
    
    @Embedded
    private Receiver receiver;
}
```

### 기본 생성자
* 엔티티와 밸류 생성자는 객체를 생성할 때 필요한 것을 전달받는다.
```
// 불변 타입이면 생성 시점에 필요한 값을 모두 전달받으므로 값을 변경하는 set 메서드를 제공하지 않는다.
public class Receiver {
	private String name;
    private String phone;
    
    public Receiver(String name, String phone) {
    	this.name = name;
        this.phone = phone;
    }
}
```
* JPA에선 @Entity와 @Embeddable 클래스를 매핑할려면 기본 생성자를 제공해야하는데 DB에서 데이터를 읽어와 매핑된 객체를 생성할 때 기본 생성자를 사용해서 객체를 생성해야 하기 때문이다.
```
// 기본 생성자는 JPA 프로바이더가 객체 생성 시에만 사용한다.
// 다른 코드에서 기본 생성자를 사용하지 못하도록 접근 제한자를 protected로 선언한다.
@Embeddable
public class Receiver {
	@Column(name = "receiver_name")
    private String name;
    @Column(name = "receiver_phone")
    private String phone;
    
    protected Receiver() {}	// JPA 적용을 위한 기본 생성자
    
    public Receiver(String name, String phone) {
    	this.name = name;
        this.phone = phone;
    }
}
```

### 필드 접근 방식 사용
* JPA에서는 필드와 메서드의 두 가지 방식으로 매핑 처리를 할 수 있다.
* 메서드 방식을 사용하려면 프로퍼티를 위한 get/set 메서드를 구현해야 한다.
```
@Entity 
@Access(AccessType.PROPERTY) 
public class Order {
    @Column(name = "state") 
    @Enumerated(EnumType.STRING) 
    public OrderState getState() {
        return state; 
    }

    public void setState(OrderState state) { 
        this.state = state;
    }
}
```
* 엔티티에 프로퍼티를 위한 공개 get/set 메서드를 추가하면 도메인의 의도가 사라지고 객체가 아닌 데이터 기반으로 엔티티를 구현할 가능성이 높아잔다.
* set 메서드는 내부 데이터를 외부에서 변경할 수 있는 수단이 되기 때문에 캡슐화를 깨는 원인이 될 수 있다.
* 엔티티가 객체로서 제 역할을 하려면 외부에 set 메서드 대신 의도가 잘 드러나는 기능르 제공해야한다.
* 밸류 타입을 불변으로 구현하려면 set 메서드 자체가 필요하지 않고 JPA의 구현 방식 때문에 공개 set 메서드를 추가하는 것도 좋지 않다.
* 객체가 제공할 기능 중심으로 엔티티를 구현하게끔 유도하려면 JPA 매핑 처리를 프로퍼티 방식이 아닌 필드 방식으로 선택해서 불필요한 get/set 메서드를 구현하지 말아야 한다.
```
@Entity 
@Access(AccessType.FIELD) 
public class Order {

    @EmbeddedId
    private OrderNo number;

    @Column(name = "state") 
    @Enumerated(EnumType.STRING) 
    private Orderstate state;
    ... // cancel■(乂 changeShippinglnfoO 등 도메인 기능 구현 
    ... // 필요한 get 메서드 제공
}
```

### AttributeConverter를 이용한 밸류 매핑 처리
* int, long, String, LocalDate와 같은 타입은 DB 테이블의 한 개 컬럼에 매핑되는데 밸류 타입의 프로퍼티를 한 개 컬럼에 매핑해야 할 때도 있다.
  <br/><img src="./그림 4.2.png">
* 두 개 이상의 프로퍼티를 가진 밸류 타입을 핸 개 칼럼에 매핑하려면 @Embeddable 애너테이션으로는 처리 할 수 없고 AttribureConverter를 사용해서 밸류 타입과 칼럼 데이터 간의 변환 처리가 가능하다.
```
// 타입 파라미터 X는 밸류 타입이고 Y는 DB 타입이다.
public interface AttributeConverter<X, Y> {
    public Y convertToDatabaseColumn (X attribute);
    public X convertToEntityAttribute (Y dbData);
}
```
* @Converter 애노테이션의 autoApply 속성값을 true로 지정하면 모든 Money 타입의 프로퍼티에 대해 MoneyConverter를 자동으로 적용한다.
```
@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Money money) {
		if(money == null) return null;
		else return money.getValue();
	}

	@Override
	public Money convertToEntityAttribute(Integer value) {
		if(value == null) return null;
		else return new Money(value);
	}
}
```
* @Converter autoApply 속성을 false인 경우 프로퍼티 값을 변환할 떄 사용할 컨버터를 직접 지정할 수 있다.
```
public class Order {

	@Column(name = "total_amounts")
	@Convert(converter = MoneyConverter.class)
	private Money totalAmounts;
	...
}
```
### 밸류 컬렉션: 별도 테이블 매핑
* List 타입을 이용해서 컬렉션을 프로퍼티로 지정할 수 있다.
```
public class Order {
    private List<OrderLine> orderLines;
    ...
}
```
<img src="./그림 4.2.png">

* 밸류 컬렉션을 별도 테이블로 매핑할 때는 @ElementCollection과 @CollectionTable을 함께 사용한다.
* @CollectionTable은 밸류를 저장할 테이블을 지정할 때 사용한다. name 속성으로 테이블 이름을 지정하고 joinColumns 속성은 외부키로 사용하는 컬럼을 지정한다.
```
@Entity
@Table(name = "purchase_order")
public class Order {
	...
	@ElementCollection
	@CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_number"))
	@orderColumn(name = "line_idx")
	private list<OrderLine> orderLines;
    ...
}

@Embeddable
public class OrderLine {
	@Embedded
	private ProductId productId;
	...
}
```

### 밸류 컬렉션: 한 개 컬럼 매핑
* 밸류 컬렉션을 별도 테이블이 아닌 한 개 컬럼에 저장해야 할 때가 존재하는데 도메인 모델에는 이메일 주소 목록을 Set으로 보관하고 DB에는 한 개 칼럼에 콤마로 구분해서 저장해야 할 때가 있다.
* AttributeConverter를 사용하면 밸류 컬렉션을 한 개 컬럼에 쉽게 매핑 할 수 있다.
* AttributeConverter를 사용하려면 밸류 컬렉션을 표현하는 새로운 밸류 타입을 추가해야 한다.
```
// 밸류 객체 생성
public class EmailSet {
	private Set<Email> emails = new HashSet<>();

	private EmailSet(Set<Email> emails) {
		this.emails.addAll(emails);
	}

	public Set<Email> getEmails() {
		return Collections.unmodifiableSet(emails);
	}
}
```
```
// AttributeConverter 구현
@Converter
public class EmailSetConveter implements AttributeConveter<EmailSet, String> {
	@Override
	public String convertToDatabaseColumn(EmailSet attribute) {
		if(attribute == null) return null;
		return attribute.getEmails().stream()
						.map(Email::toString)
						.collect(Collectors.joining(","));
	}
	@Override
	public EmailSet convertToEntityAttribute(String dbData) {
		if(dbData == null) return null;
		String[] emails = dbData.split(",");
		Set<Email> emailSet = Arrays.stream(emails)
						.map(value ->  new Email(value))
						.collect(toSet());
		return new EmailSet(emailSet);
	}
}
```
```
// EmailSet 타입 프로퍼티가 Converter로 EmailSetConverter를 사용하도록 지정
@Column(name = "emails")
@Convert(converter = EmailSetConverter.class)
private EmailSet emailSet;
```

### 밸류를 이용한 ID 매핑
* 식별자라는 의미를 부각시키기 위해 식별자 자체를 밸류 타입으로 만들 수도 있다.
```
// 밸류 타입을 식별자로 매핑하면 @Id 대신 @EmbeddedId 애너테이션을 사용한다.
@Entity
@Table(name = "purchase_order")
public class Order {
    @EmbeddedId
    private OrderNo number;
    ...
}
// JPA에서 식별 자 타입은 Serializable 타입이어여 하므로 식별자를 사용할 밸류 타입은 Serializable 인터페이스를 상속 받아야 한다.
@Embeddable
public class OrderNo implements Serializable {
	@Column(name = "order_number")
	private String number;
	...
}
```
* 밸류 타입으로 식별자를 구현할 때 얻을 수 있는 장점은 식별자에 기능을 추가 할 수 있다는 점이다.
```
// OrderNo 클래스에 시스템 세대를 구분할 수 있는 기능을 구현할 수 있다.
@Embeddable
public class OrderNo implements Serializable {
	@Column(name = "order_number")
	private String number;

	public boolean is2ndGeneration() {
		return number.startsWith("N");
	}
	...
}
```
```
// 시스템 세대 구분이 필요한 코드는 OrderNo가 제공하는 기능을 이용해서 구분한다.
if (order.getNumber().is2ndGeneration()) {

}
```
* JPA 내부적으로 엔티티를 비교할 목적으로 equals() 메서드와 hashcode() 값을 사용하므로 식별자로 사용할 밸류 타입은 이 두 메서드를 알맞게 구현해야 한다.

### 별도 테이블에 저장하는 밸류 매핑
* 애그리거트에서 루트 엔티티를 뺀 나미저 구성요소는 대부분 밸류다.
  * 루트 엔티티 외에 또 다른 엔티티가 있다면 진짜 엔티티인지 의심해 봐야 한다.
  * 별도 테이블에 데이터를 저장한다고 해서 엔티티인 것은 아니다.
* 밸류가 아닌 엔티티가 확실하다면 해당 엔티티가 다른 애그리거트는 아닌지 확인해야 한다.
  * 자신만은 독자적인 라이프 사이클을 갖는다면 구분되는 애그러거트일 가능성이 높다.
* 애그리거트에 속한 객체가 밸류인지 엔티티인지 구분하는 방법은 고유 식별자를 갖는지 확인하는 것이다.
  * 식별자를 찾을 때 매핑되는 테이블의 식별자를 애그리거트의 구성요소의 식별자와 동일한 것으로 착각하면 안된다.
  * 별도의 테이블에 PK가 있다고 해서 테이블과 매핑되는 애그리거트 구성요소가 항상 고유 식별자를 갖는 것은 아니기 때문이다.

<img src="./그림 4.6.png">

* ArticleContent는 밸류이므로 @Embeddable로 매핑한다.
* ArticleContent와 매핑되는 테이블은 Artible과 매핑되는 테이블과 다른데, 이때 밸류를 매핑한 테이블을 지정하기 위해 @SecondaryTable과 @AttributeOverride를 사용한다.
* @SecondaryTable의 name 속성은 밸류를 저장할 테이블을 지정한다.

```
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
	
    private String title;

	@AttributeOverrides({
		@AttributeOverride(name = "content",
			column = @Column(table = "article_content")),
		@AttributeOverride(name = "contentType",
			column = @Column(table = "article_content"))
	})
    @Embedded
	private ArticleContent content;
	...
}
```
* @SecondaryTable을 이용하면 아래 코드를 실행할 때 두 테이블을 조인해서 데이터를 조회한다.
* 한 가지 단점은 @SecondaryTable 사용하면 목록 화면에 Article을 조회할 때 article_content 테이블까지 조인해서 테이터를 읽어오는데 이는 원하는 결과가 아니며 5장의 조회 전용 쿼리를 실행하여 해결할 수 있다.
```
// @SecondaryTable로 매핑된 artible_content 테이블을 조인
Article article = entityManager.find(Article.class, 1L);
```

### 밸류 컬렉션을 @Entity로 매핑하기
* 개념적으로 밸류인데 구현 기술의 한계나 팀 표준 때문에 @Entity를 사용해야 할 때도 있다.
* JPA는 @Embeddable 타입의 클래스 상속 매핑을 지원하지 않는데 상속 구조를 갖는 밸류 타입을 사용할려면 @Entity를 이용해서 상속 매핑으로 처리해야 한다.
* @Entity로 매핑하기에 식별자와 구현 클래스 구분을 위한 타입 식별 칼럼을 추가해야한다.
  <img src="./그림 4.7.png">

### ID 참조와 조인 테이블을 이용한 단방향 M-N 매핑
* 애그리거트 간 집합 연관은 성능 상의 이유로 피해야 하지만 요구사항을 구현하는데 집합 연관을 사용하는 것이 유리하다면 ID 참조를 이용한 단방향 집합 연관을 적용해 볼 수 있다.
```
// Product에서 Category로 단방향 M-N 연관을 ID 참조 방식으로 구현
@Entity
@Table(name = "product")
public class Product {
	@EmbeddedId
	private ProductId id;

	@ElementCollection
	@CollectionTable(name ="product_category",
		joinColumns = @JoinColumn(name = "product_id"))
	private Set<CategoryId> categoryIds;
	...
}
```

## 애그리거트 로딩 전략
* JPA 매핑을 설정할때 중요한점은 애그리겉의 속한 객체가 모두 모여야 완전한 하나가 된다는 것이다.
* 애그리거트는 개념적으로 하나여야 한다. 하지만, 루트 엔티티를 로딩하는 시점에 애그리거트에 속한 객체를 모두 로딩해야 하는 것은 아니다.
* 애그리거트가 완전해야 하는 이유는 두가지 정도이다.
  * 상태를 변경하는 기능을 실행할때 애그리거트 상태가 완전해야 한다.
  * 표현 영역에서 애그리거트의 상태 정보를 보여줄 때 필요하다.
* 상태 변경 기 능을 실행하기 위해 조회 시점에 즉시 로딩을 이용해서 애그리거트를 완전한 상태로 로딩할 필 요는 없다. JPA는 트랜잭션 범위 내에서 지연 로딩을 허용하기 때문에 다음 코드처럼 실제로 상태를 변경하는 시점에 필요한 구성요소만 로딩해도 문제가 되지 않는다.
```
@Transactional
public void revmoeoptions(ProductId id, int optIdxToBeDeleted) {
		//Product를 로딩/ 컬렉션은 지연 로딩으로 설정했다면 Option은 로딩되지 않음
		Product product = productRepository.findByid(id);
		
		// 트랜잭션 범위이므로 지연 로딩으로 설정한 연관 로딩 가능
		product.removeOption(optIdxToBeDeleted);
}	
```
```
@Entity
public class Product {
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "product_option",
		joinColumns = @JoinColumn(name = "product_id"))
	@OrderColumn(name = "list_idx")
	private List<Option> options = new ArrayList<>();

	public void removeOption(int optIdx) {
			//실제 컬렉션에 접근할 때 로딩
			this.options.remove(optIdx);
	}
}
```
* 일반적으로 상태를 변경하기 보다는 조회하는 빈도 수가 높다. 이런 이유로 애그리거트 내의 모든 연관을 즉시 로딩으로 설정할 필요는 없다. 물론, 지연 로딩은 즉시 로딩보다 쿼리 실행 횟수가 많아질 가능성이 더 높다. 따라서, 무조건 즉시 로딩이나 지연 로딩으로만 설정하기보다는 애그리거트에 맞게 즉시 로딩과 지연 로딩을 선택해야 한다.

## 애그리거트 영속성 전파
* 애그리거트는 완전한 상태여야 한다는 것은 조회할 때뿐만 아니라 저장하고 삭제할 때도 하나로 처리해야 함을 의미한다.
  * 저장 메서드는 애그리거트 루트만 저장하면 안 되고 애그리거트에 속한 모든 객체를 저장해야 한다
  * 삭제 메서드는 애그리거트 루트뿐만 아니라 애그리거트에 속한 모든 객체를 삭제 해야 한다.
* @Embeddable 매핑 타입의 경우 함께 저장되고 삭제되므로 cascade 속성을 추가로 설정하지 않아도 된다. 반면에 애그리거트에 속한 @Entity 타입에 대한 매핑은 cascade 속성을 사용해서 저장과 삭제 시에 함께 처리되도록 설정해야 한다.
  * @OneToOne, @OneToMany는 cascade 속성의 기본값이 없으므로 cascade 속성값으로 CascadeType.PERSIST, CascadeType.REMOVE를 설정한다.

## 식별자 생성 기능
* 식별자는 크게 세가지 방식 중 하나로 생성한다.
  * 사용자가 직접 생성
  * 도메인 로직으로 생성
  * DB를 이용한 일련번호 사용
* 식별자 생성 규칙이 있다면 엔티티를 생성할 때 식별자를 엔티티가 별도 서비스로 식별자 생성 기능을 분리해야 한다.
* 식별자 생성 규칙은 도메인 규칙이므로 도메인 영역에 식별자 생성 기능을 위치시켜야 한다.
* 도메인 서비스를 만들어 도메인 영역에 위치시킬 수 있다.
```
public class ProductIdService {
    public ProductId nextId() {
        ... // 정해진 규칙으로 식별자 생성
    }
}
```
* 식별자 생성 규칙을 구현하기에 적합한 또 다른 장소는 리포지터리인데 인터페이스에 기능을 추가하고 구현 클래스에서 알맞게 구현하면 된다.
```
public interface ProductRepository {
    ...// save() 등 다른 메서드

    // 식별자를 생성하는 메서드
    ProductId nextId();
}
```
* DB 자동증가 컬럼을 식별자로 사용할 경우 @GeneratedValue를 사용하며 DB에 insert 쿼리가 실행돼야 식별자가 생성된다.
* JPA의 식별자 생성 기능을 사용하는 경우에도 저장 시점에서 식별자를 생성한다.

## 도메인 구현과 DIP
* JPA를 사용하면 JPA에 특화된 @Entity, @Table, @Id, @Column 등의 애너테이션을 사용하고 있다.
* DIP에 따르면 구현 기술에 속하므로 도메인 모델이 구현 기술인 JPA에 의존하면 안된다.
* 리포지터리 또한 마찬가지인데 인터페이스는 도메인 패키지에 위치 하는데 구현 기술인 Spring Data JPA의 레파지토리 인터페이스를 상속하고 있고 도메인이 인프라에 의존하고 것이다.
* 도메인에서 구현 기술에 대한 의존 없이 도메인을 순수하게 유지하려면 스프링 데이터 JPA의 Repository를 상속받지 않도록 수정하고 구현한 클래스를 인프라에 위치시켜야 한다.
  <img src="./그림 4.9.png">

* DIP를 적용하는 이유는 저수준 구현이 변경되어도 고수준이 영향 받지 않도록 하기 위함인데 리포지터리와 도메인 모델의 구현 기술은 거의 바꾸지 않는데 따라서 거의 변경이 없는 경우에 변경을 미리 대비하는 것은 오버엔지니어링일 수 있다.
* 저자의 경우 애그리거트, 리포지터리 등 도메인 모델을 구현할 때 타협을 했고 JPA 전용 애너테이션을 사용하긴 했지만 도메인 모델 단위 테스트하는 데와 리포지터리 자체는 인터페이스고 테스트 가능성을 해치지 않는다.
* DIP를 완전히 지키면 좋지만 개발 편의성과 실용성을 가져가면서 구조적인 유연함은 어느정도 유지했으면 복잡도를 높이지 않으면서 구현 기술에 따른 제약이 낮다면 합리적인 선택이라고 생각한다.