package com.zippyziggy.search.service;

import com.zippyziggy.search.client.MemberClient;
import com.zippyziggy.search.client.PromptClient;
import com.zippyziggy.search.dto.response.SearchTalk;
import com.zippyziggy.search.dto.response.SearchTalkList;
import com.zippyziggy.search.dto.response.WriterResponse;
import com.zippyziggy.search.dto.response.server.MemberResponse;
import com.zippyziggy.search.dto.response.server.SyncEsTalk;
import com.zippyziggy.search.exception.EsTalkNotFoundException;
import com.zippyziggy.search.model.EsTalk;
import com.zippyziggy.search.repository.EsTalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class EsTalkService {

    private final EsTalkRepository esTalkRepository;
    private final CircuitBreakerFactory circuitBreakerFactory;
    private final PromptClient promptClient;
    private final MemberClient memberClient;

    public SearchTalkList searchTalks(
        String crntMemberUuid,
        String keyword,
        int page,
        int size,
        String sort
    ) {
        final Sort sortBy = Sort.by(Sort.Direction.DESC, sort);
        final Pageable pageable = PageRequest.of(page, size, sortBy);

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");

        final Page<EsTalk> pagedEsTalk = search(keyword, pageable);
        final long totalTalksCnt = pagedEsTalk.getTotalElements();
        final int totalPageCnt = pagedEsTalk.getTotalPages();

        final List<SearchTalk> searchTalks = new ArrayList<>();
        for (EsTalk esTalk : pagedEsTalk) {
            final Long talkId = esTalk.getTalkId();

            // MemberClient에 memberUuid로 요청
            MemberResponse member;
            try {
                 member = circuitBreaker
                        .run(() -> memberClient
                                .getMemberInfo(UUID.fromString(esTalk.getMemberUuid()))
                                .orElseGet(MemberResponse::new));
            } catch (NoFallbackAvailableException e) {
                member = new MemberResponse();
            }
            final WriterResponse writer = member.toWriterResponse();

            // PromptClient에 commentCnt 요청
            final Long commentCnt = circuitBreaker
                    .run(() -> promptClient.getTalkCommentCnt(talkId));

            searchTalks.add(SearchTalk.of(writer, esTalk, commentCnt));
        }
        return SearchTalkList.of(totalTalksCnt, totalPageCnt, searchTalks);
    }

    public void insertDocument(SyncEsTalk syncEsTalk) {
        final EsTalk esTalk = EsTalk.of(syncEsTalk);
        esTalkRepository.save(esTalk);
    }

    public void deleteDocument(Long talkId) {
        final EsTalk esTalk = esTalkRepository
                .findEsTalkByTalkId(talkId)
                .orElseThrow(EsTalkNotFoundException::new);
        esTalkRepository.delete(esTalk);
    }

    public void updateHit(Long talkId, Long hit) {
        final EsTalk esTalk = esTalkRepository
            .findEsTalkByTalkId(talkId)
            .orElseThrow(EsTalkNotFoundException::new);
        esTalk.setHit(hit);
        esTalkRepository.save(esTalk);
    }

    public void updateLikeCnt(Long talkId, Long likeCnt) {
        final EsTalk esTalk = esTalkRepository
            .findEsTalkByTalkId(talkId)
            .orElseThrow(EsTalkNotFoundException::new);
        esTalk.setLikeCnt(likeCnt);
        esTalkRepository.save(esTalk);
    }

    private Page<EsTalk> search(
        String keyword,
        Pageable pageable
    ) {
        Page<EsTalk> pagedEsTalk;
        keyword = (keyword.equals("")) ? null : keyword;

        if (null != keyword) {
            pagedEsTalk = esTalkRepository
                .findByKeywordOnly(keyword, pageable);

        } else {
            pagedEsTalk = esTalkRepository
                .findAll(pageable);
        }
        return pagedEsTalk;
    }

}
