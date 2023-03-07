# 5장. 스프링 데이터 JPA를 이용한 조회 기능

## 5.1 시작에 앞서
- CQRS란 명령모델과 조회모델을 분리하는 패턴이다.
- 엔티티, 애그리거트에서 살펴본 모델은 주로 상태를 변경할 때 사용된다. 즉 도메인 모델은 명령 모델을 주로 사용한다.

## 5.2 검색을 위한 스펙
- 검색 조건을 다야하게 조합해야 할 때 사용할 수 있는것이 스펙이다. 스펙은 애그리거트가 특정 조건을 충족하는지 검사할 때 사용하는 인터페이스다.

## 5.4 리포지터리/DAO에서 스펙 사용하기
- 스펙을 충족하는 엔티티를 검색하고 싶다면 findAll() 메소드를 사용하면 된다. findAll 메소드느 스펙 인터페이스를 파라미터로 갖는다.
```java
public interface OrderSummaryDao extends Repository<OrderSummary, String> {
    List<OrderSummary> findAll(Specification<OrderSummary> spec);
}
```
- 이 메서드를 사용하는 쪽 코드는 아래와 같다.
```java
Specification<OrderSummary> spec = new OrdererIdSpec("user1");
List<OrderSummary> results = orderSummaryDao.findAll(sepc);
```

## 5.5 스펙 조합
- 스프링 데이터 JPA가 제공하는 스펙 인터페이스는 스펙을 조합하는 메소드를 제공하는데 and, or 메소드이다.
- and 메소드는 두 스펙을 모두 충족하는 스펙을 생성하고 or 메소드는 둘 중 하나 이상 충족하는 스펙을 생성한다.
- 이외에도 not() 메소드, null 여부 검사를 위해 where() 메소드를 사용할 수 있다.

## 5.6 정렬 지정하기
- 스프링 데이터 JPA는 두 가지 방법을 사용해서 정렬을 지정할 수 있다.
  - 메소드 이름에 OrderBy를 사용
  - sort를 인자로 정렬
- 이 중 JPA Repository에 이름에 OrderBy를 사용하는 방법은 간단하지만 정렬 기준 프로퍼티가 두 개 이상이면 메서드 이름이 길어지는 단점이 있다.
- 또한 **메소드 이름으로 정렬 순서가 정해지기 때문에** 동적으로 정렬 순서를 변경할 수 없다. 이럴 때는 sort 타입을 사용하면 된다.
```java
public interface OrderSummaryDao extends Repository<OrderSummary, String> {
    List<OrderSummary> findAllOrdererId(String ordererId, Sort sort);
}
```
- 위에서 find 메소드에 Sort 파라미터를 추가했다. 스프링 데이터 JPA는 파라미터로 받은 Sort를 사용해 정렬쿼리를 만든다.
```java
Sort sort = Sort.by("number").ascending();
List<OrderSummary> results = orderSummaryDao.findByOrdererId("user1", sort);
```

## 5.7 페이징 처리하기
- 전체 데이터 중 일부만 보여주는 페이징 처리는 기본이다. 스프링 데이터 JPA는 이를 위해 Pageable 타입을 이용한다.
- Sort 타입과 마찬가지로 파라미터로 전달하면 페이징을 자동으로 처리해준다.
```java
public interface OrderSummaryDao extends Repository<OrderSummary, String> {
    List<OrderSummary> findAllOrdererId(String ordererId, Pageable pageable);
}
```
- Pageable은 인터페이스므로 실제 타입 객체는 PageRequest를 사용한다.
```java
PageRequest pageRequest = PageRequest.of(1, 10);
List<OrderSummary> results = orderSummaryDao.findByOrdererId("user1", pageRequest);
```
- Page 타입을 사용하면 데이터 목록 뿐만 아니라 전체 개수도 구할 수 있다.
```java
public interface OrderSummaryDao extends Repository<OrderSummary, String> {
  Page<OrderSummary> findAllOrdererId(String ordererId, Pageable pageable);
}
```
