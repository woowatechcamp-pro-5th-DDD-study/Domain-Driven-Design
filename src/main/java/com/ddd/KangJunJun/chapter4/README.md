# Chapter 4 - 리포지터리와 모델 구현

## 4.1 JPA 를 이용한 리포지터리 구현

### 4.1.1 모듈 위치

리포지터리 인터페이스 --> 도메인 영역  
리포지터리 구현 클래스(ex. JpaModelRepository) --> 인프라 영역

리포지터리 구현 클래스를 인프라스트럭처 영역에 위치 시켜서 인프라스트럭처에 대한 의존을 낮춰야 한다. (DIP)

### 4.1.2 리포지터리 기본 기능 구현

리포지터리 기본 기능
* ID로 애그리거트 조회하기
* 애그리거트 저장하기

````java
public interface OrderRepository {
    Order findById(OrderNo no);
    void save(Order order);
}
````

인터페이스는 애그리거트 루트를 기준으로 작성한다.  
스프링과 JPA로 구현한다면 리포지터리 구현 클래스는 스프링 데이터 JPA 가 알아서 만들어준다.  
애그리거트를 수정한 결과도 JPA 를 사용하면 트랜잭션 범웨에서 변경한 데이터를 자동으로 DB에 반영하기 때문에 메서드를 별도 추가할 필요 없다.

@Transaction 어노테이션을 단 서비스 레이어의 메서드를 실행한 결과로 애그리거트가 변경되면 JPA 는 자동으로 Update 쿼리를 실행한다.

ID가 아닌 다른 조건으로 애그리거트를 조회할 때는 findBy 뒤에 해당 프로퍼티명을 붙인다.

````java
public interface OrderRepository {
    List<Order> findByUserId(String userId, int startRow, int size);
    void save(Order order);
}
````

ID 외의 조건으로 조회할 경우 JPA의 Criteria OR JPQL 을 사용 가능하다.

JPQL 이란?  
- JPQL 은 SQL 이 아니라 객체 지향 쿼리 언어이다. 테이블을 대상으로 조회하는것이 아니다.
- JPQL 은 특정 데이터베이스에 의존하지 않는다.
- JPQL 은 결국 SQL 로 변환된다.

Criteria 란?  
- Criteria 는 JPQL 의 작성을 도와주는 빌더 클래스이다. 
- 문자열로 JPQL 을 작성하면 런타임이 되어야 문법 오류를 알 수 있지만 Criteria 는 자바 코드 기반이기 때문에 안전하게 JPQL 을 작성할 수 있다.


## 4.2 스프링 데이터 JPA 를 이용한 리포지터리 구현

스프링 데이터 JPA 는 지정한 규칙에 맞게 리포지터리 인터페이스를 정의하면 리로지터리를 구현한 객체를 자동으로 만들어 스프링 빈으로 등록해 준다.

## 4.3 맵핑 구현

### 4.3.1 엔티티와 밸류 기본 맵핑 구현

* 애그리거트 루트는 엔티티이므로 @Entity 로 매핑 설정한다.
* 밸류는 @Embeddable 로 맵핑 한다.
* 밸류 타입 프로퍼티는 @Embedded 로 맵핑한다.

![](image/orderERM.png)

위 그림처럼 한 테이블로 맵핑 가능한 애그리거트는 Entity 와 Value 로 구성되어 있으며, Value는 @Embeddable 로 매핑한다.

### 4.3.2 기본 생성자



### 4.3.3 필드 접근 방식 사용

### 4.3.4 AttributeConverter 를 이용한 밸류 매핑 처리

### 4.3.5 밸류 컬렉션: 별도 테이블 매핑

### 4.3.6 밸류 컬랙션: 한개 컬럼 매핑

### 4.3.7 밸류를 이용한 ID 매핑

### 4.3.8 별도 테이블에 저장하는 밸류 매핑

### 4.3.9 밸류 컬렉션을 @Entity 로 매핑하기

### 4.3.10 ID 참조와 조인 테이블을 이용한 단방향 M-N 매핑


## 4.4 애그리거트 로딩 전략

## 4.5 애그리거트의 영속성 전파

## 4.6 식별자 생성기능

## 4.7 도메인 구현과 DIP