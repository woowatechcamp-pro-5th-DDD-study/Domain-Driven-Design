# 8 애그리거트 트랜잭션 관리

## 8.1 애그리거트와 트랜잭션
한 주문 애그리거트에 대해 운영자는 배송상태로 변경하고 사용자는 배송지 주소를 변경하면 트랜잭션이 필요하다.

<img src="./그림 8.1.png">

트랜잭션마다 리포지터리는 새로운 애그리거트 객체를 생성하므로 운영자 스레드와 고객 스레드는 같은 주문 애그리거트를 나타내는 다른 객체를 구하게 된다.

운영자 스레드와 고객 스레드는 개념적으로 동일한 애그리거트지만 물리적으론 서로 다른 애그리거트 객체를 사용하는데 운영자 스레드에서 배송 상태를 변경해도 고객 스레드에서 사용하는 주문 애그리거트 객체에는 영향이 없기 때문에 고객 스레드에선 배송 전이기 때문에 배송지 변경이 가능한 상황이 발생한다.

두 스레드는 각각 트랜잭션을 커밋할 때 수정한 내용을 DB에 반영한다. 이 시점에서 배송 상태도 변경되고, 배송지 정보보 변경되는데 애그리거트의 일관성이 깨지게 된다.

일관성이 깨지지 않도록 하려면 두 가지 중 하나를 해야한다.
* 운영자가 배송지 정보를 조회하고 상태를 변경하는 동안, 고객이 애그리거트를 수정하지 못하게 막는다.
* 운영자가 배송지 정보를 조회한 이후에 고객이 정보를 변경하면, 운영자가 애그리거트를 다시 조회한 뒤 수정하도록 한다.

애그리거트에 대해 사용할 수 있는 대표적인 트랜잭션 처리 방식에는 선점(Pessimistic) 잠금과
비선점(Optimistic) 잠금의 두 가지 방식이 있다. Pessimistic Lock(비관적 잠금), Optimistic(낙관적 잠금)이라는 용어를 쓰기도 하는데 책에선 선점 잠금과, 비선점 잠금이란 용어를 사용한다.

## 8.2 선점 잠금
`선점 잠금`은 먼저 애그리거트를 구한 스레드가 애그리거트 사용이 끝날 때까지 다른 스레드가 해당 애그리거트를 수정하지 못하게 하는 방식이다.

<img src="./그림 8.2.png">

* 스레드1이 선점 잠금 방식으로 애그리거트를 구한 뒤 이어서 스레드2가 같은 애그리거트를 구하고 있다. 이때 스레드2는 스레드1이 애그리거트에 대한 잠금을 해제할 때까지 블로킹 Blocking된다.
* 스레드1이 수정 후 커밋을 하면 잠금이 해제된다. 이때 대기하고 있던 스레드2가 애그리거트에 접근한다.
* 스레드1이 커밋한 뒤 스레드2가 애그리거트를 구하게 되서 스레드1이 수정한 애그리거트를 보게 된다.

한 스레드가 애그리거트를 구하고 수정하는 동안 다른 스레드가 수정할 수 없으므로 동시에 애
그리거트를 수정할 때 발생하는 데이터 충돌 문제를 해소할 수 있다.

<img src="./그림 8.3.png">

* 운영자 스레드가 먼저 선점 잠금 방식으로 주문 애그리거트를 구하면 운영자 스레드가 잠금을
해제할 때까지 고객 스레드는 대기 상태가 된다.
* 운영자 스레드가 배송 상태로 변경한 뒤 트랜 잭션을 커밋하면 잠금을 해제한다.
* 배송 상태이므로 주문 애그리거트는 배송지 변경 시 에러를 발생하고 트랜잭션은 실패하게 된다. 이 시점에 고객은 "이미 배송이 시작되어 배송지를 변경할 수 없습니다"와 같은 안내 문구를 보게 된다.

선점 잠금은 보통 DBMS 가 제공하는 행 단위 잠금을 사용해서 구현한다. 오라클을 비롯한 다수 DBMS 가 for update 와 같은 쿼리를 사용해서 특정 레코드에 한 사용자만 접근할 수 있는 잠금 장치를 제공한다.

