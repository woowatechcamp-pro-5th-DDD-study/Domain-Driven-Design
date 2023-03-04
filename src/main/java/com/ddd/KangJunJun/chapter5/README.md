# Chapter 5 - 스프링 데이터 JPA 를 이용한 조회 기능

## 5.1 시작에 앞서

* CQRS : 명령<sup>Command</sup> 모델과 조회<sup>Query</sup> 모델을 분리하는 패턴
  * 명령 모델 : 상태를 변경하는 기능을 구현할 때 사용  
  * 조회 모델 : 데이터를 조회하는 기능을 구현할 때 사용  

도메인 모델(엔티티, 애그리거트, 리포지터리)은 명령 모델로 주로 사용된다.  
정렬, 검색 조건 지정과 같은 기능은 조회 기능에 사용된다.


## 5.2 검색을 위한 스펙

다양한 검색 조건을 조합해야 하는 경우 스펙(Specification)을 사용한다.  
````java
public interface Specification<T>{
    protected boolean isSatisfiedBy(T agg);
}
````

agg 파라미터는 검사 대상이 되는 객체다. 
- 리포지터리에서 사용 -->  agg : 애그리거트 루트
- DAO에서 사용 --> agg : 검색 결과 객체

리포지터리나 DAO는 검색 대상을 걸러내는 용도로 스펙을 사용한다.

````java
public class MemoryOrderRepository implements OrderRepository {
  public List<Order> findAll(Specification<Order> spec){
      List<Order> allOrders = findAll();
      return allOrders.stream()
              .filter(order -> spec.isSatisfiedBy(order))
              .toList();
  }
}
````

아래 코드와 같이 스펙을 리포지터리에 전달하면 리포지터리가 스펙을 이용해서 검색을 걸러준다.
````
Specification<Order> orderSpec = new OrderSpec("kangjunjun");
List<Order> orders = orderRepository.findAll(orderSpec);
````

하지만 findAll 을 하고나서 거르면 메모리와 성능의 문제가 있기 때문에 실제로는 위와 같은 방법으로는 구현하지 않는다.

## 5.3 스프링 데이터 JPA 를 이용한 스펙 구현

스프링 데이터 JPA는 검색 조건을 표햔하기 위한 스펙 인터페이스를 제공한다.  
스펙 인터페이스에서 제네릭 타입 파라미터 T는 JPA 엔티티 타입을 의미한다.  
toPredicate() 메서드는 JPA 크리테리아(Criteria) API에서 조건을 표현하는 Predicate를 생성한다.

스펙 구현 클래스를 개별적으로 만들지 않고 별도 클래스에 스펙 생성 기능을 모아도 된다.  
스펙 인터페이스는 함수형 인터페이스이므로 람다식을 이용해서 객체를 생성할 수 있다.

## 5.4 리포지터리/DAO 에서 스펙 사용하기

findAll() 메서드는 스펙 인터페이스를 파라미터로 갖는다.  
이 메서드와 스펙 구현체를 사용하면 특정 조건을 충족하는 엔티티를 검색할 수 있다.

````
Specification<OrderSummary> spec = new OrderIdSpec("1");
List<OrderSummary> results = orderSummaryDao.findAll(spec);
````

## 5.5 스펙 조합

* 스프링 데이터 JPA는 스펙을 조합할 수 있는 두 메서드 `and`, `or`를 제공한다.
* 부정 비교를 위한 `not` 메서드도 기본 제공한다.
* null exception 방지를 위한 메서드 `where` 를 제공한다.

## 5.6 정렬 지정하기

스프링 데이터 JPA는 두 가지 정렬 방법이 있다.
* 메서드 이름에 OrderBy를 사용해서 정렬 기준 지정
  * 메소드 뒤에 이름만 붙이면 되서 사용이 간단하다.
  * 정렬 기준 프로퍼티가 두 개 이상이면 메서드 이름이 길어지는 단점이 있다.
  * 메서드 이름으로 정렬 순서가 정해지기 때문에 상황에 따라 정렬 순서를 변경할 수도 없다.
