# 스프링 데이터 JPA를 이용한 조회 기능

## 시작에 앞서
* `CQRS`는 명령 모델과 조회 모델을 분리하는 패턴이다.
    * `명령 모델`은 상태를 변경하는 기능을 구현할 때 사용한다.
    * `조회 모델`은 데이터를 조죄하는 기능을 구현할 때 사용한다.
* 도메인 모델은 주문 취소, 배송지 변경과 같이 상태를 변경할 때 주로 사용되는데 도메인 모델은 명령 모델로 주로 사용한다.
* 정렬, 페이징, 검색 조건 지정과 같은 주문 목록, 상품 상세와 같은 조회 기능은 조회 모델을 구현할 때 주로 사용한다.

## 검색을 위한 스펙
* 검색 조건이 고정되어 있다면 기능을 특정 조건 조회 기능을 추가하면 된다.
```
public interface OrderDataDao {
    Optional<OrderData> findById(OrderNo id);
    List<OrderData> findByOrderer(String orderId, Data fromData, Data toData);
    ...
}
```
* 검색 조건을 다양하게 조합해야 할 때 사용할 수 있는 것이 스펙인데 스펙은 애그리거트가 특정 조건을 충족하는지 검사할 때 사용하는 인터페이스다.
```
public interface Speficiation<T> {
    public boolean isSatisfiedBy(T egg);
}
```
* agg 파라미터는 검사 대상이 되는 객체이며 리포지터리에서 사용하면 애그리거트 루트가 되고 스펙을 DAO 에 사용하면 검색 결과로 리턴할 데이터 객체가 된다.
```
public class OrdererSpec implements Specification<Order> {

  private String ordererId;

  public boolean isSatisfiedBy(Order agg) {
    return agg.getOrdererId().getMemberId().getId().equals(ordererId);
  }

}
```
* 리포지터리나 DAO는 검색 대상을 걸러내는 용도로 스펙을 사용한다.
```
public class MemoryOrderRepository implements OrderRepository {
    public List<Order> findAll(Specification<Order> spec) {
        List<Order> aUOrders = findAll();
        return aUOrders.stream()
            .filter(order -> spec.isSatisfiedBy(order))
            .toList();
    }
}
```
* 모든 애그리거트 객체를 메모리에 보관하기도 어렵고 설사 메모리에 다 보관할 수 있다 하더라도 조회 성능에 심각한 문제가 발생하기 때문에 실제 스펙은 사용하는 기술에 맞춰 구현하게 된다.

## 스프링 데이터 JPA를 이용한 스펙 구현
* 스프링 데이터 JPA는 검색 조건을 표현하기 위한 인터페이스인 Specification을 제공한다.
* 제네릭 타입 파라미터 T는 JPA 엔티티 타입을 의미한다.
```
public interface Specification<T> extends Serializable {
  // not, where, and, or 메서드 생략

  @Nullable
  Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder cb);
}
```
* toPredicat() 메서드는 JPA 크리테리아(Criteria) API에서 조건을 표현하는 Predicate를 생성한다.
* OrdererIdSpec 클래스는 Specification<OrderSummary> 타입을 구현하므로 OrderSummary에 대한 검색 조건을 표현한다.
* toPredicate() 메서드는 ordererId 프로퍼티 값이 생성자로 전달받은 ordererId와 동일한지 비교하는 Predicate를 생성한다.
```
// 스펙 인터페이스를 구현한 클래스 예시
public class OrdererldSpec implements Specification<OrderSummary> {
    
    private String ordererld;
    
    public OrdererIdSpec(String ordererld) {
        this.ordererld = ordererld;
    }

    @Override
    public Predicate toPredicate(Root<OrderSummary> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.equal(root.get(OrderSummary_.ordererId), ordererld);
    }
}
```
* 스펙 구현 클래스를 개별적으로 만들지 않고 별도 클래스에 스펙 생성 기능을 모아도 된다.
```
// 스펙 생성 기능을 별도 클래스에 모은 예시
public class OrderSummarySpecs {
    public static Specification<OrderSummary> ordererId(String ordererld) {
        return (Root<OrderSummary> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb.equal(root.<String>get("ordererld"), ordererld);
    }

    public static Specification<OrderSummary> orderDateBetween( LocalDateTime from, LocalDateTime to) {
        return (Root<OrderSummary> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> cb.between(root.get(OrderSummary_.orderDate), from, to);
    } 
}
```
```
// 스펙 생성 기능 클래스를 이용한 코드
Specification<OrderSummary> betweenSpec = OrderSummarySpecs.orderDateBetween(from, to);
```

