package com.ddd.programmersjk.domain.order;

import com.ddd.programmersjk.domain.member.Member;

import javax.persistence.*;

@Entity
public class Orderer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Member member;

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }
}
