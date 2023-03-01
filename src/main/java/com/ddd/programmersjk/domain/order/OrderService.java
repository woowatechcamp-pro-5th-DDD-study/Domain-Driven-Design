package com.ddd.programmersjk.domain.order;

import com.ddd.programmersjk.domain.member.Member;
import com.ddd.programmersjk.domain.order.Order;
import com.ddd.programmersjk.domain.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void changeAddress(Long id, String address) {
        Order order = findById(id);
        order.changeAddress(address);
        Member member = order.getOrderer().getMember();
        member.changeAddress(address);
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 주문이 없습니다."));
    }
}