## 리포지터리/DAO에서 스펙 사용하기
* 스펙을 충족하는 엔티티를 검색하고 싶다면 findAll() 메서드를 사용하면 된다.
```
// 메서드를 사용하는 예시
public interface OrderSummaryDao extends Repository<OrderSummary, String> {
    List<OrderSummary> findAll(Specification<OrderSummary> spec);
}
```
* 스펙 구현체를 사용하면 특정 조건을 충족하는 엔티티를 검색할 수 있다.
```
// 코드 단위로 사용하는 예시
// 스펙 객체를 생성하고
Specification<OrderSummary> spec = new OrdererIdSpec("user1"); 
// findAllO 메서드를 이용해서 검색
List<OrderSummary> results = OrderSummaryDao.findAll(spec);
```

## 스펙 조합
* 스프링 데이터 JPA가 제공하는 스펙 인터페이스는 스펙을 조합할 수 있는 `and`와 `or`를 제공하고 있다.
```
// and와 or 메서드를 제공하는 스펙 인터페이스
public interface Specification<T> extends Serializable {

  default Specification<T> and(@Nullable Specification<T> other) { ... }
  default Specification<T> or(@Nullable Specification<T> other) { ... }
  
  @Nullable
  Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder);
}
```
* and()와 or() 메서드는 기본 구현을 가진 디폴트 메서드이다.
* `and()` 메서드는 두 스펙을 모두 충족하는 조건을 표현하는 스펙을 생성하고 `or()` 메서드는 두 스펙 중 하나 이상 충족하는 조건을 표현하는 스펙을 생성한다.
```
// and()와 or()의 사용 예시
Specification<OrderSummary> specl = OrderSummarySpecs.ordererId("userl"); 
Specification<OrderSummary> spec2 = OrderSummarySpecs.orderDateBetween(
    LocaWateTime.of(2022, 1, 1, 0, 0, 0),
    LocalDateTime.of(2022, 1, 2, 0, 0, 0)); 
Specification<OrderSummary> spec3 = spec1.and(spec2);
Specification<OrderSummary> spec3 = spec1.or(spec2);
```
```
// 체이닝을 통한 불필요한 변수 사용 제거
Specification<OrderSummary> spec = OrderSummarySpecs.ordererId("user1")
    .and(OrderSummarySpecs.orderDateBetween(from, to));
```
* `not()` 메서드 또한 제공하는데 조건을 반대로 적용할 때 사용한다.
```
Specification<OrderSummary> spec = Specification.not(OrderSummarySpecs.ordererld(user1"));
```
* null 가능성이 있는 스펙과 다른 스펙을 조합해야하면 NullPointerException이 발생할 수 있는데 코드를 통해서 null을 검사하면 매우 힘들기 때문에 `where()` 메서드를 사용하면 해당 문제를 방지할 수 있다.
```
Specification<OrderSummary> spec = Specification.where(createNull.ableSpec()).and(createOtherSpec());
```

## 정렬 지정하기
* 스프링 데이터 JPA는 두 가지 방법을 사용해서 정렬을 지정할 수 있다.
    * 메서드 이름에 OrderBy를 사용해서 정렬 기준 지정
    * Sort를 인자로 전달 
