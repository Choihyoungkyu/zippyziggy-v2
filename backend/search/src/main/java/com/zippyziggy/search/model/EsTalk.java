package com.zippyziggy.search.model;

import com.zippyziggy.search.dto.response.server.SyncEsTalk;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.persistence.Id;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Document(indexName = "talk")
public class EsTalk {

	@Id
	private String id;

	@Field(type = FieldType.Long, name = "talk_id")
	private Long talkId;

	@Field(type = FieldType.Text, name = "prompt_uuid")
	private String promptUuid;

	@Field(type = FieldType.Text, name = "member_uuid")
	private String memberUuid;

	@Field(type = FieldType.Text, name = "title")
	private String title;

	@Field(type = FieldType.Long, name = "reg_dt")
	private Long regDt;

	@Field(type = FieldType.Long, name = "like_cnt")
	private Long likeCnt;

	@Field(type = FieldType.Long, name = "hit")
	private Long hit;

	@Field(type = FieldType.Text, name = "model")
	private String model;

	@Field(name = "es_messages")
	private List<EsMessage> esMessages;

	public void setHit(Long hit) { this.hit = hit; }

	public void setLikeCnt(Long likeCnt) { this.likeCnt = likeCnt; }

	public static EsTalk of (SyncEsTalk syncEsTalk) {
		return EsTalk.builder()
				.talkId(syncEsTalk.getTalkId())
				.promptUuid(syncEsTalk.getPromptUuid())
				.memberUuid(syncEsTalk.getMemberUuid())
				.title(syncEsTalk.getTitle())
				.regDt(syncEsTalk.getRegDt())
				.likeCnt(syncEsTalk.getLikeCnt())
				.hit(syncEsTalk.getHit())
				.esMessages(syncEsTalk.getEsMessages())
				.model(syncEsTalk.getModel())
				.build();
	}

}
