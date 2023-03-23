# 제 7장 : 도메인 서비스
## 여러 애그리거트가 필요한 기능
애그리거트로 개발을 하다보면 때때로 하나의 애그리거트로 기능을 구현할 수 없을 때가 종종 있다.
- 상품 애그리거트 : 구매하는 상품의 가격이 필요하다. 또한 상품에 따라 배송비가 추가되기도 한다.
- 주문 애그리거트 : 상품별로 구매 개수가 필요하다.
- 할인 쿠폰 애그리거트 : 쿠폰별로 지정한 할인 금액이나 비율에 따라 주문 총금액을 할인한다.
- 회원 애그리거트 : 회원 등급에 따라 추가 할인이 가능하다

- 이 상황에서 어떤 의문점이 생길 수 있을까?
- 실제 계산 금액을 계산해야 하는 애그리거트는 누구인가?
- 총 주문 금액에서 할인 금액을 계산해야 하는데 이 할인 금액을 구하는 것은 누구의 책임일까?

- 이처럼 한 애그리거트에 포함시키기 애매한 도메인 기능의 경우 특정 애그리거트로 우겨넣어서 구현하면 안된다.
- 이러면 애그리거트는 자신의 책임 범위를 넘어서는 기능을 구현하기 떄문에 코드가 길어지고 외부에 대한 의존이 높아지게 된다.
- 이는 결과적으로 코드를 복잡하게 만들어 수정을 어렵게 만든다.
- 이 문제를 가장 쉽게 해결할 수 있는 방법은  도메인 서비스 를 별도로 구현하는 것이다.

## 도메인 서비스
- 응용 영역의 서비스가 응용 로직을 다룬다면 도메인 서비스는 도메인 로직을 다룬다.
- 도메인 서비스가 도메인 영역의 애그리거트나 밸류와 같은 다른 구성요소와의 차이점이 있다면 상태 없이  로직 만 구현한다는 점이다.
- 도메인 서비스를 구현하는데 필요한 상태는 애그리거트나 다른 방법으로 전달받는다.

~~~java
public class DiscountCalculationService {
    public Money calculateDiscountAmounts(List<OrderLIne> orderLines,List<Coupon> coupons,MemberGrade grade) {
        Money couponDiscount = coupons.stream()
                                .map(coupon -> calculateDiscount(coupon))
                                .reduce(Money(0), (v1, v2) - v1.add(v2));
        Money membershipDiscount = calculateDiscount(orderer.getMember().getGrade());
        return couponDiscount.add(membershipDiscount);
    }
    /*...*/

~~~
- 할인 계산 서비스를 사용하는 주체는 애그리거트가 될 수도 있고 응용 서비스가 될 수도 있다.
- 이 도메인 서비스를 주문 애그리거트에 전달하면 다음과 같은 형태가 된다.
~~~java
public class Order {
    public void calculateAmounts(DiscountCalculationService disCalSvc, MemberGrade grade) {
        Money totalAmounts = getTotalAmounts();
        Money discountAmounts =
                disCalSvc.calculateDiscountAmounts(this.orderLInes, this.coupons, greade);
        this.paymentAmounts = totalAmounts.minus(discountAmounts);
    }
    ... 
}
~~~
- 이 경우 사용 주체는 애그리거트가 된다.
~~~java
public class OrderService {
    private DiscountCalculationService discountCalculationService;

    @Transactional
    public OrderNo placeOrder(OrderRequest orderRequest) {
        OrderNo orderno = orderRepository.nextId();
        Order order = createOrder(orderNo, orderRequest);
        orderRepository.save(order);
        // 응용 서비스 실행 후 표현 영역에서 필요한 값 리턴

        return orderNo;
    }

