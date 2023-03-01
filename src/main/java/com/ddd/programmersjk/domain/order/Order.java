package com.ddd.programmersjk.domain.order;

import com.ddd.programmersjk.domain.member.Member;
import org.hibernate.annotations.Comment;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("주문명")
    private String name;

    @Comment("주소")
    private String address;

    @Comment("주문 상태")
    private OrderStatus orderStatus;

    @Comment("주문 금액")
    @Embedded
    private Money totalAmount;

    protected Order() {}

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Orderer orderer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    public Order(String name, String address, Orderer orderer, List<OrderLine> orderLines) {
        this.name = name;
        this.address = address;
        this.orderer = orderer;
        this.orderLines = orderLines;
    }

    public void changeAddress(String address) {
        if (!canChangeAddress()) {
            throw new IllegalArgumentException("이미 배송이 시작되었습니다.");
        }
        this.address = address;
    }

    private boolean canChangeAddress() {
        return this.orderStatus == OrderStatus.READY;
    }

    public void calculateTotalAmount() {
        int sum = orderLines.stream()
                .mapToInt(OrderLine::calculateAmount)
                .sum();
        this.totalAmount = new Money(sum);
    }

    public Orderer getOrderer() {
        return orderer;
    }

    // 아래와 같이 한 애그리거트에서 다른 애그리거트를 수정하면 안된다.
    public void changeAddressWithMember(String address) {
        changeAddress(address);
        Member member = orderer.getMember();
        member.changeAddress(address);
    }
}
