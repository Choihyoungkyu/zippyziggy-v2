package com.zippyziggy.prompt.prompt.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zippyziggy.prompt.prompt.dto.response.RatingDto;
import com.zippyziggy.prompt.prompt.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PromptRepositoryImpl implements PromptCustomRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<RatingDto> findAllByCategory(Category category) {
        QPrompt qPrompt = QPrompt.prompt;
        QRating qRating = QRating.rating;

        return jpaQueryFactory.select(Projections.constructor(RatingDto.class,
                        qPrompt.id,
                        qPrompt.hit,
                        qPrompt.likeCnt,
                        qRating.score
                        ))
                .from(qPrompt)
                .leftJoin(qRating)
                .on(qPrompt.id.eq(qRating.id))
                .where(qPrompt.category.eq(category).and(qPrompt.statusCode.eq(StatusCode.OPEN)))
                .fetch();
    }

}