    private Order createOrder(OrderNo orderNo, OrderRequest orderReq) {
        Member member =findMember(orderReq.getOrdererId());
        Order order = new Order(orderNo, orderReq.gerOrderLines(),
                orderReq.getCoupons(), createOrderer(member),
                orderReq.getShippingInfo());
        order.calculateAmounts(this.discountCalculationService, member.getGrade());
        return order;
    }
	...
}
~~~
- 이러한 애그리거트 객체에 도메인 서비스를 전달하는 것은 순전히 응용 서비스의 책임이다.
- 애그리거트 메서드를 실행할 때 도메인 서비스를 인자로 전달하지 않고 반대로 도메인 서비스의 기능을 실행할 때 애그리거트를 전달하기도 한다.
- 이런 식으로 동작하는 것 중 하나가 계좌 이체 기능이다.
- 계좌 이체의 경우 두 계좌 애그리거트가 관여하는데 한 애그리거트는 금액을 출금하고, 한 애그리거트는 금액을 입금한다.
- 이를 위한 도메인 서비스는 다음과 같이 구현할 수 있다.

~~~java
public class TransferService {
	public void transfer(Account fromAcc, Account toAcc, Money amounts) {
		fromAcc.withdraw(amounts);
		toAcc.credit(amounts);
	}
}
~~~
- 응용 서비스는 두 Account 애그리거트를 구한 뒤에 해당 도메인 영역의 Transfer-Service를 이용해 계좌 이체 도메인의 기능을 실행한 것이다.
- 도메인 서비스는 도메인 로직을 수행하지 응용 로직을 수행하지는 않는다.
- 트랜잭션 처리와 같은 로직은 응용로직이므로 도메인 서비스가 아닌 응용 서비스에서 처리한다.

##  도메인 서비스 객체를 애그리거트에 주입하지 않기 
- 도메인 서비스를 애그리거트에 주입한다는 것은 애그리거트가 도메인 서비스에 의존한다는 의미가 된다.
- 스프링의 DI와 같은 의존성 주입 기술에 심취하다 보면 도메인 서비스를 애그리거트에 주입해서 사용하고 싶은 강한 충동에 휩싸이게 된다.
- 그러나 이는 좋은 방향이 아니다.

- 도메인 객체의 필드 (프로퍼티)로 구성된 데이터와 메서드를 이용한 기능을 이용해 개념적으로 하나의모델 을 표현한다.
~~~java
 public class DDD {
    @Autowired
    DomainService  domainService ;
}
~~~
- 만약 도메인 서비스를 DI와 같은 기술을 사용해 주입하려고 하면 이 도메인 서비스 필드는 데이터 자체와는 관련이 없게 된다.
- 또 모든 기능에서 도메인 서비스를 필요로 하는 것도 아니며 일부 기능에서만 필요로 한다.
- 일부 기능을 위해 굳이 도메인 서비스 객체를 애그리거트에 의존 주입하는 것은 개발자의 욕심을 채우는 것에 불과하다. (저자의 생각)

##  도메인 서비스의 패키지 위치 

![https://user-images.githubusercontent.com/43809168/99547542-fe4db380-29fa-11eb-86f8-80e6801fc2b7.png](https://user-images.githubusercontent.com/43809168/99547542-fe4db380-29fa-11eb-86f8-80e6801fc2b7.png)

- 도메인 서비스는 도메인 로직을 수행하므로 다른 도메인 구성 요소와 동일한 패키지에 위치시킨다.
- 가령 주문에 관련된 도메인 서비스라면 주문 패키지에 같이 위치시킨다.
- 도메인 서비스의 개수가 많거나 엔티티나 밸류와 같은 다른 구성요소와 명시적으로 구분하고 싶다면 domain 패키지 아래에 domain.model, domain.service, domain.repository와 같이 하위 패키지를 구분해서 위치시켜도 좋다.

##  도메인 서비스의 인터페이스와 클래스 

- 도메인 서비스 로직이 고정되어 있지 않은 경우라면 도메인 서비스 자체를 인터페이스로 구현하고 이를 구현한 클래스를 둘 수도 있다.

![https://user-images.githubusercontent.com/43809168/99547954-7b792880-29fb-11eb-9cec-e8974038d28b.png](https://user-images.githubusercontent.com/43809168/99547954-7b792880-29fb-11eb-9cec-e8974038d28b.png)

- 위 그림과 같이 도메인 서비스의 구현이 특정 구현 기술에 의존적이거나 외부 시스템의 API를 실행한다면 도메인 영역의 도메인 서비스는 인터페이스로 추상화해야 한다.
- 이를 통해 도메인 영역이 특정 구현에 종속되는 것을 방지할 수 있고 도메인 영역에 대한 테스트가 수월해진다.
