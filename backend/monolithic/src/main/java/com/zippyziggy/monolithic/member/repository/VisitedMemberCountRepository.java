package com.zippyziggy.monolithic.member.repository;

import com.zippyziggy.monolithic.member.model.VisitedMemberCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitedMemberCountRepository extends JpaRepository<VisitedMemberCount, Long> {
    // 날짜로 해당 객체 불러오기
    VisitedMemberCount findByNowDate(String nowDate);

    List<VisitedMemberCount> findAllByNowDateContains(String dateTime);

}
