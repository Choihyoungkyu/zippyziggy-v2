package com.zippyziggy.monolithic.member.service;

import com.zippyziggy.monolithic.common.kafka.KafkaProducer;
import com.zippyziggy.monolithic.common.util.SecurityUtil;
import com.zippyziggy.monolithic.member.dto.request.MemberSignUpRequestDto;
import com.zippyziggy.monolithic.member.dto.response.MemberIdResponse;
import com.zippyziggy.monolithic.member.model.JwtToken;
import com.zippyziggy.monolithic.member.model.Member;
import com.zippyziggy.monolithic.member.model.Platform;
import com.zippyziggy.monolithic.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.NonUniqueResultException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {


    private final MemberRepository memberRepository;
    private final JwtProviderService jwtProviderService;
//    private final RedisService redisService;
    private final S3Service s3Service;
    private final SecurityUtil securityUtil;
    private final KafkaProducer kafkaProducer;


    /**
     * 기존 회원인지 검증 서비스
     * @param platform: Kakao, Google
     * @param platformId: 각 플랫폼 아이디
     * @return boolean true, false
     */
    public Member memberCheck(Platform platform, String platformId) throws Exception {
        Optional<Member> member = memberRepository.findByPlatformAndPlatformId(platform, platformId);
        if (member.isEmpty()) {
            return null;
        } else {
            return member.get();
        }
    }

    /**
     * 회원가입
     * 유저가 이미 존재한다면 에러 발생
     */
    public JwtToken memberSignUp(MemberSignUpRequestDto dto, MultipartFile file) {

        try {
            // 보내준 파일이 없을 경우 그냥 플랫폼에서 받은 프로필 이미지를 삽입
            if (file == null || file.isEmpty()) {
                UUID uuid = UUID.randomUUID();

                JwtToken jwtToken = signUpLogic(dto, uuid, dto.getProfileImg());

                return jwtToken;

            } else {
                // 파일을 업로드해서 보내준 경우
                UUID uuid = UUID.randomUUID();

                String imgUrl = s3Service.uploadProfileImg(uuid, file);

                JwtToken jwtToken = signUpLogic(dto, uuid, imgUrl);

                return jwtToken;

            }
        } catch (NonUniqueResultException error) {
          throw  new NonUniqueResultException("유저가 이미 존재합니다.");
        } catch (Exception e) {
            log.error("에러발생 = " + e);
            throw new NullPointerException();
        }


    }

    /**
     * 회원가입되는 로직
     */
    private JwtToken signUpLogic(MemberSignUpRequestDto dto, UUID uuid, String profileImg) throws Exception {
        String nickname = dto.getNickname();

        Member checkMemberPlatform = memberCheck(dto.getPlatform(), dto.getPlatformId());
        if (checkMemberPlatform == null) {
            Member member = Member.builder()
                    .name(dto.getName())
                    .nickname(nickname)
                    .platform(dto.getPlatform())
                    .platformId(dto.getPlatformId())
                    .profileImg(profileImg)
                    .userUuid(uuid)
                    .regDt(LocalDateTime.now())
                    .build();
            memberRepository.save(member);
            return autoLogin(nickname);

        } else {
            // 이미 가입된 회원
            if (checkMemberPlatform.getActivate().equals(true)) {
                throw new NonUniqueResultException("유저가 이미 존재합니다.");
            } else {
                // 가입 이력이 있는 회원이라면
                checkMemberPlatform.setActivate(true);
                checkMemberPlatform.setNickname(nickname);
                checkMemberPlatform.setProfileImg(profileImg);

                return autoLogin(nickname);
            }
        }
    }

    /**
     * 회원가입 후 자동로그인 로직 -> 토큰 부여
     */
    private JwtToken autoLogin(String nickname) {

        // 자동 로그인을 위해 JWT Token 부여
        Optional<Member> newMember = memberRepository.findByNicknameEquals(nickname);

        // Jwt 토큰 반환
        JwtToken jwtToken = jwtProviderService.createJwtToken(newMember.get().getUserUuid());

        // refreshToken DB 저장
        newMember.get().setRefreshToken(jwtToken.getRefreshToken());

        return jwtToken;
    }


    /**
     * 닉네임 중복 검사
     */
    public boolean nicknameCheck(String nickname) throws Exception {
        Optional<Member> member = memberRepository.findByNicknameEquals(nickname);
        if (member.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 회원탈퇴
     */
    public void memberSignOut() throws Exception {

        Member member = securityUtil.getCurrentMember();

        kafkaProducer.send("delete-member-topic", member.getUserUuid());
        memberRepository.delete(member);
//        member.setActivate(false);
//        member.setNickname("");
//        member.setRefreshToken(null);

        s3Service.deleteS3File(member.getProfileImg());

    }


    /**
     * 회원 수정
     */
    public Member updateProfile(String nickname, MultipartFile file, Member member) throws Exception {
        // 닉네임만 바꿀 경우
        if (nickname != null && (file.isEmpty()) ) {
            member.setNickname(nickname);
        } else if (nickname == null && !file.isEmpty()) {
            // 프로필 이미지만 바꿀 경우
            String profileImg = s3Service.uploadProfileImg(member.getUserUuid(), file);
            s3Service.deleteS3File(member.getProfileImg());
            member.setProfileImg(profileImg);
        } else if (nickname != null && !file.isEmpty()) {
            // 둘다 바꿀 경우
            member.setNickname(nickname);
            String profileImg = s3Service.uploadProfileImg(member.getUserUuid(), file);
            s3Service.deleteS3File(member.getProfileImg());
            member.setProfileImg(profileImg);
        } else {
            // 변경점이 없을 경우
            throw new NullPointerException("입력된 값이 없습니다.");
        }

        return member;
    }

    public List<MemberIdResponse> findAll() {
        return memberRepository
                .findAll()
                .stream()
                .map(m -> MemberIdResponse.from(m))
                .collect(Collectors.toList());
    }

    public Long findLongIdByMemberUuid(String memberUuid) {
        return memberRepository.findByUserUuid(UUID.fromString(memberUuid)).getId();
    }

}
