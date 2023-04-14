# 10. 이벤트

## 10.1 시스템간 강결합 문제
- 구매를 취소하면 환불을 처리해야 한다. 이를 도메인 객체에서 처리한다면 아래와 같은 코드를 작성할 수 있다.
```java
public class Order {
    public void cancel(RefundService refundService) {
        verifyNotYetShipped();
        this.state = OrderState.CANCEL;
        this.refundState = RefundState.REFUND_STARTED;

        try {
            refundService.refund(getPaymentId());
            this.refundStatus = RefundState.COMPLETED;
        } catch (Exception ex) {
            // 예외처리
        }
    }    
}
```
- `RefundService`가 외부의 결제 시스템을 호출한다면 **세 가지 문제**가 발생
  - **외부서비스 장애**시 트랜잭션 처리를 어떻게 해야하는지 결정이 필요
  - 외부 서비스가 환불처리를 30초동안 하면 주문 취소기능도 30초 딜레이
  - 주문로직과 결제로직이 섞이는 문제가발생
- 이런 문제가 발생하는 이유는 **주문과 결제 바운디드 컨텍스트가 강결합**이기 떄문이다. 이런 경우 비동기 이벤트를 사용해 두 시스템간 결합을 낮출수 있다.

## 10.2 이벤트 개요
- 이벤트는 과거에 벌어진 어떤 것을 의미한다. 예를 들어 `주문을 취소할때 메일을 보낸다` 라는 요구사항에서 주문을 취소할 때는 `주문취소됨 이벤트`를 활용해 기능을 구현할 수 있다.

### 10.2.1 이벤트 구성요소
- 도메인 모델에 이벤트를 도입하려면 네개의 구성요소인 `이벤트, 이벤트 생성주체, 이벤트 디스패처, 이벤트 핸들러`를 구현해야 한다.
- **이벤트를 생성하는 주체**는 엔티티, 밸류, 도메인 서비스와 같은 **도메인 객체**이다.
- **이벤트 핸들러**는 발생한 이벤트를 받아 원하는 기능을 처리한다. 예를들어 '주문 취소됨 이벤트'를 전달받은 이벤트 핸들러는 SMS로 취소 사실을 전달할 수 있다.
- `이벤트 생성주체`와 `이벤트 핸들러`를 연결해 주는 것이 **이벤트 디스패처**다. 이벤트 디스패처 구현방식에 따라 동기나 비동기로 실행하게 된다.

### 10.2.2 이벤트의 구성
- 이벤트는 발생한 이벤트 정보를 담는데 보통 `이벤트의 종류, 이벤트 발생시간, 추가데이터`를 포함한다.
- 배송지 변경할 때 발생하는 이벤트를 생각해보자. 이 이벤트는 다음과 같이 작성할 수 있다.
```java
public class ShippingInfoChangedEvent {
    private String orderNumber;
    private long timestamp;
    private ShppingInfo newShippingInfo;
    .
    .
}
```
- **이벤트**는 현재 기준으로 과거에 벌어진 것을 표현하기 때문에 이벤트 이름에는 과거시제를 사용한다.
- 이 이벤트를 생성하는 주체는 Order 애그리거트다. `Events.raise()`라는 디스패처를 통해 이벤트를 전파하는데 아래와 같이 코드를 작성할 수 있다.
```java
public class Order {
    public void changeShippingInfo(ShppingInfo newShppingInfo) {
        verifyNotYetShipped();
        updateShippingInfo(newShppingInfo);
        Events.raise(new ShippingInfoChangedEvent(number, newShppingInfo));
    }
}
```
- 이벤트 핸들러가 필요한 데이터만 담으면 된다. 가령 주문을 취소할때 필요한게 주문 id 하나라면 그것만 보낸다.

#### 10.2.3 이벤트 용도 (자연스럽게 MSA가 떠오름)
- 이벤트는 크게 **두 가지 용도**로 쓰인다. 첫 번째는 **트리거**인데 도메인 상태가 바뀔 때 후처리가 필요하면 이벤트를 사용할 수 있다. 
  ex) 주문을 취소하고 주문 취소 이벤트를 트리거로 후처리인 환불 처리를 진행.
