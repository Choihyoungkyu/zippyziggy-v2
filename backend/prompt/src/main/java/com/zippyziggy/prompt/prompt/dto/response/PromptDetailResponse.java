package com.zippyziggy.prompt.prompt.dto.response;

import java.time.ZoneId;
import org.springframework.lang.Nullable;
import com.zippyziggy.prompt.prompt.model.Prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter @Setter
@AllArgsConstructor
@Builder
public class PromptDetailResponse {

	private WriterResponse writerResponse;

	@Nullable
	private OriginerResponse originerResponse;

	private String title;
	private String description;
	private String thumbnail;
	private Long likeCnt;
	private Boolean isLiked;
	private Boolean isBookmarked;
	private Boolean isForked;
	private String category;
	private long regDt;
	private long updDt;

	private MessageResponse messageResponse;

}
