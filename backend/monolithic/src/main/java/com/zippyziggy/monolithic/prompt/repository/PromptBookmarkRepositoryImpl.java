package com.zippyziggy.monolithic.prompt.repository;

import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zippyziggy.monolithic.prompt.model.Prompt;
import com.zippyziggy.monolithic.prompt.model.QPrompt;
import com.zippyziggy.monolithic.prompt.model.QPromptBookmark;
import com.zippyziggy.monolithic.prompt.model.StatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PromptBookmarkRepositoryImpl implements PromptBookmarkCustomRepository {
    private final JPAQueryFactory queryFactory;


    @Override
    public Page<Prompt> findAllPromptsByMemberUuid(UUID memberUuid, Pageable pageable) {

        QPrompt qPrompt = QPrompt.prompt;
        QPromptBookmark qPromptBookmark = QPromptBookmark.promptBookmark;

        JPQLQuery<Prompt> query = queryFactory.selectFrom(qPrompt)
                .leftJoin(qPromptBookmark)
                .on(qPromptBookmark.prompt.id.eq(qPrompt.id))
                .distinct()
                .where(qPromptBookmark.memberUuid.eq(memberUuid)
                        .and(qPrompt.statusCode.eq(StatusCode.OPEN)));

        long totalCount = query.fetchCount();

        List<Prompt> promptList = query.orderBy(qPromptBookmark.regDt.desc().nullsLast()) // nullsLast() 메서드 추가
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(promptList, pageable, totalCount);
    }

}
