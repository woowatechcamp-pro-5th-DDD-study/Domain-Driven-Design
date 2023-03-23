# 7 도메인 서비스

## 7.1 여러 애그리거트가 필요한 기능
도메인 영역의 코드를 작성하다 보면, 한 애그리거트로 기능을 구현할 수 없을 때가 있다. 

대표적인 예가 결제 금액 계산 로직으로 실제 결제 금액을 계산할 때는 다음과 같은 내용이 필요하다.
* 상품 애그리거트 : 구매하는 상품의 가격이 필요하다. 또한 상품에 따라 배송비가 추가되기도 한다.
* 주문 애그리거트 : 상품별로 구매 개수가 필요하다.
* 할인 쿠폰 애그리거트 : 쿠폰별로 지정한 할인 금액이나 비율에 따라 주문 총금액을 할인한다.
* 회원 애그리거트 : 회원 등급에 따라 추가 할인이 가능하다

이 상황에서 결제 금액을 계산해야 하는 주체는 어떤 애그리거트일까? 총 주문 금액을 계산하는 것은 주문 애그리거트가 할 수 있지만 결제 금액은 이야기가 다르다. 총 주문 금액에서 할인 금액을 구하는 것은 누구 책임일까? 할인 쿠폰이 할인 규칙을 갖고 있으니 할인 쿠폰 애그리거트가 계산해야 할까? 하지만 할인 쿠폰을 두 개 이상 적용할 수 있다면 단일 할인 쿠폰 애그리거트로는 총 결제 금액을 계산할 수 없다.

이런 경우 주문 애그리거트가 필요한 데이터를 모두 가지도록 한 뒤 할인 금액 계산 책임을 주문 애그리거트에 할당하는 방법이 존재한다. 하지만 이렇게 한 애그리거트에 넣기 애매한 도메인 기능을 억지로 특정 애그리거트에 구현하면 안 된다. 이 경우 애그리거트는 자신의 책임 범위를 넘어서는 기능을 구현하기 때문에 코드가 길어지고 외부에 대한 의존이 높아지게 된다. 이는 코드를 복잡하게 만들어 수정을 어렵게 만드는 요인이 된다. 이런 문제를 해소하는 가장 쉬운 방법이 하나 있는데 그것은 바로 도메인 서비스를 별도로 구현하는 것이다.

## 7.2 도메인 서비스
도메인 서비스는 도메인 영역에 위치한 도메인 로직을 표현할 때 사용된다.
* 계산 로직: 여러 애그리거트에 걸친 계산 로직, 한 애그리거트에 넣기 복잡한 계산 로직
* 외부 시스템 연동이 필요한 도메인 규칙: 구현하기 위해 타 시스템을 사용해야 하는 도메인 로직

### 7.2.1 계산 로직과 도메인 서비스
한 애그리거트에 넣기 애매한 도메인 개념을 구현하려면 애그리거트에 억지로 넣기보다는 도메인 서비스를 이용해서 도메인 개념을 명시적으로 드러내면 된다. 응용 영역의 서비스가 응용 로직을 다룬다면 도메인 서비스는 도메인 로직을 다룬다.

도메인 서비스가 도메인 영역의 애그리거트나 밸류와 같은 다른 구성요소와 비교할 때
다른 점이 있다면 상태 없이 로직만 구현한다는 점이다. 도메인 서비스를 구현하는 데 필요한 상태는 다른 방법으로 전달 받는다.
```
public class DiscountCalculationService {
    public Money calculateDiscountAmounts(
        List<OrderLine> orderLines,
        List<Coupon> coupons,
        MemberGrade grade) {
        Money couponDiscount = coupons.stream()
            .map(coupon -> calculateDiscount(coupon))
            .reduce(Money(0), (vl，v2) -> v1.add(v2));
        
        Money membershipDiscount = calculateDiscount(orderer.getMember().getGrade());
        return couponDiscount .add(membershipDiscount);
    }

    private Money calculateDiscount(Coupon coupon) {
        ...
    }

    private Money calculateDiscount(MemberGrade grade) {
        ...
    }
}
```
할인 계산 서비스의 주체는 애그리거트가 될 수 있고, 응용 서비스가 될 수 있다. 할인 계산 서비스를 다음과 같이 애그리거트의 결제 금액 계산
기능에 전달하면 사용 주체는 애그리거트가 된다.
```
public class Order {

    public void calculateAmounts(DiscountCalculationService disCalSvc, MemberGrade grade) {
        Money totalAmounts = getTotalAmounts();
        Money discountAmounts = disCalSvc.calculateDiscountAmounts(this.orderLInes, this.coupons, greade);
        this.paymentAmounts = totalAmounts.minus(discountAmounts);
    }
    ...
}
```
애그리거트 객체에 도메인 서비스를 전달하는 것은 응용 서비스 책임이다.
```
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
        Member member = findMember(orderReq.getOrdererId());
        Order order = new Order(orderNo, orderReq.gerOrderLines(),
            orderReq.getCoupons(), createOrderer(member),
            orderReq.getShippingInfo());
        order.calculateAmounts(discountCalculationService, member.getGrade());
        return order;
    }
    ...
}
```
주의사항이 존재하는데 도메인 서비스 객체를 애그리거트에 주입하면 안된다.

