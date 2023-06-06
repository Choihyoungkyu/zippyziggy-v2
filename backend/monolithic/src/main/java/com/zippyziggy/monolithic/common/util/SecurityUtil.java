package com.zippyziggy.monolithic.common.util;

import com.zippyziggy.monolithic.member.dto.response.MemberInformResponseDto;
import com.zippyziggy.monolithic.member.filter.User.CustomUserDetail;
import com.zippyziggy.monolithic.member.model.Member;
import com.zippyziggy.monolithic.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityUtil {

    private SecurityUtil() {
    }

    @Autowired
    private MemberRepository memberRepository;

    // SecurityContext에 저장된 유저 정보 가져오기
    public Member getCurrentMember() {

        // 인증된 유저 가져오기
        CustomUserDetail principal = (CustomUserDetail) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = principal.getUsername();
        UUID uuid = UUID.fromString(userUuid);

        return memberRepository.findByUserUuid(uuid);
    }

    public UUID getCurrentMemberUUID() {

        CustomUserDetail principal = (CustomUserDetail) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userUuid = principal.getUsername();
        UUID uuid = UUID.fromString(userUuid);

        return null;
    }

    // SecurityContext에 저장된 유저 정보 가져오기
    public MemberInformResponseDto getCurrentMemberInformResponseDto() {

        // 인증된 유저 가져오기
        CustomUserDetail principal = (CustomUserDetail) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String userUuid = principal.getUsername();
        String MemberKey = "member" + userUuid;


        // Redis 캐쉬에 존재하지 않는 경우
        UUID uuid = UUID.fromString(userUuid);
        Member member = memberRepository.findByUserUuid(uuid);

        return MemberInformResponseDto.builder()
                .nickname(member.getNickname())
                .profileImg(member.getProfileImg())
                .userUuid(member.getUserUuid()).build();

    }

}
