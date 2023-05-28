package com.zippyziggy.monolithic.talk.dto.request;

import com.zippyziggy.monolithic.talk.dto.response.MessageResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EsTalkRequest {

    private Long talkId;
    private String promptUuid;
    private String memberUuid;
    private String title;
    private Long regDt;
    private Long likeCnt;
    private Long hit;

    private List<MessageResponse> esMessages;

}
