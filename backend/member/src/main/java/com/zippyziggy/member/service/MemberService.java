package com.zippyziggy.member.service;

import com.zippyziggy.member.dto.request.MemberSignUpRequestDto;
import com.zippyziggy.member.model.Member;
import com.zippyziggy.member.model.Platform;
import com.zippyziggy.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.NonUniqueResultException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    // 기존 회원인지 검증
    public boolean memberCheck(Platform platform, String platformId) throws Exception {
        Optional<Member> member = memberRepository.findByPlatformAndPlatformId(Platform.KAKAO, platformId);
        if (member.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    // 회원가입
    // 유저가 이미 존재한다면 에러 발생
    public void memberSignUp(MemberSignUpRequestDto dto) throws Exception {

        String nickname = dto.getNickname();

        Optional<Member> checkMember = memberRepository.findByNicknameEquals(nickname);

        if (checkMember.isEmpty()) {
            Member member = Member.builder()
                    .name(dto.getName())
                    .nickname(nickname)
                    .platform(dto.getPlatform())
                    .platformId(dto.getPlatformId())
                    .profileImg(dto.getProfileImg())
                    .userUuid(UUID.randomUUID())
                    .build();

            memberRepository.save(member);
        } else {
            throw new NonUniqueResultException("유저가 이미 존재합니다.");
        }

    }

    // 닉네임 중복 검사
    public boolean nicknameCheck(String nickname) throws Exception {
        Optional<Member> member = memberRepository.findByNicknameEquals(nickname);
        if (member.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

}
