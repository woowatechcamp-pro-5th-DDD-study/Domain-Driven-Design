# 제 7장 : 도메인 서비스

---

## 여러 애그리거트가 필요한 기능

### 하나의 애그리거트로 각각의 기능을 모두 구현할 수 있을까?
- 상품 애그리거트
    - 구매하는 상품의 `가격`이 필요 
    - 상품에 따라 `배송비`가 추가되기도 함
- 주문 애그리거트 
    - 상품별로 구매 `개수`가 필요
- 할인 쿠폰 애그리거트
    - 쿠폰별로 지정한 할인 금액이나 비율에 따라 `주문 총금액을 할인`
- 회원 애그리거트
    - 회원 `등급`에 따라 `추가 할인`이 가능


### 그렇다면 위 정보를 모두 활용하는 애그리거트로 만들면 될까??
- 그러면 이 종합된 애그리거트는 `실제 계산 금액`을 계산에 대한 책임
- `총 주문 금액에서 할인 금액`을 계산에 대한 책임  
    -> 다른 애그리거트의 역할을하며 다른 애그리거트의 의존성이 깊어 보인다. 

~~~java
public class Order {
    /*...*/
    private Orderer orderer;
    private List<OrderLine> orderLineList;
    private List<Coupon> usedCoupons;

    private Money calculatePayAmounts() {
        Money totalAmounts = calculatePayAmounts();
        // 쿠폰별로 할인 금액을 구한다
        Money discount = 
                coupons.stream()
                .map(coupon -> calculateDiscount(coupon));
                .reduce(Money(0)), (v1, v2) -> v1.add(v2));
        // 회원에 따라 추가 할인을 구한다.
        Money membershipDiscount = calculateDiscount(orderer.getMember().getGrade());
        // 실제 결제 금액 계산
        return totalAmounts.minus(discount).minus(membershipDiscount);
    }

    private Money caculateDiscount(Coupon coupon) {
        // orderLines의 각 상품에 대해 쿠폰을 적용해서 할인 금액 계산하는 로직
        // 쿠폰의 적용 조건 등을 확인하는 코드
        // 정책에 따라 복잡한 if-else 와 계산 코드
        /*....*/
    }

    private Money calculateDiscount(MemberGrade grade) {
        // ... 등급에 따라 할인 금액 계산
    }
    
    
}
~~~

### 도메인 서비스를 별도로 구현
- 이처럼 한 애그리거트에 포함시키기 애매한 도메인 기능의 경우 특정 애그리거트로 억지로 구현하면 안된다.
- 이러면 애그리거트는 자신의 책임 범위를 넘어서는 기능을 구현하기 떄문에 코드가 길어지고 외부에 대한 의존이 높아지게 된다.
- 이는 결과적으로 코드를 복잡하게 만들어 수정을 어렵게 만든다.
- 이 문제를 가장 쉽게 해결할 수 있는 방법은 도메인 서비스를 별도로 구현하는 것이다.

## 도메인 서비스
- 주로 `계산로직` 혹은 `외부시스템 연동이 필요한 도메인 로직` 을 표현할 때 주로 사용
- 응용 영역의 서비스가 응용 로직을 다룬다면 도메인 서비스는 도메인 로직을 다룬다.
- 도메인 서비스가 도메인 영역의 애그리거트나 밸류와 같은 다른 구성요소와의 차이점이 있다면 상태 없이 로직 만 구현한다는 점이다.
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
- 이 도메인 서비스를 주문 애그리거트에 전달하면 아래와 같은 형태가 되며, 사용 주체는 애그리거트이다.
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
### 도메인 서비스 객체를 애그리거트에 주입하지 않기
- 이러한 애그리거트 객체에 `도메인 서비스를 전달`하는 것은 `응용 서비스의 책임`
- `애그리거트 메서드를 실행`할 때 도메인 서비스를 인자로 전달하지 않고,   
반대로 `도메인 서비스의 기능을 실행`할 때 `애그리거트를 전달`하기도 한다.
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

### Note 내용 
- 도메인 서비스를 애그리거트에 `주입`한다는 것은 `애그리거트`가 `도메인 서비스`에 `의존`한다는 의미가 된다.
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
- 또 모든 기능에서 도메인 서비스를 필요로 하는 것도 아니며 `일부 기능`에서만 필요로 한다.
- 일부 기능을 위해 굳이 도메인 서비스 객체를 애그리거트에 의존 주입하는 것은 개발자의 욕심을 채우는 것에 불과하다. 라고 저자는 설명하고 있다.

### 외부 시스템 연동과 도메인 서비스
- `외부 시스템` 혹은 `타 도메인과`의 `연동 기능`도 하나의 도메인 서비스가 될 수 있음  
- 예)_설문조사시스템과 사용자 역활관리 시스템이 분리되어 있다고 가정
- `설문조사 시스템` -> 사용자가 설문 조사 생성 `권한 여부` 확인 필요 -> 권한여부 확인하는 `도메인 로직`
~~~java
public interface SurveyPermissionChecker {
    boolean hasUserCreationPermission(String userId);
}
~~~
(도메인 로직 관점에서 인터페이스를 작성, 타 서비스 연동한다는 관점으로 작성하지 않음)
- 응용 서비스는 이 도메인 서비스를 이용해서 생성 권한을 검사
~~~java
public class CreateSurveyService {
    private SurveyPermissionChecker permissionChecker;
    
    public long createSurvey(CreateSurveyRequest req) {
        validate(req);
        // 도메인 서비스를 이용해서 외부 시스템 연동을 표현
        if (!permissionChecker.hasUserCreationPermission(req.getRequestorId())) {
            throw new NoPermissionException();
        }
    }
}
~~~

##  도메인 서비스의 패키지 위치 

![https://user-images.githubusercontent.com/43809168/99547542-fe4db380-29fa-11eb-86f8-80e6801fc2b7.png](https://user-images.githubusercontent.com/43809168/99547542-fe4db380-29fa-11eb-86f8-80e6801fc2b7.png)

- 도메인 서비스는 도메인 로직을 수행하므로 `다른 도메인 구성 요소`와 `동일`한 패키지에 위치시킨다.
- 주문에 관련된 도메인 서비스라면 주문 패키지에 같이 위치시킨다.
- 도메인 서비스의 개수가 많거나 엔티티나 밸류와 같은 다른 구성요소와 명시적으로 구분하고 싶다면  
  domain 패키지 아래에 domain.model, domain.service, domain.repository와 같이 하위 패키지를 구분해서 위치시킬 수도 있다.

##  도메인 서비스의 인터페이스와 클래스 

- 도메인 서비스 로직이 고정되어 있지 않은 경우라면 도메인 서비스 자체를 인터페이스로 구현하고 이를 구현한 클래스를 둘 수도 있다.

![https://user-images.githubusercontent.com/43809168/99547954-7b792880-29fb-11eb-9cec-e8974038d28b.png](https://user-images.githubusercontent.com/43809168/99547954-7b792880-29fb-11eb-9cec-e8974038d28b.png)

- 위 그림과 같이 도메인 서비스의 구현이 특정 구현 기술에 의존적이거나 외부 시스템의 API를 실행한다면 도메인 영역의 도메인 서비스는 인터페이스로 추상화해야 한다.
- 이를 통해 도메인 영역이 특정 구현에 종속되는 것을 방지할 수 있고 도메인 영역에 대한 테스트가 수월해진다.
