package com.ddd.programmersjk.domain.order;

import com.ddd.programmersjk.domain.member.Member;

import javax.persistence.*;

@Entity
public class Orderer2 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 연관 관계가 사라짐
    private Long memberId;

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }
}
