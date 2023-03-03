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

## 5.8 스펙 조합을 위한 스펙 빌더 클래스

## 5.9 동적 인스턴스 생성

## 5.10 하이버네이트 @Subselect 사용