package com.zippyziggy.monolithic.talk.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TalkCommentRequest {
	private String content;
}
