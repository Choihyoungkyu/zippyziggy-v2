package com.zippyziggy.monolithic.talk.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TalkResponse {
    private Long talkId;
    private String title;
    private List<MessageResponse> messages;
    private LocalDateTime regDt;
    private UUID memberUuid;
}
