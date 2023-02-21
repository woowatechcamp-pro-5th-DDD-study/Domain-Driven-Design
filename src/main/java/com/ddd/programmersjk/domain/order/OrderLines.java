package com.ddd.programmersjk.domain.order;

import javax.persistence.Embeddable;
import java.util.List;

@Embeddable
public class OrderLines {
    private List<OrderLine> orderLines;

    public int calculateTotalAmount() {
        return orderLines.stream()
                .mapToInt(OrderLine::calculateAmount)
                .sum();
    }
}
