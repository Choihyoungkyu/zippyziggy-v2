package com.zippyziggy.monolithic.talk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageResponse {
	private String role;
	private String content;
}