- 두 번쨰 용도는 **데이터 동기화**이다. 배송지를 변경하면 외부 배송 서비스에 배송지 정보를 전달해야 하는데 이벤트를 통해 배송지 정보를 동기화 할 수 있다.

#### 10.2.4 이벤트 장점
- 서로 다른 도메인 로직이 섞이는 것을 방지할 수 있다. 
  - 위에 이벤트를 사용하는 `changeShippingInfo` 함수를 보면 더이상 구매취소에 환불 로직이 없는 것을 볼 수 있다.
- 기능 확장이 용이하다. 
  - 구매 취소시 추가적으로 이메일을 보내고 싶다면 이메일 발송을 처리하는 핸들러를 구현하면 된다. 기능은 확장해도 구매 취소 로직을 수정할 필요가 없다.
  ![img.png](images/EventHandler.png)

## 10.3 이벤트, 핸들러, 디스패처 구현
- 스프링이 제공하는 ApplicationEventPublisher 를 이용해 구현을 해보자.

### 10.3.1 이벤트 클래스
- **이벤트 자체를 위한 상위 타입**은 존재하지 않기 때문에 원하는 클래스를 이벤트로 사용한다.
- 이름을 결정할 때 **과거시제를 사용**하는 것만 유의하자. OrderCanceledEvent로 정해도 되고 OrderCanceled로 정해도 된다.
- 이벤트 핸들러에서 필요한 데이터를 포함한다.
```java
public class OrderCanceledEvent {
    private String orderNumber;
    public OrderCanceledEvent(String number) {
        this.orderNumber = number;
    }
}
```
- 모든 이벤트가 가지는 공통 프로퍼티가 존재한다면 상위 클래스를 만들어도 된다.
```java
public abstract class Event {
    private long timestamp;
    public Event() {
        this.timestamp = System.currentTimeMillis();
    }
}
```

### 10.3.2 Events 클래스와 ApplicationEventPublisher
- 이벤트 발생과 출판을 위해 스프링이 제공하는 ApplicationEventPublisher를 사용해보자.
```java
public class Events {
    private static ApplicationEventPublisher publisher;
    
    static void setPublisher(ApplicationEventPublisher publisher) {
        Events.publisher = publisher;
    }
    
    public static void raise(Object event) {
        publisher.publishEvent(event);
    }
}
```

### 10.3.3 이벤트 발생과 이벤트 핸들러
- 이벤트를 발생시킬 코드는 Events.raise() 메소드를 사용한다.
```java
public class Order {
    public void cancel() {
        verifyNotYetShipped();
        this.state = OrderState.CANCEL;
        Events.raise(new OrderCanceledEvent(number));
    }
}
```
- 이벤트를 처리할 핸들러는 `@EventListener 애너테이션`을 사용해서 구현한다.
```java
@Service
public class OrderCanceledEventHandler {
    private RefundService refundService;
    
    public OrderCanceledEventHandler(RefundService refundService) {
        this.refundService = refundService;
    }
    
    @EventListener(OrderCanceledEvent.class)
    public void handle(OrderCanceledEvent event) {
        refundService.refund(event.getOrderNumber());
    }
}
```

### 10.3.4 흐름 정리
- 도메인 기능이 실행 -> 도메인에서 `Events.raise()` 를 사용해 이벤트 발생 -> 스프링의 `ApplicationEventPublisher`를 통해 이벤트 출판 -> ApplicationEventPublisher는 `@EventListener`(이벤트타입.class) 에너테이션이 붙은 메서드를 찾아 실행한다.

## 10.4 동기 이벤트 처리 문제
- 이벤트를 사용해서 **주문로직과 결제로직이 섞이는 문제는 해결**했지만 외부서비스에 영향을 받는 문제가 있다.
- 아래 코드에서 `refundService.refund`가 외부 환불 서비스와 연동된다고 가정해보자.
```java
@Transactional
public void cancel(OrderNo orderNo) {
    Order order = findOrder(orderNo);
    order.cancel() // 내부에서 Event 발생 
}

@Service
public class OrderCanceledEventHandler {
  @EventListener(OrderCanceledEvent.class)
  public void handle(OrderCanceledEvent event) {
    refundService.refund(event.getOrderNumber());
  }
}
```
- 여전히 외부 서비스에 장애가 발생하거나 느려진다면 영향을 받는다는 문제가 존재한다.
- 외부 시스템과 연동을 동기로 처리할 때 발생하는 성능과 트랜잭션 문제를 해소하는 방법은 **이벤트를 비동기로 처리**하거나 **이벤트와 트랜잭션을 연계**하는 방법이 있다.

