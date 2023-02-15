package com.ddd.jimbaemon.chapter1.bookstore.domain.order3;

import com.ddd.jimbaemon.chapter1.bookstore.domain.order2.Product;

public class OrderLine {

    private Product product; //주문 상품
    private Money price; //상품 가격 (돈이라는 의미가 명확해진다)
    private int quantity; //구매 개수
    private Money amounts; //구매 가격 합 (돈이라는 의미가 명확해진다)

    public OrderLine(Product product, Money price, int quantity) {
        this.product = product;
        this.price = price;
        this.quantity = quantity;
        this.amounts = price.multiply(quantity);
    }

}