JPA의 EntityManager를 사용할 경우 LockModeType.PESSIMISTIC_WRITE 사용한다. 스프링 데이터 JPA 사용시 @Lock(LockModeType.PESSIMISTIC_WRITE) 사용한다.
* JPA EntityManager는 LockModeType을 인자로 받는 find () 메서드를 제공한다.
LockModeType.PESSIMISTIC_WRITE를 값으로 전달하면 해당 엔티티와 매핑된 테이블을
이용해서 선점 잠금 방식을 적용한다.
    ```
    // JPA EntityManager는 LockModeType을 인자로 받는 find() 메서드를 제공한다.
    Order order = entityManager.find(Order.class, orderNo, LockModeType.PESSIMISTIC_WRITE);
    ```
* JPA 프로바이더와 DBMS에 따라 잠금 모드 구현이 다르다. 하이버네이트의 경우 PESSIMISTIC_WRITE를 잠금 모드로 사용하면 for update 쿼리를 이용해서 선점 잠금을 구현한다.
    ```
    // 스프링 데이터 JPA는 @Lock 애너테이션을 사용해서 잠금 모드를 지정한다.
    public interface MemberRepository extends Repository<Member, MemberId> {
        @Lock(LockModeType.PESSIMISTICJWRITE)
        @Query("select m from Member m where m.id = :id")
        Optional<Member> findByIdForUpdate(@Param("id") MemberId memberId);
    }
    ```

### 8.2.1 선점 잠금과 교착 상태
선점 잠금 기능을 사용할 때는 잠금 순서에 따른 교착 상태(deadlock)가 발생하지 않도록 주의해야한다.

예를 들어 다음과 같은 순서로 두 스레드가 잠금 시도를 한다.
1. 스레드1: A 애그리거트에 대한 선점 잠금 구함
2. 스레드2: B 애그리거트에 대한 선점 잠금 구함
3. 스레드1: B 애그리거트에 대한 선점 잠금 시도
4. 스레드2: A 애그리거트에 대한 선점 잠금 시도

이 순서에 따르면 스레드1은 영원히 B애그리거트에 대한 선점 잠금을 구할 수 없다. 스레드2가 B애그리거트에 대한 잠금을 이미 선점하고 있기 때문인데, 동일한 이유로 스레드2는 A애그리거트에 대한 잠금을 구할 수 없다. 두 스레드는 상대방 스레드가 먼저 선점한 잠금을 구할 수 없어 더 이상 다음 단계를 진행하지 못하게 되고, 스레드1과 스레드2는 `교착 상태`에 빠지게 된다.

선점 잠금에 따른 교착 상태는 상대적으로 사용자 수가 많을 때 발생할 가능성이 높고, 사용자 수가 많아지면 교착 상태에 빠지는 스레드는 더 빠르게 증가하게 된다. 더 많은 스레드가 교착 상태에 빠질수록 시스템은 아무것도 할 수 없는 상태가 된다.

이런 문제가 발생하지 않도록 하려면 잠금을 구할 때 최대 대시 시간을 지정해야 한다. 
JPA에서 선점 잠금을 시도할 때 최대 대기 시간을 지정하려면 다음과 같이 힌트를 사용하면 된다.
```
Map<String, Object> hints = new HashMap<>();
hints.put("javax.persistence.lock.timeout", 2000);
Order order = entityManager.find(Order.class, orderNo, LockModeType.PESSIMISTIC_WRITE, hints);
```
지정한 시간 이내에 잠금을 구하지 못하면 익셉션을 발생시킨다. 
이 힌트를 사용할 때 주의할 점은 DBMS에 따라 힌트가 적용되지 않을 수도 있다는 것이다. 
힌트를 이용할 때에는 사용 중인 DBMS가 관련 기능을 지원하는지 확인해야 한다.

스프링 데이터 JPA는 @QueryHints 애너테이션을 사용해서 쿼리 힌트를 지정할 수 있다.
```
public interface MemberRepository extends Repository<Member, MemberId> { 
    @Lock(LockModeType.PESSIMISTIC少 RITE)
    @QueryHints({
        @QueryHint(name = "javax.persistence.lock.timeout", value = "2000")
    })
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") MemberId memberId);
```

## 8.3 비선점 잠금
선점 잠금이 강력해 보이지만 선점 잠금으로 모든 트랜잭션 충돌 문제가 해결되는 것은 아니다.

<img src="./그림 8.4.png">

1. 운영자는 배송을 위해 주문 정보를 조회한다. 시스템은 정보를 제공한다.
2. 고객이 배송지 변경을 위해 변경 폼을 요청한다. 시스템은 변경 폼을 제공한다.
3. 고객이 새로운 배송지를 입력하고 폼을 전송하여 배송지를 변경한다.
4. 운영자가 1번에서 조회한 주문 정보를 기준으로 배송지를 정하고 배송 상태 변경을 요청한다.

