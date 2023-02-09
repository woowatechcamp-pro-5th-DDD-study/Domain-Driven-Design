package com.ddd.jimbaemon.bookstore.domain.order;

/*
주문 도메인의 경우 '출고 전에 배송지를 변경 할 수 있다' 라는 규칙과 '주문 취소는 배송 전에만 할 수 있다'
라는 규칙을 구현한 코드가 도메인 계층에 위치하게 된다.

핵심 규칙을 구현한 코드는 도메인 모델에만 위치하므로 규칙이 바뀌거나 규칙을 확장해야 할 때 다른 코드에 영향을 덜 주고 변경 내역을 모델에 반영할 수 있게 된다
 */
public class Order {

    private OrderState state;
    private ShippingInfo shippingInfo;

    public Order(OrderState state, ShippingInfo shippingInfo) {
        this.state = state;
        this.shippingInfo = shippingInfo;
    }

    /*
    출고 전에 배송지를 변경할 수 있다
    */
    public void changeShippingInfo(ShippingInfo newShippingInfo) {
        if (!state.isShippingChangeable()) {
            throw new IllegalStateException("can't change shipping in " + state);
        }
        this.shippingInfo = newShippingInfo;
    }

    /*
    큰 틀에서 보면 OrderState 는 Order에 속한 데이터 이므로 정보 변경 가능 여부를 Order로 이동할 수도 있다.

     */
    private boolean isShippingChangeable() {
        return state == OrderState.PAYMENT_WAITING || state == OrderState.PREPARING;
    }

    /*
    주문 취소는 배송 전에만 할 수 있다
     */
    public void cancel() {
        //미안하다 구현은 귀찮았다!!
    }

    private enum OrderState {
        PAYMENT_WAITING {
            public boolean isShippingChangeable() {
                return true;
            }
        },
        PREPARING {
            public boolean isShippingChangeable() {
                return true;
            }
        },
        SHIPPED, DELIVERING, DELIVERY_COMPLETED;

        public boolean isShippingChangeable() {
            return false;
        }
    }
}