```
// 특정 프로퍼티를 조회하는 find 메서드는 이름 뒤에 OrderBy를 사용해서 정렬 순서를 지정하는 코드 예시
public interface OrderSummaryDao extends Repository<OrderSummary, String> { 
    // ordererId 프로퍼티 값을 기준으로 검색 조건 지정
    // number 프로퍼티 값 역순으로 정렬
    List<OrderSummary> findByOrdererIdOrderByNumberDesc(String ordererld);
    // number 프로퍼티 값을 오름차순으로 정렬
    List<OrderSummary> findByOrdererIdOrderByNumberDesc(String ordererld);
}
```
* 메서드 이름 통해서 정렬을 하면 간단하지만 정렬 기준이 많아지면 메서드 이름이 길어지는 단점이 존재한다. 이런 경우에는 Sort 타입을 사용해서 처리가 가능하다.
```
// Sort 타입을 파라미터로 사용한 예시
public interface OrderSummaryDao extends Repository<OrderSummary, String> { 
   List<OrderSummary> findByOrdererId(String ordererld, Sort sort); 
   List<OrderSummary> findAll(Specification<OrderSummary> spec, Sort sort); 
}
```
```
// Sort를 사용한 구현 예시
Sort sort = Sort.by("number").ascendingO;
List<OrderSummary> results = OrderSummaryDao.findByOrdererldf(user1", sort);
// 오름차순
Sort sort1 = Sort.by("number").ascending();
// 역순
Sort sort2 = Sort.by("orderDate").descending();
// 두 개 이상의 정렬 순서
Sort sort = sort1.and(sort2);
// 체이닝은 통한 짧은 표현
Sort sort = Sort.by("number")
    .ascending()
    .and(Sort.by("orderDate").descending());
```

## 페이징 처리하기
* 목록을 보여줄 때 전체 데이터 중 일부만 보여주는 페이징 처리는 기본인데 스프링 데이터 JPA는 페이칭 처리를 위해 Pagealbe 타입을 이용한다.
```
// 레파지토리 구현 예시
public interface MemberDataDao extends JpaRepository<MemberData, String> {
    List<Member> findByNameLike(String name, Pageable pageable)
}
```
```
// 코드 구현 예시
PageRequest pageReq = PageRequest.of(1, 10);
List<MemberData> user = memberDataDao.findByNameList("사용자%",  pageReq);
```
* Page와 Sort를 사용하면 정렬 순서를 지정할 수 있다.
```
// Page와 Sort를 사용한 코드 구현 예시
Sort sort = Sort.by("name").descending();
PageRequest pageReq = PageRequest.of(1, 2, sort);
List<MemberData> user = memberDataDao.findByNameLike("사용자%", pageReq);
```
* Page 타입을 사용하면 데이터 목록뿐만 아니라 조건에 해당하는 전체 개수도 구할 수 있다.
```
public interface MemberDataDao extends Repository<MemberData, String> { 
    Page<MemberData> findByBlocked(boolean blocked, Pageable pageable);
}
```
* Pageable을 사용하는 메서드 `리턴 타입이 Page인 경우에만` 스프링 데이터 JPA는 목록 조회 쿼리와 함께 COUNT 쿼리도 실행해서 조건에 해당하는 개수도 함께 조회한다.

```
// Page가 제공하는 메서드의 일부 예시
Pageable pageReq = PageRequest.of(2, 3);
Page<MemberData> page = memberDataDao.findByBlocked(false, pageReq); 
List<MemberData> content = page.getC아itent(); // 조회 결과 목록
long totalElements = page.getTotalElements(); // 조건에 해당하는 전체 개수 
int totalPages = page.getTotalPages(); // 전체 페이지 번호
int number = page.getNumber(); // 현재 페이지 번호
int numberOfElements = page.getNumberOfElements(); // 조회 결과 개수
int size = page.getSizeQ; // 페이지 크기
```
* 스펙을 사용하는 findAll() 메서드도 Pageable을 사용할 수 있다.
```
public interface MemberDataDao extends Repository<MemberData, String> {
     Page<MemberData> findAll(Specification<MemberData> spec, Pageable pageable);
}
```
* 처음부터 N개의 데이터가 필요하다면 Pageable을 사용하지 않고 find에 FirstN또는 TopN 형식의 메서드 를 사용할 수도 있다.
```
// findFirstN 사용 예시
List<MemberData> findFirst3ByNameLikeOrderByName(String name);
// topN 사용 예시
List<MemberData> findTop3ByNameLikeOrderByName(String name);
// N이 없는 경우 하나만 리턴하는데 사용 예시
MemberData findFirstByBlockedOrderByld(boolean blocked);
MemberData findTopByBlockedOrderByld(boolean blocked)
```