## 10.5 비동기 이벤트 처리
- 우리가 구현해야 할 것 중에서 **A하면 이어서 B를 하라**의 요구사항은 실제로 **A하면 최대 언제까지 B하라**인 경우가 있다.
  - ex) 회원가입하면 이메일로 본인인증을 하라.
- 이처럼 `A하면 최대 언제까지 B 하라로 바꿀 수 있는 요구사항`은 이벤트를 비동기로 처리하는 방식으로 구현할 수 있다. 
- **이벤트를 비동기로 구현할 수 있는 방법**은 다양한데 네 가지 방식을 소개한다.
  - 로컬 핸들러를 비동기로 실행하기
  - 메시지 큐를 사용
  - 이벤트 저장소와 이벤트 포워더 사용하기
  - 이벤트 저장소와 이벤트 제공 API 사용하기

### 10.5.1 로컬 핸들러 비동기 실행
- 이벤트 핸들러를 비동기로 실행하는 방법은 이벤트 핸들러를 별도 스레드로 실행하는 것이다.
- `@SpringBootApplication` 애너테이션에 `@EnableAsync`을 사용해 비동기 기능을 활성화한 뒤 이벤트 핸들러 메소드에 `@Async 애너테이션`을 붙인다.
```java
@SpringBootApplication
@EnableAsync
public class ShopApplication {
    // 메인함수
}

@Service
public class OrderCanceledEventHandler {
    @Async
    @EventListener(OrderCanceledEvent.class)
    public void handle(OrderCanceledEvent event) {
      //...
    }
}
```

## 10.5.2 메시징 시스템 사용
- 비동기로 이벤트를 처리할 때 **카프카나 래빗MQ와** 같은 메시징 시스템을 사용할 수 있다.
- 필요하다면 이벤트를 발생시키는 도메인 기능과 메시지 큐에 이벤트를 저장하는 절차를 한 트랜잭션으로 묶어야 하는데 이떄 **글로벌 트랜잭션**이 필요하다.
![img.png](images/global_transaction.png)
- **글로벌 트랜잭션**은 안전하게 이벤트를 메시지 큐에 전달할 수 있는 장점이 있지만 전체 성능이 떨어진다는 단점도 있다.
- **래빗MQ**의 경우 글로벌 트랜잭션 지원과 클러스트 고가용성을 지원하기 때문에 안정적으로 메시지를 전달할 수 있는 장점이 있다.
- 반대로 **카프카**는 글로벌 트랜잭션을 지원하진 않지만 다른 메시징 시스템에 비해 높은 성능을 보여준다.
> kafka vs rabbitMQ
>
> kafak는 대규모 트래픽 처리에 장점이 부각 <br>
> rabbitMQ는 admin UI를 통한 관리적인 측면 + 다양한 기능구현을 위한 서비스

### 10.5.3 이벤트 저장소를 이용한 비동기 처리

#### 포워더 방식
- 이벤트가 발생하면 DB에 이벤트를 저장한다. 포워더는 별도 스레드에서 주기적으로 DB에서 이벤트를 가져와 이벤트 핸들러를 실행한다. 이 경우 이벤트 발행과 처리가 비동기로 처리된다.
  ![img_1.png](images/forwarder.png)
- 포워더 방식은 도메인의 상태와 이벤트 저장소가 동일 DB를 사용하기 때문에 로컬 트랜잭션으로 처리된다.
- 핸들러가 이벤트 처리에 실패할 경우 포워더는 DB에서 다시 이벤트를 읽어와 핸들러를 실행한다.

#### API 방식
![img_2.png](images/api.png)
- **API 방식과 포워더 방식의 차이점**은 이벤트를 전달하는 방식에 있다. 
  - 포워더는 자신이 이벤트를 가져와서 핸들러에게 전달하고, 이벤트를 어디까지 처리했는지 알아야하는 책임이 포워더에게 있다.
  - API 방식은 외부 핸들러가 이벤트 목록을 가져가기 때문에 어디까지 이벤트를 처리했는지 알아야 하는 책임이 외부 핸들러에게 있다.