운영자가 배송지 정보 조회 후 배송상태로 변경하는 동안 고객이 배송지를 변경할 경우 선점 잠금으로 해결할 수 없는데 이때 필요한 것이 `비선점 잠금`이다.
비선점 잠금은 `동시에 접근하는 것을 막는 대신 변경한 데이터를 실제 DBMS에 반영하는 시점에 변경 가능 여부를 확인하는 방식`이다.

비선점 잠금을 구현하려면 애그리거트에 버전으로 사용할 숫자 타입 프로퍼티를 추가해야하며 애그리거트를 수정할 때마다 버전으로 사용할 프로퍼티 값이 1씩 증가한다.
```
UPDATE aggtable SET version = version + 1, colx = ?, coly = ? WHERE aggid = ? and version = 현재버전
```
이 쿼리는 수정할 애그리거트와 매핑되는 테이블의 버전 값이 현재 애그리거트의 버전과 동일한 경우에만 수정하며, 수정에 성공 시 버전 값을 1 증가시킨다. 다른 트랜잭션이 먼저 수정해서 버전 값이 변경하면 실패한다.

<img src="./그림 8.5.png">

JPA는 버전을 이용한 비선점 잠금 기능을 지원한다.
버전으로 사용할 필드에 @Version 애너테이션을 붙이고 매핑되는 테이블에 버전을 저장할 컬럼을 추가하면 된다.
```
@Entity
@Table(name = "purchage_order")
@Access(AccessType.FIELD)
public class Order {
    @EmbeddedId
    private OrderNo number;

    @Version  
    private long version;
  
    ...
}
```

JPA는 엔티티가 변경되어 쿼리를 싱핼할 때 @Version에 명시한 필드를 이용해서 비선점 잠금 쿼리를 실행한다.
애그리거트 객체의 버전이 같은 경우에만 데이터를 수정한다.
```
UPDATE purchase_order SET ".생략，version = version + 1 WHERE number = ? and version = 10
```

응용 서비스는 버전을 알 필요가 없다. 
리포지터리에서 필요한 애그리거트를 구하고 알맞은 기능만 실행하면 된다.
기능 실행 과정에서 애그리거트 데이터가 변경되면 JPA는 트랜잭션 종료 시점에 비선점 잠금을 위한 쿼리를 실행한다.
```
@Transactional
public void changeshipping(ChangeShippingRequest changeReq) {
    Order order = orderRepository.findById(new OrderNo(changeReq.getNumber())); 
    checkNoOrder(order);
    order.changeShippinglnfo(changeReq.getShippinglnfoO);
}
...
```
비선점 잠금을 위한 쿼리를 실행할 때 쿼리 실행 결과로 수정된 행의 개수가 0이면
이미 누군가 앞서 데이터를 수정한 것이다. 
이는 트랜잭션이 충돌한 것이므로 트랜잭션 종료 시점에 OptimisticLockingFailureException 익셉션이 발생한다.
```
@PostMapping ("/changeshipping")
public String changeShipping(ChangeShippingRequest changeReq) {
    try { 
        changeShippingService.changeShipping(changeReq); 
        return "changeShippingSuccess";
    } catch (OptimisticLockingFailureException ex) {
        // 누군가 먼저 같은 주문 애그리거트를 수정했으므로
        // 트랜잭션이 충돌했다는 메시지를 보여준다.
        return "changeShippingTxConflict";
    }
}
...
```

버전이 동일한 경우에만 애그리거트 수정 기능을 수행하도록 함으로써 트랜잭션 충돌 문제를 해소할 수 있다.
비선점 잠금 방식을 여러 트랜잭션으로 확장하려면 애그리거트 정보를 뷰로 보여줄 때 버전 정보도 함께 사용자 화면에 전달해야 한다.
응용 서비스에서 전달받은 버전 값을 이용해서 애그리거트 버전이 일치하는지 확인하고, 일치하는 경우에만 기능을 수행한다.
* VersionConflictException: 이미 누군가가 애그리거트를 수정했다는 것을 의미
* OptimisticLockingFailureException: 누군가가 거의 동시에 애그리거트를 수정했다는 것을 의미