* Sort를 인자로 전달
  * Sort를 인자로 전달하여 위 방식의 한계를 극복할 수 있다
  * Sort 타입은 정렬 순서를 제공할 때 사용할 수 있다

```
// 조회 예시
Sort sort = Sort.by("number").ascending().and(Sort.by("orderDate").descending());
List<OrderSummary> results = orderSummaryDao.findByOrdererId("user1", sort);
```

## 5.7 페이징 처리하기

목록을 보여줄 경우 보통 페이징 처리를 한다.  
스프링 데이터 JPA는 페이징 처리를 위해 Pageable 타입을 이용한다.  
Sort 타입과 마찬가지로 find 메서드애 Pageable 타입 파라미터를 사용하면 자동으로 처리해준다.

Pageable 타입은 인터페이스로 실제 Pageable 타입 객체는 PageRequest 클래스를 이용해서 생성한다.  
PageRequest.of() 메서드의 첫 번째 인자는 페이지 번호를, 두 번째 인자는 한 페이지의 개수를 의미한다.  
세 번째 인자로 Sort를 넘기면 정렬 순서를 지정할 수 있다.
````
Pageable pageable = PageRequest.of(1, 10, Sort);
List<MemberData> user = memberDataDao.findByNameLike("사용자%", pageReq);
````
위 예시의 경우 페이지는 0부터 시작하므로 11~20번째 데이터 목록을 조회한다.

Pageable을 사용하는 메서드의 리턴 타입이 Page일 경우 스프링 데이터 JPA는 목록 조회 쿼리와 함께 COUNT 쿼리도 실행해서 조건에 해당하는 데이터 개수를 구한다.
최적화를 위해 COUNT 쿼리를 별도로 작성해서 실행시킬 수도 있다.  
Page는 전체 개수, 페이지 개수 등 페이징 처리에 필요한 데이터도 함께 제공한다.
````
page.getContent(); // 조회 결과 목록
page.getTotalElements(); // 조건에 해당하는 전체 개수
page.getTotalPages(); // 전체 페이지 번호
page.getNumber(); // 현재 페이지 번호
page.getNumberOfElements(); // 조회 결과 개수
page.getSize(); // 페이지 크기
````

프로퍼티를 비교하는 findBy프로퍼티 형식의 메서드는 Pageable 타입을 사용하더라도 리턴 타입이 List면 COUNT 쿼리를 실행하지 않는다.  
만약 페이징 처리와 관련된 정보가 필요 없다면 리턴타입을 List 로 하자.

N개의 데이터가 필요한 상황이라면 Pageable 말고 findFirstN 형식의 메서드를 사용하자.

## 5.8 스펙 조합을 위한 스펙 빌더 클래스

특정 조건에 따라 스펙을 조합해야하는 경우가 있는다.  
if문에 따라 조합하는 건 복잡도가 높으므로 스펙 빌더를 사용하면 간결한 구조를 유지할 수 있다고 한다.  
(사실 내가 보기엔 비슷한 것 같다..)

````
// 스펙 빌더 사용 X
Specification<MemberData> spec = Specification.where(null);
if (searchRequest.isOnlyNotBlocked()) {
    spec = spec.and(MemberDataSpecs.nonBlocked());
}
if (StringUtils.hasText(searchRequest.getName())) {
    spec = spec.and(MemberDataSpecs.nameLike(searchRequest.getName()));
}
List<MemberData> result = memberDataDao.findAll(spec, PageRequest.of(0, 5));
````
````
// 스펙 빌더 사용
Specification<MemberData> spec = SpecBuilder.builder(MemberData.class)
        .ifTrue(
                searchRequest.isOnlyNotBlocked(),
                () -> MemberDataSpecs.nonBlocked())
        .ifHasText(
                searchRequest.getName(),
                name -> MemberDataSpecs.nameLike(searchRequest.getName()))
        .toSpec();
List<MemberData> result = memberDataDao.findAll(spec, PageRequest.of(0, 5));
````
기본적으로 and(), ifHasText(), ifTrue() 메서드가 있는데 이외에 필요한 메서드를 추가해서 사용하면 된다.

## 5.9 동적 인스턴스 생성

