package com.planejamentomensal.member.infra;

import com.planejamentomensal.member.application.MemberRepository;
import com.planejamentomensal.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link MemberRepository}.
 *
 * @since 1.0
 */
public interface JpaMemberRepository extends JpaRepository<Member, Long>, MemberRepository {
}
