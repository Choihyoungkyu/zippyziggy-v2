package com.zippyziggy.monolithic.notice.service;

import com.zippyziggy.monolithic.common.util.SecurityUtil;
import com.zippyziggy.monolithic.notice.entity.Alarm;
import com.zippyziggy.monolithic.notice.entity.AlarmEntity;
import com.zippyziggy.monolithic.notice.repository.AlarmEntityRepository;
import com.zippyziggy.monolithic.notice.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmService {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 10; // 10분으로 설정
    private final AlarmRepository alarmRepository;
    private final AlarmEntityRepository alarmEntityRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public SseEmitter subscribe(String lastEventId) {
        String memberUuid = securityUtil.getCurrentMember().getUserUuid().toString();

        String emitterId = makeTimeIncludeId(memberUuid);

        SseEmitter emitter;

        log.info("사용자의 기존 emitter 제거 후 신규 emitter 추가");

        if (alarmRepository.findAllEmitterByMemberUuid(memberUuid) != null) {
            alarmRepository.deleteAllEmitterStartWithMemberUuid(memberUuid);
            emitter = alarmRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
        } else {
            emitter = alarmRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));
        }

        log.info("emitter 추가 완료");

        // 클라이언트가 SSE연결을 끊게 된다면 자원 낭비를 막기 위해 연결을 끊어준다.
        emitter.onCompletion(() -> alarmRepository.deleteByEmitterId(emitterId));
        // 시간 초과
        emitter.onTimeout(() -> alarmRepository.deleteByEmitterId(emitterId));
        // 에러 발생
        emitter.onError((e) -> alarmRepository.deleteByEmitterId(emitterId));
        log.info("정상 작동 중...");

        // 503 에러를 방지하기 위한 더미 이벤트 발송, 반드시 필요
        sendNotification(emitter, emitterId, "EventStream Created. [memberUuid = " + memberUuid + "]");

        // 유실 데이터가 존재하는 경우(??)
        if (hasLostData(lastEventId)) {
            // 더미 데이터 한번 더 전송??? 위에 이미 보냈는데.. 한번 더 보내는 건가...
            sendLostData(lastEventId, memberUuid, emitterId, emitter);
        }

        log.info("total emitters = " + alarmRepository.findAllEmitters());

        return emitter;
    }


    // 단순 알림 전송
    private void sendNotification(SseEmitter emitter, String emitterId, Object data) {

        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("connect")
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            alarmRepository.deleteByEmitterId(emitterId);
            emitter.completeWithError(exception);
        }

    }

    // milli-second로 emitterId 생성
    private String makeTimeIncludeId(String memberUuid) {
        return memberUuid + "_" + System.currentTimeMillis();
    }

    // Last-Event-Id의 존재 여부 확인
    private boolean hasLostData(String lastEventId) {
        log.info("lastEventId = " + lastEventId);
        return !lastEventId.isEmpty();
    }

    // 유실은 언제되는가?
    // lastEventId는 언제 보내는가?
    // 유실된 데이터 다시 전송
    private void sendLostData(String lastEventId, String memberUuid, String emitterId, SseEmitter emitter) {
        log.info("유실된 데이터 존재");
        log.info("유실 데이터 재전송 중...");
        Map<String, Object> eventCaches = alarmRepository.findAllEventCacheByMemberUuid(memberUuid);
        eventCaches.entrySet().stream()
                .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0) // 대상 문자열 보다 작은, 같은, 클 경우 -1, 0, 1을 반환
                .forEach(entry -> sendNotification(emitter, emitterId, entry.getValue()));

    }

    @Transactional
    public void send(String receiver, String content, String urlValue) {

        Alarm notification = createNotification(receiver, content, urlValue);

        // 로그인한 유저의 SseEmitter 모두 가져오기
        Map<String, SseEmitter> sseEmitters = alarmRepository.findAllEmitterByMemberUuid(receiver);

        AlarmEntity alarmEntity = AlarmEntity.builder()
                .content(content)
                .isRead(false)
                .url(urlValue)
                .memberUuid(receiver).build();
        alarmEntityRepository.save(alarmEntity);

        // 왜 for문을 도는걸까? sseEmitter는 로그인 시 유저당 하나씩 가지지 않을까 생각함.
        // 만약 로그아웃된 사용자라면 위 로직에서 알람 저장만 하고 끝난다.
        sseEmitters.forEach(
                (key, emitter) -> {
                    //데이터 캐시 저장(유실된 데이터 처리하기 위함
                    alarmRepository.saveEventCache(key, notification);
                    log.info("notification = " + notification);
                    log.info("eventCache = " + alarmRepository.findAllCacheEmitters());

                    // 데이터 전송
                    sendToClient(emitter, key, notification);

                }
        );

    }

    // 알림 생성
    private Alarm createNotification(String receiver, String content, String urlValue) {
        // 초기 알람은 읽지 않은 상태이므로 false를 가리킨다.
        return Alarm.builder()
                .content(content)
                .receiver(receiver)
                .url(urlValue)
                .isRead(false).build();
    }


    // 알림 전송
    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("sse")
                    .data(data, MediaType.APPLICATION_JSON)
                    .reconnectTime(0));

            log.info("알림 전송 데이터 = " + data.toString());
            log.info("알림 전송하고 emitters = " + alarmRepository.findAllEmitters());

        } catch (Exception exception) {
            // 에러 발생 시 emitter 지움
            alarmRepository.deleteByEmitterId(emitterId);
            // 이 친구는 뭐지?
            emitter.completeWithError(exception);
        }
    }


    // alarmId로 알림 삭제
    @Transactional
    public void deleteAlarmById(Long alarmId) {
        alarmEntityRepository.deleteById(alarmId);
    }

    // 해당 유저의 알림 모두 삭제
    @Transactional
    public void deleteAlarmByMemberUuid(String memberUuid) {
        alarmEntityRepository.deleteAllByMemberUuid(memberUuid);
    }

    // 해당 알람 읽기
    @Transactional
    public void readAlarmById(Long alarmId) {
        Optional<AlarmEntity> alarm = alarmEntityRepository.findById(alarmId);
        AlarmEntity alarmEntity = alarm.get();

        alarmEntity.setRead(true);
    }

    // 해당 유저의 알람 모두 가져오기
    public List<AlarmEntity> findMemberAlarmList(String memberUuid, Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return alarmEntityRepository.findAllByMemberUuidOrderByIdDesc(memberUuid, pageRequest);

    }

    // 해당 유저의 읽지 않은 알람 개수 가져오기
    public Long countUnReadAlarmByMemberUuid(String memberUuid) {
        return alarmEntityRepository.countByMemberUuidAndIsReadFalse(memberUuid);
    }

    // 모두 읽음 처리 진행하기
    @Transactional
    public void readAlarmAllByMemberUuid(String memberUuid) {
        List<AlarmEntity> alarms = alarmEntityRepository.findAllByMemberUuidAndIsReadFalse(memberUuid);
        for (AlarmEntity alarm : alarms) {
            alarm.setRead(true);
        }
    }


}
