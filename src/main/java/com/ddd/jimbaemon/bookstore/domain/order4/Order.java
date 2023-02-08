package com.ddd.jimbaemon.bookstore.domain.order4;

import com.ddd.jimbaemon.bookstore.domain.order.ShippingInfo;

public class Order {

    private OrderState state;
    private ShippingInfo shippingInfo;

    public Order(OrderState state, ShippingInfo shippingInfo) {
        this.state = state;
        this.shippingInfo = shippingInfo;
    }

    public void changeShippingInfo(ShippingInfo newShippingInfo) {
        //STEP1과 STEP2가 무슨 의미인지 전혀 전달되지 않는다.
        if (!(state == OrderState.STEP1 || state == OrderState.STEP2)) {
            throw new IllegalStateException("can't change shipping in " + state);
        }
        this.shippingInfo = newShippingInfo;
    }

    private enum OrderState {
        STEP1, STEP2, STEP3, STEP4, STEP5;

        public boolean isShippingChangeable() {
            return false;
        }
    }
}
