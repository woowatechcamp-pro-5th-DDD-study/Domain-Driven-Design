package com.ddd.jimbaemon.chapter1.bookstore.domain.order3;

import java.util.Objects;

public class Order {

    private String orderNumber; //특정한 규칙에 따라 생성한 주문 번호
    private Money totalAmounts;
    private ShippingInfo shippingInfo;
    private OrderState state;

    public Order(String orderNumber, Money totalAmounts,
        ShippingInfo shippingInfo, OrderState state) {
        this.orderNumber = orderNumber;
        this.totalAmounts = totalAmounts;
        this.shippingInfo = shippingInfo;
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(orderNumber, order.orderNumber); // 엔티티의 식별자가 같으면 두 엔티티는 같다고 판단할 수 있다.
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderNumber);
    }
}
