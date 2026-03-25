package com.recorday.recorday.media.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.media.dto.request.UserMediaDisplayNameUpdateRequest;
import com.recorday.recorday.media.dto.request.UserMediaRegisterRequest;
import com.recorday.recorday.media.dto.response.UserMediaResponse;
import com.recorday.recorday.media.enums.UserMediaType;
import com.recorday.recorday.media.service.UserMediaService;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User Media", description = "사용자 미디어(사진/영상) 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/user/media")
public class UserMediaController {

	private final UserMediaService userMediaService;

	@Operation(summary = "내 미디어 등록", description = "S3 업로드 완료 후, key를 DB에 등록합니다.")
	@PostMapping
	public ResponseEntity<Response<UserMediaResponse>> registerMedia(
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@RequestBody @Valid UserMediaRegisterRequest request
	) {
		UserMediaResponse response = userMediaService.registerMedia(principal.getId(), request);
		return Response.ok(response).toResponseEntity();
	}

	@Operation(summary = "내 미디어 목록 조회", description = "사용자의 사진/영상 목록과 다운로드 URL을 반환합니다.")
	@GetMapping
	public ResponseEntity<Response<List<UserMediaResponse>>> getMyMedia(
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@RequestParam(value = "type", required = false) UserMediaType type
	) {
		List<UserMediaResponse> response = userMediaService.getMyMedia(principal.getId(), type);
		return Response.ok(response).toResponseEntity();
	}

	@Operation(summary = "미디어 다운로드 URL 조회", description = "다운로드 버튼 클릭 시 사용할 presigned URL을 반환합니다.")
	@GetMapping("/{mediaId}/download-url")
	public ResponseEntity<Response<String>> getDownloadUrl(
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@PathVariable("mediaId") Long mediaId
	) {
		String response = userMediaService.getDownloadUrl(principal.getId(), mediaId);
		return Response.ok(response).toResponseEntity();
	}

	@Operation(summary = "미디어 파일명 수정", description = "사용자에게 표시될 파일명을 수정합니다.")
	@PatchMapping("/{mediaId}/display-name")
	public ResponseEntity<Response<UserMediaResponse>> updateDisplayName(
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@PathVariable("mediaId") Long mediaId,
		@RequestBody @Valid UserMediaDisplayNameUpdateRequest request
	) {
		UserMediaResponse response = userMediaService.updateDisplayName(principal.getId(), mediaId, request);
		return Response.ok(response).toResponseEntity();
	}
}
