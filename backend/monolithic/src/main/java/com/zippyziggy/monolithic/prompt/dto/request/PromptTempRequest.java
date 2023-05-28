package com.zippyziggy.monolithic.prompt.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptTempRequest {

	private String title;

	private String content;

	private String category;

	private PromptMessageRequest message;
}
