package com.recorday.recorday.storage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.media.dto.TranscodeRequest;
import com.recorday.recorday.media.dto.response.TranscodeTaskStatusResponse;
import com.recorday.recorday.media.dto.response.TranscodeTaskSubmitResponse;
import com.recorday.recorday.media.service.TranscodingService;
import com.recorday.recorday.storage.dto.request.PresignedUploadRequest;
import com.recorday.recorday.storage.dto.response.PresignedUploadResponse;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "File Storage", description = "파일 업로드 및 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/user/files")
public class FileController {

	private final FileStorageService fileStorageService;
	private final TranscodingService transcodingService;

	@Operation(
		summary = "Presigned URL 생성",
		description = "S3에 파일을 업로드하기 위한 Presigned URL을 생성합니다. 업로드 타입(type)에 따라 저장 경로가 결정됩니다."
	)
	@PostMapping("/presigned-upload")
	public ResponseEntity<Response<PresignedUploadResponse>> createPresignedUpload(
		@RequestBody @Valid PresignedUploadRequest request,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {

		PresignedUploadResponse response = fileStorageService.generatePresignedUploadUrl(
			request.type(),
			request.filename(),
			request.contentType(),
			principal.getPublicId(),
			request.isTemp()
		);

		return Response.ok(response).toResponseEntity();
	}

	@Operation(summary = "이미지 조회용 Presigned URL 생성", description = "저장된 key를 기반으로 이미지 다운로드용 Presigned URL을 생성합니다.")
	@GetMapping("/presigned-img")
	public ResponseEntity<Response<String>> getPresignedImgUrl(@RequestParam("key") String key) {
		String response = fileStorageService.generatePresignedGetUrl(key);

		return Response.ok(response).toResponseEntity();
	}

	@Operation(
		summary = "S3 객체 삭제",
		description = "S3에 업로드 되어있는 파일을 key를 참고해 삭제합니다."
	)
	@DeleteMapping("/delete")
	public ResponseEntity<Response<Void>> deleteFile(@RequestParam("key") String key) {

		fileStorageService.delete(key);

		return Response.ok().toResponseEntity();
	}

	@Operation(
		summary = "동영상 변환 요청 (WebM -> MP4)",
		description = "S3에 WebM 업로드가 완료된 후, 이 API를 호출하면 MediaConvert 작업을 시작하고 즉시 taskId/jobId를 반환합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "202", description = "변환 작업 제출 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "500", description = "변환 작업 제출 실패")
	})
	@PostMapping("/transcode")
	public ResponseEntity<Response<TranscodeTaskSubmitResponse>> startTranscoding(
		@RequestBody @Valid TranscodeRequest request,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		TranscodeTaskSubmitResponse response = transcodingService.submitTranscodeTask(
			principal.getPublicId(),
			request.filename()
		);
		return ResponseEntity.accepted().body(Response.ok(response));
	}

	@Operation(
		summary = "동영상 변환 상태 조회",
		description = "taskId를 기준으로 MediaConvert 변환 상태를 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "상태 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "403", description = "다른 사용자의 작업 접근"),
		@ApiResponse(responseCode = "404", description = "변환 작업을 찾을 수 없음")
	})
	@GetMapping("/transcode/status")
	public ResponseEntity<Response<TranscodeTaskStatusResponse>> getTranscodeStatus(
		@RequestParam("taskId") String taskId,
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		TranscodeTaskStatusResponse response = transcodingService.getTaskStatus(taskId, principal.getPublicId());
		return Response.ok(response).toResponseEntity();
	}
}
