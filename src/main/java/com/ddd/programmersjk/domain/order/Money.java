package com.ddd.programmersjk.domain.order;

import javax.persistence.Embeddable;

@Embeddable
public class Money {
    private int money;

    protected Money() {}

    public Money(int money) {
        this.money = money;
    }
}