## 스펙 조합을 위한 스펙 빌더 클래스
* 스펙을 생성하다 보면 다음 코드처럼 조건에 따라 스펙을 조합해야 할 때가 있다.
```
// 스펙 조합 코드 예시
Specification<MemberData> spec = Specification.where(null);
if(searchRequest.isOnlyNotBlocked()) {
	spec = spec.and(MemberDataSpecs.nonBlocked());
}
if(StringUtils.hasText(searchRequest.getName())) {
	spec = spec.and(MemberDataSpecs.nameLike(searchRequest.getName()));
}
List<MemberData> results = memberDataDao.findAll(spec, PageRequest.of(0, 5));
```
* 스펙을 조합하게 되면 if와 각 스펙들이 섞여서 복잡한 구조를 가지게 되는데 이때 스펙 빌더를 사용해서 코드 가독성을 높이고 구조를 단순하게 작성할 수 있다.
```
// 체이닝을 통한 변수할당과 if문을 줄이는 코드 예시
Specification<MemberData> spec = SpecBuilder.builder(MemberData.class)
	.ifTrue(searchRequest.isOnlyNotBlocked(),
    	() -> MemberDataSpecs.nonBlocked())
    .ifHasText(searchRequest.getName(),
    	name -> MemberDataSpecs.nameLike(searchRequest.getName()))
    .toSpec();
List<MemberData> result = memberDataDao.findAll(spec, PageRequest.of(0, 5));
```
* 스펙 빌더 클래스는 and(), ifHasText(), ifTrue() aㅔ서드가 존재하는데 이 외에 필요한 메서드는 추가해서 사용하면 된다.
```
public class SpecBuilder {
	public static <T> Builder<T> build(Class<T> type) {
    	return new Builder<T>();
    }
    
    public static class Builder<T> {
    	private List<Specification<T>> specs = new ArrayList<>();
        
        public Builder<T> and(Specification<T> spec) {
        	specs.add(spec);
            return this;
        }
        
        public Builder<T> ifHasText(String str, 
        	Function<String, Specification<T>> specSupplier) {
            if(StringUtils.hasText(str)) {
                specs.add(specSupplier.apply(str));
            }
            return this;
        }
        
        public Builder<T> ifTrue(Boolean cond, 
        	Supplier<Specification<T>> specSupplier) {
         	if(cond != null && cond.booleanValue()) {
            	specs.add(specSupplier.get());
            }
            return this;
        }
        
        public Specification<T> toSpec() {
        	Specification<T> spec = Specification.where(null);
            for(Specification<T> s : specs) {
            	spec = spec.and(s);
            }
            return spec;
        }
    }
}
```

## 동적 인스턴스 생성
* JPA는 쿼리 결과에서 임의의 객체를 동적으로 생성할 수 있는 기능을 제공하고 있다.
```
// JPQL에서 동적 인스턴스를 사용한 코드
public interface OrderSummaryDao extends Repository<OrderSummary, String> {
    
    @Query("""
        select new com.myshop.order.query.dto.OrderView(
            o.number, o.state, m.name, m.id, p.name 
        )
        from Order o join o.orderLines ol, Member m, Product p 
        where o.orderer.memberld.id = :ordererld
        and o.orderer.memberld.id = m.id
        and index(ol) = 0
        and ol.productld.id = p.id 
        order by o.number.number desc
        """)
    List<OrderView> findOrderView(String ordererld);
```
* 표현영역을 통해 사용자에게 데이터를 보여주기 위해 조회 전용 모델을 만든다.
* 밸류타입을 원하는 형식으로 출력하도록 프레임워클르 확장해서 조회 전용 모델에서 밸류 타입의 의미가 사라지지 않도록 할 수 있다.
```
// 조회 전용 모델
 public class Orderview {
    private final String number; 
    private final Orderstate state; // 밸류타입
    private final String memberName; 
    private final String memberld; 
    private final String productName;

    public OrderView(OrderNo number, Orderstate state. String memberName, Memberld memberld, String productName) { 
        this.number = number.getNumber();
        this.state = state; 
        this.memberName = memberName; 
        this.memberld = memberld.getld(); 
        this.productName = productName;
    }
    
    ... // get 메서드
}
```
* 동적 인스턴스의 장점은 JPQL을 그대로 사용하므로 객체 기준으로 쿼리를 작성하면서도 동시에 지연/즉시 로딩과 같은 고민 없이 원하는 모습으로 데이터를 조회할 수 있다.

