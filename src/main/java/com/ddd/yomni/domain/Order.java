package com.ddd.yomni.domain;

import java.util.ArrayList;
import java.util.List;

import static com.ddd.yomni.domain.OrderState.*;

public class Order {
    private static final String EXCEPTION_MESSAGE_FOR_CANNOT_CHANGE_SHIPPINGINFO = "can't change shipping in ";

    private String orderNumber;
    private Money totalAmounts;
    private List<OrderLine> orderLines = new ArrayList<>();
    private Orderer orderer;
    private DeliveryStatus deliveryStatus;
    private OrderState state;
    private ShippingInfo shippingInfo;
    private PaymentInfo paymentInfo;

    public void changeShippingInfo(ShippingInfo newShippingInfo) {
        verifyNotYetShipped();
        this.shippingInfo = newShippingInfo;
    }

    public void cancel() {
        verifyNotYetShipped();
        this.state = CANCELED;
    }

    private void verifyNotYetShipped() {
        if (state != PAYMENT_WAITING && state != PREPARING) {
            throw new IllegalArgumentException("already shipped");
        }
    }
}
