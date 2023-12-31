package com.zippyziggy.prompt.talk.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zippyziggy.prompt.talk.model.Talk;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TalkRepository extends JpaRepository<Talk, Long> {
	Page<Talk> findAllByPromptPromptUuid(UUID promptUuid, Pageable pageable);

	long countAllByPromptPromptUuid(UUID promptUuid);

	List<Talk> findAllByMemberUuid(UUID memberUuid);

	Page<Talk> findTalksByMemberUuid(UUID memberUuid, Pageable pageable);

	@Modifying
	@Query("update Talk set hit = hit + 1 where id = :talkId")
	int updateHit(@Param(value = "talkId") Long talkId);
}
