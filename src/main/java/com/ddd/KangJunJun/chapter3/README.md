# Chapter 3 - 애그리거트

## 3.1 애그리거트

애그리거트는 관련 객체를 하나로 묶은 군집.

도메인 모델의 규모가 커질수록 코드가 복잡해지고 요구사항을 반영하기 어려워진다. <br />
이를 해결하기 위한 방법 중 하나가 애그리거트 이다. 구조를 큰 범위의 관점으로 이해하기 쉬워지고 일관성을 관리하기에도 용의해진다.

한 애그리거트에 속한 객체는 유사하거나 동일한 라이프 사이클을 갖는다. <br />
A가 B를 갖는다는 연관관계가 있더라도 항상 A와 B가 같은 애그리거트에 속하는 것은 아니다. 

## 3.2 애그리거트 루트와 역할

도메인 규칙을 지키려면 애그리거트에 속한 모든 객체가 정상 상태를 가져야 한다. <br />
일관된 상태를 유지하려면 애그리거트 전체를 관리할 주체가 필요하며 이것이 애그리거트 루트다.

### 3.2.1 도메인 규칙과 일관성

애그리거트 루트의 핵심 역할은 일관성이 깨지지 않도록 하는 것이다. <br />
불필요한 중복을 피하고 애그리거트 루트를 통해서만 도메인 로직을 구현하게 만들어야 한다.

일관성을 유지하기 위해서는..? 
- 단순히 필드를 변경하는 set 메서드를 public 으로 만들지 않는다.
- 밸류 타입은 불변으로 구현한다.

```java
public class Order {
    private ShippingInfo shippingInfo;
    public void changeShippingInfo(ShippingInfo newShippingInfo) {
        //...
        setShippingInfo(newShippingInfo);
    }

    private void setShippingInfo(ShippingInfo newShippingInfo) {
        // set 메서드는 private 로만 사용하고 VO인 경우 불변으로 set을 사용하지 말아야 한다.
        this.shippingInfo = newShippingInfo;
    }
}        
```
즉 캡슐화를 이용하여 애그리거트 전체의 의 일관성을 유지한다.


### 3.2.2 애그리거트 루트의 기능 구현

애그리거트 루트는 애그리거트 내부 다른 객체를 조합하는 방식으로 기능을 구현한다.  <br />
애그리거트 루트는 구성요소의 상태 참조 및 기능 실행을 위임하기도 한다.

### 3.2.3 트랜잭션 범위

트랜잭션 범위는 작을수록 좋다. 한 트랜잭션에서는 한 개의 애그리거트만 수정해야 한다.  <br />
한번에 수정하는 애그리거트 수가 많아질수록 전체 처리량이 떨어지게 된다.

부득이하게 한 트랜잭션으로 두 개 이상의 애그리거트를 수정해야 한다면 애그리거트에서 다른 애그리거트를 직접 수정하지 말고 응용 서비스에서 두 애그리거트를 수정하도록 구현한다. <br />
( 도메인 이벤트를 사용하면 한 트랜잭션에서 한 애그리거트를 수정하면서 동기or비동기로 다른 애그리거트의 상태를 변경할 수 있다.)


## 3.3 애그리거트와 리포지터리

리포지터리는 애그리거트 단위로 존재한다. 

애그리거트는 개념적으로 하나이므로 리포지터리는 애그리거트 전체를 저장소에 영속화 해야한다. <br />
애그리거트를 저장할 떄 루트와 매핑되는 테이블 뿐 아니라 애그리거트에 속한 모든 구성요소에 매핑된 테이블에 데이터를 저장해야한다. <br />
조회 역시 리포지터리 메서드는 완전한 애그리거트를 제공해야 한다.

리포지터리가 완전한 애그리거트를 제공하지 않으면 오류 문제가 발생 할 수 있다.

## 3.4 ID를 이용한 애그리거트 참조

한 객체가 다른 객체를 참조하는 것처럼 애그리거트도 다른 애그리거트를 참조한다. 참조 역시 애그리거트 루트를 참조한다는 것과 같다. <br />
필드를 이용해 다른 애그리거트를 직접 참조하는 것은 구현의 편리함을 제공한다.

직접 참조할 경우의 문제점
1. 애그리거트가 괸리하는 범위는 자기 자신으로 한정해야하는데, 다른 애그리거트에서 객체에 접근 할 수 있게되면 편리함으로 인해 다른 애그리거트를 수정하게 되는 일이 발생 할 수있다.
2. 성능에 대한 고민을 해야한다. 연관 맵핑이나 쿼리의 로딩 전략 등을 고려해서 결정해야 한다.
3. 유연한 확장을 위한 시스템 분리가 어려워 진다.

ID 참조를 사용하면 모든 객체가 참조로 연결되지 않고 애그리거트 간 물리적인 연결을 제거하여 복잡도를 낮춰주고 의존을 제거하므로 응집도를 높히는 효과도 있다. <br />
구현 복잡도도 낮아지고 eager / lazy 로딩에 대한 고민을 하지 않아도 된다. 참조하는 애그리거트가 필요하면 서비스에서 ID를 이용해 로딩하면 된다.

### 3.4.1 ID를 이용한 참조와 조회 성능

ID로 차몾하는 경우 여러 애그리거트를 읽을 때 조회 속도가 문제 될 수 있다. <br />
각 조회 건마다 연결된 애그리거트를 읽어온다면 join 한번으로 될 것을 매 건마다 ID로 조회하는 쿼리를 실행하게 된다. ( N+1 이슈 )

이를 해결하려면 조회 전용 쿼리를 사용하면 된다. <br />
애그리거트 마다 서로 다른 저장소를 사용한다면 성능을 높이기 위해 캐시를 사용하거나 조회 전용 저장소를 별도로 구성하는 방법도 있다.


## 3.5 애그리거트 간 집합 연관

개념적으로 존재하는 애그리거트 간의 1:N 연관을 실제 구현에 반영하는것이 요구사항을 충족하는 것과 상관없는 경우가 종종 있다.

자신이 속한 카테고리를 N:1로 연관지어 구할 수도 있다.

## 3.6 애그리거트를 팩토리로 사용하기

중요한 도메인 로직 처리를 응용 서비스에 노출시키지 않기 위해 애그리거트를 팩토리로 이용할 수 있다.

```
public class Store {
    public Product createProduct(ProductId newProductId, ... ) {
        if (!isBlocked())  throw new StoreBlockedException();      
        return new Product(newProductId, getId(), ...);
    }
}

public class RegisterProductService {
    public ProductId registerNewProduct(NewProductRequest req) {
        Store store = storeRepository.findStoreById(req.getStoreId());
        checkNull(store);
        ProductId id = productRepository.nextId();
        Product product = store.createProduct(id, ...); // Store에서 직접 생성
        productRepository.save(product);
        return id;
    }
}
```
Store 애그리거트의 createProduct()는 Product 애그리거트를 생성하는 팩토리 역할을 한다.

애그리거트를 팩토리로 사용하여 가능 여부를 확인하는 도메인 로직을 변경해도 도메인 영역의 Store만 변경하면 되고 응용 서비스는 영향을 받지 않는다. <br />
즉 의존성을 없애고 도메인의 응집도도 높아졌다.

