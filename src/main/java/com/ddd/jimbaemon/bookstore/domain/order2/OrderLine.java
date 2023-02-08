package com.ddd.jimbaemon.bookstore.domain.order2;

//한 상품을 한 개 이상 주문할 수 있기 때문에(1:N) 객체로 분리
public class OrderLine {

    private Product product; //주문 상품
    private int price; //상품 가격
    private int quantity; //구매 개수
    private int amounts; //구매 가격 합

    public OrderLine(Product product, int price, int quantity) {
        this.product = product;
        this.price = price;
        this.quantity = quantity;
        this.amounts = calculateAmounts();
    }

    //각 상품의 구매 가격 합은 상품 가격에 구매 개수를 곱한 값이다.
    private int calculateAmounts() {
        return price * quantity;
    }

    public int getAmounts() {
        return amounts;
    }
}