## 하이버네이트 @Subselect 사용
* 하이버네이트는 JPA 확장 기능으로 @Subselect를 제공한다.
* @Subselect는 쿼리 결과를 @Entity로 매핑할 수 있는 유용한 기능이다.
* @Immutable, @Subselect, @Synchronize는 하이버네이트 전용 애너테이션으로 해당 태그를 사용해서 테이블이 아닌 쿼리 결과를 @Entity로 매핑할 수 있다.
* @Subselect는 조회 쿼리를 값으로 가지는데 이렇게 조회한 @Entity는 매핑된 테이블이 없기 때문에 수정을 할 수 없다.
* 만약 수정을 하게 된다면 하이버네이트는 오류가 발생하게 된다. 이 때 @Immutable를 통해서 해당 엔티티 매핑 필드/프로퍼티가 변경되도 DB에 반영되지 않고 무시한다.
```
// 쿼리로 매핑하는 코드 예시
@Entity
@Immutable
@Subselect(
        """
        select o.order_number as number,
        o.version,
        o.orderer_id,
        o.orderer_name,
        o.total_amounts,
        o.receiver_name,
        o.state,
        o.order_date,
        p.product_id,
        p.name as product_name
        from purchase_order o inner join order_line ol
            on o.order_number = ol.order_number
            cross join product p
        where
        ol.line_idx = 0
        and ol.product_id = p.product_id"""
)
@Synchronize({"purchase_order", "order_line", "product"})
public class OrderSummary {
    @Id
    private String number;
    private long version;
    @Column(name = "orderer_id")
    private String ordererId;
    @Column(name = "orderer_name")
    private String ordererName;
    ...생략

    protected OrderSummary() {
}
    }
```
* 하이버네이트는 일반적으로 트랙잭션을 커밋하는 시점에 DB에 반영하는데 변경 내역을 아직 반영하지 않은 상태에서 다시 조회하게 되면 최신 값이 담기지 않게 된다.
* 이런 문제를 해소하기 위해서 @Synchronize를 사용하는데 @Synchronize에 해당 엔티티와 관련된 테이블 목록을 명시하고 엔티티를 로딩하기 전 지정한 테이블과 관련된 변경이 발생하면 플러시를 먼저한다.
```
// purchase_order 테이블에서 조회
Order order = orderRepository.findById(orderNumber);
order.changeShippingInfo(newInfo); // 상태 변경
// 변경 내역이 DB에 반영되지 않았는데 purchase_order 테이블에서 조회
List<OrderSummary> summaries = orderSummaryRepository.findByOrdererld(userid);
```
* @Subselect를 사용해도 일반 @Entity와 같기 때문에 EntityManger#find(), JPQL, Criteria를 사용해서 조회하는 장점이 있다. 스펙또한 사용이 가능하다.
```
// @Subselect를 적용한 @Entity는 일반 @Entity와 동일한 방법으로 조회할 수 있다.
Specification<OrderSummary> spec = orderDateBetween(from, to);
Pageable pageable = PageRequest.of(1, 10);
List<OrderSummary> results = orderSummaryDao.findAll(spec, pageable);
```
* 서브 쿼리를 사용하고 싶지 않다면 네이티브 SQL 쿼리를 사용하거나 마이바티스와 같은 별도 매퍼를 사용해서 조회 기능을 구현해야 한다.