JPA는 쿼리 결과에서 임의의 객체를 동적으로 생성할 수 있는 기능을 제공하고 있다.  
select절에서 new 키워드 뒤에 생성할 인스턴스의 완전한 클래스 이름을 지정하고 괄호 안에 생성자에 인자로 전달할 값을 지정한다.

````java
public interface OrderSummaryDao extends Repository<OrderSummary, String> {

    @Query("""    
            select new com.myshop.order.query.dto.OrderView(
                o.number, o.state, m.name, m.id, p.name
            )
            from Order o join o.orderLines ol, Member m, Product p
            where o.orderer.memberId.id = :ordererId
            and o.orderer.memberId.id = m.id
            and index(ol) = 0
            and ol.productId.id = p.id
            order by o.number.number desc
            """)
    List<OrderView> findOrderView(String ordererId);
}
````

조회 전용 모델을 만드는 이유는 표현 영역을 통해 사용자에게 데이터를 보여주기 위함이다.  
동적 인스턴스의 장점은 JPQL을 그대로 사용하므로 객체 기준으로 쿼리를 작성하면서도 동시에 지연/즉시 로딩과 같은 고민 없이 원하는 모습으로 데이터를 조회할 수 있다는 점이다.

## 5.10 하이버네이트 @Subselect 사용

하이버네이트는 JPA 확장 기능으로 @Subselect 를 제공한다.  
@Subselect는 쿼리 결과를 @Entity로 매핑해주는 기능이다.

@Immutable, @Subselect, @Synchronize는 하이버네이트 전용 애너테이션인데 이 태그를 사용하면 테이블이 아닌 쿼리 결과를 @Entity로 매핑할 수 있다.  
@Suselect는 조회 쿼리를 값으로 갖는다.
* 하이버네이트는 이 쿼리의 결과를 매핑할 테이블처럼 사용한다.
* DBMS가 여러 테이블을 조인해서 조회한 결과를 한 테이블처럼 보여주기 위한 용도로 뷰를 사용하는 것처럼 @Subselect를 사용하면 쿼리 실행 결과를 매핑할 테이블처럼 사용한다.

뷰를 수정할 수 없듯이 @Subselect로 조회한 @Entity 역시 수정할 수 없다.
* 실수로 @Subselect를 이용한 @Entity의 매핑 필드를 수정하면 하이버네이트는 update 쿼리를 실행시키지만, 매핑한 테이블이 실제 DB에는 없으므로 에러가 발생한다.
* 이 문제를 발생하기 위해 @Immutable을 사용한다.
* @Immutable을 사용하면 하이버네이트는 해당 엔티티의 매핑 필드/프로퍼티가 변경되도 DB에 반영하지 않고 무시한다.

하이버네이트는 트랜잭션을 커밋하는 시점에 변경사항을 DB에 반영하기 때문에 아래 코드에서 OrderSummary에는 최신 값이 아닌 이전 값이 담기게 된다
````
// purchase_order 테이블에서 조회
Order order = orderRepository.findById(orderNumber);
order.changeShippingInfo(newInfo); // 상태 변경

// 변경 내역이 DB에 반영되지 않았는데 purchase_order 테이블에서 조회
List<OrderSummary> summaries = orderSummaryRepository.findByOrderId(userId);
````

이런 문제를 해소하기 위해 @Synchronize 로 해당 엔티티와 관련된 테이블 목록을 명시한다. 
하이버네이트는 엔티티를 로딩하기 전에 지정한 테이블과 관련된 변경이 발생하면 플러시를 먼저한다.  
따라서 OrderSummary를 로딩하는 시점에서는 변경 내역이 반영된다.

@Subselect를 사용해도 일반 @Entity와 같기 때문에 EntityManager#find(), JPQL, Criteria를 사용해서 조회할 수 있다는 것이 @Subselect의 장점이다.

````
// @Subselect를 적용한 @Entity는 일반 @Entity와 동일한 방법으로 조회할 수 있다.
Specification<OrderSummary> spec = OrderSummarySpecs.orderDateBetween(from, to);
Pageable pageable = PageRequest.of(1, 1);
List<OrderSummary> results = orderSummaryDao.findAll(spec, pageable);
````