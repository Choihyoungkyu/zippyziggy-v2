package com.zippyziggy.monolithic.talk.controller;

import com.zippyziggy.monolithic.talk.dto.request.TalkCommentRequest;
import com.zippyziggy.monolithic.talk.dto.request.TalkRequest;
import com.zippyziggy.monolithic.talk.dto.response.*;
import com.zippyziggy.monolithic.talk.service.TalkCommentService;
import com.zippyziggy.monolithic.talk.service.TalkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/talks")
@RequiredArgsConstructor
public class TalkController {

	private final TalkService talkService;
	private final TalkCommentService talkCommentService;

	@GetMapping("")
	public ResponseEntity<List<TalkListResponse>> getTalkList(@RequestHeader String crntMemberuuid) {
		return ResponseEntity.ok(talkService.getTalkList(crntMemberuuid));
	}

	@Operation(summary = "톡 생성", description = "새로운 톡을 생성한다.")
	@PostMapping("")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<TalkResponse> createTalk(@RequestBody TalkRequest data) {
		return ResponseEntity.ok(talkService.createTalk(data));
	}

	@Operation(summary = "톡 삭제", description = "톡을 삭제한다.")
	@DeleteMapping("/{talkId}")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "삭제 완료"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<Void> removeTalk(
			@PathVariable Long talkId
	) {
		talkService.removeTalk(talkId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "톡 상세 조회", description = "톡 상세페이지를 조회한다. 프롬프트 활용 프롬프트면 Pagable 관련 없음 ! 안써도 됨")
	@GetMapping("/{talkId}")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<TalkDetailResponse> getTalkDetail(
		@PathVariable Long talkId,
		@PageableDefault(sort = "regDt",  direction = Sort.Direction.DESC) Pageable pageable,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		talkService.updateHit(talkId, request, response);
		return ResponseEntity.ok(talkService.getTalkDetail(talkId, pageable));
	}

	@Operation(summary = "톡 댓글 조회", description = "톡 상세페이지에서 댓글을 조회한다.")
	@GetMapping("/{talkId}/comments")
	public ResponseEntity<TalkCommentListResponse> getTalkComments(
			@PathVariable Long talkId,
			@PageableDefault(size = 8, sort = "regDt")
			Pageable pageable
	) {
		final TalkCommentListResponse talkCommentList = talkCommentService.getTalkCommentList(talkId, pageable);
		return ResponseEntity.ok(talkCommentList);
	}

	@Operation(summary = "톡 댓글 생성", description = "톡에 새로운 댓글을 생성한다.")
	@PostMapping("/{talkId}/comments")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<TalkCommentResponse> createTalkComment(
			@PathVariable Long talkId,
			@RequestBody TalkCommentRequest data
			) {
		final TalkCommentResponse talkComment = talkCommentService.createTalkComment(talkId, data);
		return ResponseEntity.ok(talkComment);
	}

	@Operation(summary = "톡 댓글 수정", description = "톡 게시물에 본인이 작성한 댓글을 수정한다.")
	@PutMapping("/{talkId}/comments/{commentId}")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<TalkCommentResponse> modifyTalkComment(
			@PathVariable Long commentId,
			@RequestBody TalkCommentRequest data
	) {
		final TalkCommentResponse comment = talkCommentService.modifyTalkComment(commentId, data);
		return ResponseEntity.ok(comment);
	}

	@Operation(summary = "톡 댓글 삭제", description = "톡 게시물에 본인이 작성한 댓글을 삭제한다.")
	@DeleteMapping("/{talkId}/comments/{commentId}")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "삭제 완료"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<Void> removeTalkComment(
		@PathVariable Long commentId
	) {
		talkCommentService.removeTalkComment(commentId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "톡 좋아요", description = "현재 로그인된 유저가 톡 게시물을 좋아요/좋아요 취소 한다.")
	@PostMapping("/{talkId}/like")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "좋아요 완료"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "500", description = "서버 에러")
	})
	public ResponseEntity<Void> likeTalk(
			@PathVariable Long talkId
	) {
		talkService.likeTalk(talkId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "톡 댓글 개수", description = "톡 댓글 개수를 반환합니다.")
	@GetMapping("/{talkId}/commentCnt")
	public ResponseEntity<Long> talkCommentCnt(@PathVariable Long talkId) {
		return ResponseEntity.ok(talkService.findCommentCnt(talkId));
	}

}