### 8.3.1 강제 버전 증가
애그리거트에 애그리거트 루트 외에 다른 엔티티가 존재하는데 기능 실행 도중 루트가 아닌 다른 엔티티의 값만 변경된다고 하자 
연관된 엔티티의 값이 변경된다고 해도 루트 엔티티 자체의 값은 바뀌는 것이 없으므로 버전 값을 갱신하지 않는다. 
따라서 애그리거트 내에 어떤 구성요소의 상태가 바뀌면 루트 애그리거트의 버전 값을 증가해야 비선점 잠금이 올바르게 동작한다.

JPA는 이런 문제를 처리할 수 있도로 EntityManager.find() 메서드로 엔티티를 구할때 강제로 버전 값을 증가시키는 잠금 모드를 지원하고 있다.
```
Repository
public class JpaOrderRepository implements OrderRepository {
	@PersistenceContext
	private EntityMangager entityManager;

	@Override
	public Order findbyIdOptimisticLockMode(OrderNo id) {
		return entityManager.find(Order.class, id, LockModeType.OPTIMISTTIC_FORCE_INCREMENT);
	}
...
```
LockModeType.OPTIMISTTIC_FORCE_INCREMENT를 사용하면 해당 엔티티의 상태가 변경되었는지에 상관없이 트랜잭션 종료 시점에 버전 값 증가 처리를 한다.
이 잠금 모드를 사용하면 루트 엔티티가 아닌 다른 엔티티나 밸류가 변경되더라도 버전 값을 증가시킬 수 있으므로 비선점 잠금 기능을 안전하게 적용할수 있다.
스프링 데이터 JPA를 사용하면 @Lock 애너테이션을 이용해서 지정하면 된다.

## 8.4 오프라인 선점 잠금
더 엄격하게 데이터 충돌을 막고 싶다면 누군가 수정 화면을 보고 있을 때 수정 화면 자체를 실행하지 못하하도록 해야한다.
한 트랜잭션 범위에서만 적용되는 선점 잠금 방식이나 나중에 버전 충돌을 확인하는 비선점 잠금 방식으로는 이를 구현할 수 없다.
이 때 필요한 것이 오프라인 선점 잠금 방식이다.

단일 트랜잭션에서 동시 변경을 막는 선점 잠금 방식과 달리 오프라인 선점 잠금은 여러 트랜잭션에 걸쳐 동시 변경을 막는다. 첫 번째 트랜잭션을 시작할 때 오프라인 잠금을 선점하고, 마지막 트랜잭션에서 잠금을 해제한다.
잠금을 해제하기 전까지 다른 사용자는 잠금을 구할 수 없다.

<img src="./그림 8.8.png">

잠금을 해제하지 않은 경우 다른 사용자는 영원히 잠금을 구할 수 없는 상황이 발생하기에 오프라인 선점 방식은 잠금 유효 시간을 가져야 한다.
예를 들어 수정 폼에서 1분 단위로 Ajax 호출을 해서 잠금 유효 시간을 1분씩 증가시키는 방법이 있다.

### 8.4.1 오프라인 선점 잠금을 위한 LockManager 인터페이스와 관련 클래스
오프라인 선점 잠금은 크게 네 가지 기능이 필요하다.
* 잠게 잠금 선점 시도
* 잠금 확인
* 잠금 해제
* 잠금 유효시간 연장

LockManager라는 인터페이스에서 기능을 수행한다.
```
public interface LockManager {
    LockId tryLock(String type, String id) throws LockException;
    
    void checkLock(LockId lockId) throws LockException;
    
    void releaseLock(LockId lockId) throws LockException;

    void extendLockExpiration(LockId lockId, long inc) throws LockException;
```

잠금시 반드시 주어진 lockId를 갖는 잠금이 유효한지 검사해야한다.
* 잠금의 유효 시간이 지났으면 이미 다른 사용자가 잠금을 선점한다.
* 잠금을 선점하지 않은 사용자가 기능을 실행했다면 기능 실행을 막아야 한다.

### 8.4.2 DB를 이용한 LockManager 구현
잠금 정보를 저장할 테이블과 인덱스를 생성한다.
```
create table locks (
    `type` varchar(255),
    id varchar(255),
    lockid varchar(255), 
    expiration_time datetime, 
    primary key (`type`, id)
) character set utf8;

create unique index locks idx ON locks (lockid);
```
DB 연동은 스프링이 제공하는 JdbcTemplate를 사용하며, SpringLockManager를 통해서 코드를 구현한다.