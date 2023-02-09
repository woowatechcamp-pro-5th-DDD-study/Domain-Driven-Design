package com.ddd.jimbaemon.bookstore.domain.order3;

public class Money {

    private int value;

    public Money(int value) {
        this.value = value;
    }

    public Money addMoney(Money money) {
        return new Money(this.value + money.value);
    }

    //돈만을 위한 기능 추가
    public Money multiply(int multiplier) {
        return new Money(value * multiplier);
    }
}
