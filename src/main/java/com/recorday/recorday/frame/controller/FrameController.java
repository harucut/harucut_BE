package com.recorday.recorday.frame.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.frame.dto.request.FrameCreateRequest;
import com.recorday.recorday.frame.dto.response.FrameResponse;
import com.recorday.recorday.frame.service.FrameService;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Frame", description = "프레임 생성/조회/수정/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/user")
public class FrameController {

	private final FrameService frameService;

	@Operation(summary = "프레임 생성", description = "사용자 프레임을 생성합니다.")
	@PostMapping("/frame")
	public ResponseEntity<Response<Void>> createFrame(
		@RequestBody FrameCreateRequest request,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {

		frameService.createFrame(principal.getId(), request);

		return Response.ok().toResponseEntity();
	}

	@Operation(summary = "내 프레임 목록 조회", description = "현재 로그인한 사용자의 프레임 목록을 조회합니다.")
	@GetMapping("/frame")
	public ResponseEntity<Response<List<FrameResponse>>> getMyFrames(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		List<FrameResponse> response = frameService.getMyFrame(principal.getId());

		return Response.ok(response).toResponseEntity();
	}

	@Operation(summary = "프레임 단건 조회", description = "프레임 ID로 단건 조회합니다.")
	@GetMapping("/frame/{frameId}")
	public ResponseEntity<Response<FrameResponse>> getFrame(
		@Parameter(description = "프레임 ID", required = true) @PathVariable Long frameId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		FrameResponse response = frameService.getFrame(frameId, principal.getId());

		return Response.ok(response).toResponseEntity();
	}

	@Operation(summary = "프레임 삭제", description = "프레임 ID로 프레임을 삭제합니다.")
	@DeleteMapping("/frame/{frameId}")
	public ResponseEntity<Response<Void>> deleteFrame(
		@Parameter(description = "프레임 ID", required = true) @PathVariable Long frameId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		frameService.deleteFrame(principal.getId(), frameId);
		return Response.ok().toResponseEntity();
	}

	@Operation(summary = "프레임 수정", description = "프레임 ID로 기존 프레임을 수정합니다.")
	@PutMapping("/frame/{frameId}")
	public ResponseEntity<Response<Void>> updateFrame(
		@Parameter(description = "프레임 ID", required = true) @PathVariable Long frameId,
		@RequestBody FrameCreateRequest request,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		frameService.updateFrame(principal.getId(), frameId, request);
		return Response.ok().toResponseEntity();
	}
}
