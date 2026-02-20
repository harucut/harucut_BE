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
import org.springframework.web.context.request.async.DeferredResult;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.media.dto.TranscodeRequest;
import com.recorday.recorday.media.service.TranscodingService;
import com.recorday.recorday.storage.dto.request.PresignedUploadRequest;
import com.recorday.recorday.storage.dto.response.PresignedUploadResponse;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
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
		@AuthenticationPrincipal CustomUserPrincipal principal
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
		description = "S3에 WebM 업로드가 완료된 후, 이 API를 호출하면 MediaConvert 작업을 시작합니다."
	)
	@PostMapping("/transcode")
	public DeferredResult<ResponseEntity<Response<Void>>> startTranscoding(
		@RequestBody @Valid TranscodeRequest request,
		@AuthenticationPrincipal CustomUserPrincipal principal
	) {
		// 1. 타임아웃 설정
		DeferredResult<ResponseEntity<Response<Void>>> deferredResult = new DeferredResult<>(120000L);

		// 2. AWS에 요청 보내고 Job ID 받기
		String jobId = transcodingService.createConversionJob(principal.getPublicId(), request.filename());

		// 3. Job ID와 대기 객체를 서비스에 등록 (Webhook이 올 때까지 대기 시작)
		transcodingService.registerDeferredResult(jobId, deferredResult);

		// 4. 즉시 리턴하지만, 실제 응답은 Webhook이 trigger 하거나 타임아웃 될 때 나감
		return deferredResult;
	}
}
