package com.ddd.programmersjk;

import com.ddd.programmersjk.domain.member.Member;
import com.ddd.programmersjk.domain.member.MemberRepository;
import com.ddd.programmersjk.domain.order.Order;
import com.ddd.programmersjk.domain.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService2 {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    public OrderService2(OrderRepository orderRepository, MemberRepository memberRepository) {
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void changeAddress(Long id, String address) {
        Order order = findById(id);
        order.changeAddress(address);
        // memberId를 구헀다 치고
        Member member = findMemberById(1L);
        member.changeAddress(address);
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 주문이 없습니다."));
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 없습니다."));
    }
}
