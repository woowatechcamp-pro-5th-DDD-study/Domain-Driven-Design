package com.ddd.programmersjk.domain.order;

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

    protected Order() {}

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Orderer orderer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    public Order(String name, Orderer orderer, List<OrderLine> orderLines) {
        this.name = name;
        this.orderer = orderer;
        this.orderLines = orderLines;
    }
}