![img.png](images/event_class_diagram.png)
- 이벤트는 과거에 벌어진 사건이므로 데이터가 변경되지 않는다. 따라서 이벤트를 추가하는 기능과 조회하는 기능만 제공하고 수정하는 기능은 제공하지 않는다.
- API와 이벤트 포워더 구현을 보면 어떤 이벤트까지 처리했는지(offset)를 포워더는 알고 있고 API는 모르고 있다.

## 10.6 이벤트 적용시 추가 고려사항
- 앞의 `EventEntry`는 **이벤트 발생주체에 대한 정보**가 없기 때문에 해당 기능이 필요한 경우 EventEntry에 발생주체를 추가해야 한다.
- 그 다음 고려할 점은 **포워더에서 전송 실패를 얼마나 허용할 것이냐**에 대한 것이다. 포워더는 이벤트 전송에 실패하면 다시 읽어와 전송을 시도한다. 계속 전송에 실패하면 나머지 이벤트를 전송할 수 없기 때문에 실패한 이벤트의 재전송 횟수 제한을 두어야 한다.
> 처리에 실패한 이벤트는 별도 DB나 메시지 큐에 저장해 분석에 사용한다.
- 그 다음 고려할 점은 **이벤트 손실**이다. 로컬 핸들러를 이용해서 이벤트를 비동기로 처리할 경우 이벤트 처리에 실패하면 이벤트는 유실된다.
- 다음으로 **이벤트 순서**다. **이벤트 발생 순서대로** 외부 시스템에 전달해야 할 경우 이벤트 저장소를 사용하는게 좋다. 이벤트 저장소는 이벤트를 발생 순서대로 저장하고 그 순서대로 이벤트 목록을 제공하기 때문이다. 반면에 메시징 시스템은 사용 기술에 따라 이벤트 발생순서와 전달 순서가 달라질 수 있다.
- 다음으로 **이벤트 재처리**인데 동일한 이벤트를 다시 처리해야할 때 마지막 처리 순서를 기억했다가 이미 처리한 순번의 이벤트가 들어오면 무시하거나 혹은 멱등성을 가지도록 처리할 수 있다.

### 10.6.1 이벤트 처리와 DB 트랜잭션 고려
```text
- 주문을 취소하면 주문취소 이벤트가 발생한다.
- 주문 취소 이벤트 핸들러는 환불 서비스에 환불 처리를 요청한다.
- 환불 서비스는 pg사에 API 호출을 통해 결제를 취소한다.
```
- 위 상황에서 **두 가지 문제**를 고려해 볼 수 있다.
  - 이벤트를 **동기**로 처리할 때, pg사에 결제는 취소가 되었는데 DB 업데이트가 실패한 경우
  - 이벤트를 **비동기**로 처리할 때, DB 업데이트는 성공했는데 비동기로 실행된 pg사 환불이 실패한 경우 
- 결국 **이벤트 처리 실패**와 **트랜잭션 처리 실패**를 함께 고려해야 하는데 경우의 수를 줄이기 위해 트랜잭션이 성공할 때만 이벤트 핸들러를 실행하도록 한다.
- 스프링에는 `@TransactionEventListener 애너테이션`을 지원하는데 옵션에 따라 트랜잭션 커밋이 성공한 뒤 핸들러 메서드를 실행한다.
```java
@TransactionalEventListener(
  classes = OrderCanceledEvent.class, 
  phase = TransactionPhase.AFTER_COMMIT
)
public void handle(OrderCanceledEvent event) {
  // 환불 로직
}
```
- 중간에 에러가 발생해서 **트랜잭션이 롤백**되면 핸들러 메서드는 실행되지 않는다. 즉, 이벤트 핸들러는 실행됬는데 롤백되는 상황은 발생하지 않는다.
- 트랜잭션이 성공할 떄만 이벤트 핸들러를 실행하게 되면 트랜잭션 실패에 대한 경우의 수가 줄어들어 이벤트 처리 실패만 고민하면 된다. 이벤트 특성에 따라 재처리 방식을 결정하면 된다.