package com.ddd.programmersjk.domain.member;

import com.ddd.programmersjk.domain.member.Member;
import com.ddd.programmersjk.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> { }