애그리거트 메서드를 실행할 때 도메인 서비스를 인자로 전달하지 않고 반대로 도메인 서비스의 기능을 실행할 때 애그리거트를 전달하기도 한다. 그 중 하나가 계좌 이체 기능이다.
```
public class TransferService {
    // 계좌 이체는 두 계좌 애그리거트가 관여하고 하나는 출금 하나는 입금한다.
    public void transfer(Account fromAcc, Account toAcc, Money amounts) {
        fromAcc.withdraw(amounts);
        toAcc.credit(amounts);
    }
    ...
}
```
도메인 서비스는 도메인 로직을 수행하지 응용 로직을 수행하진 않는다. 트랜잭션 처리와 같은 로직은 응용 로직이므로 도메인 서비스가 아닌 응용 서비스에서 처리해야 한다. 도메인 서비스인지 응용 서비스인지 감을 잡기 어려울 때는 도메인 상태 변경을 하거나 상태 값을 계산하는 도메인 로직이면서 한 애그리거트에 넣기 적합하지 않으면 도메인 서비스로 구현하게 된다.

### 7.2.2 외부 시스템 연동과 도메인 서비스
외부 시스템이나 타 도메인과의 연동 기능도 도메인 서비스가 될 수 있다. 설문 조사 시스템과 역할 관리 시스템이 분리되어 있다고 했을 때 설문 조사를 생성할 때 사용자가 생성 권한 역할을 확인하기 위해서 역할 관리 시스템과 연동해야한다.

시스템간 연동은 HTTP API 호출로 이루어질 수 있지만 설문 조사 도메인 입장에선 사용자 권한을 체크하는 도메인 로직으로 볼 수 있다. 중요한 점은 도메인 로직 관점에서 인터페이스를 작성했다는 것이다.
```
public interface SurveyPermissionChecker {
    boolean hasUserCreationPermission(String userId);
}
```
응용 서비스는 도메인 서비스를 이용해서 생성 권한을 검사한다.
```
public class CreateSurveyService {
    private SurveyPermissionChecker surveyPermissionChecker;

    public Long createSurvey(CreateSurveyRequest req) {
        validate(req);
        // 도메인 서비스를 이용해서 외부 시스템 연동을 표현
        if (!surveyPermissionChecker.hasUserCreationPermission(req.getRequestorId())) {
            throw new NoPermissionException();
        }
    }
}
```
SurveyPermissionChecker 인터페이스의 구현체는 인프라 영역에 위치해 연동을 포함한 권한 검사 기능을 구현한다.

### 7.2.3 도메인 서비스의 패키지 위치
도메인 서비스는 도메인 로직을 표현하므로 도메인 서비스의 위치는 다른 도메인 구성요소와 동일한 패키지에 위치한다.

<img src="./그림 7.1.png">

도메인 서비스 개수가 많거나 다른 구성요소와 명시적으로 구분하고 싶을 경우 domain.model,  domain.service, domain.repository와 같이 하위 패키지를 구분하여 위치시켜도 된다.

### 7.2.4 도메인 서비스의 인터페이스와 클래스
도메인 서비스의 로직이 고정되어 있지 않은 경우 도메인 서비스 자체를 인터페이스로 구현하고 이를 구현한 클래스를 둘 수도 있다. 특히 도메인 로직을 외부 시스템이나 별도 엔진을 이용해서 구현해야 할 경우에
인터페이스와 클래스를 분리하게 된다. 도메인 영역에는 도메인 서비스 인터페이스가 위치하고 실제 구현인 구현체는 인프라스트럭처 영역에 위치한다.

<img src="./그림 7.2.png">

도메인 서비스의 구현이 특정 기술에 의존하거나 외부 시스템 API 를 실행한다면 도메인 영역의 도메인 서비스는 인터페이스로 추상화 해야한다. 이를 통해 도메인 영역이 특정 구현에 종속되는 것을 방지할 수 있고
도메인 영역에 대한 테스트가 쉬워진다.