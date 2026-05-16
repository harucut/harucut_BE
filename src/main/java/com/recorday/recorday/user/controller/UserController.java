package com.recorday.recorday.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.user.dto.request.ChangeProfileImageRequest;
import com.recorday.recorday.user.dto.response.SubscriptionUsageResponse;
import com.recorday.recorday.user.dto.response.UserInfoResponse;
import com.recorday.recorday.user.service.UserService;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "사용자 정보 조회 및 수정 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/user")
public class UserController {

	private final UserService userService;

	@Operation(summary = "내 정보 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다.")
	@GetMapping("/info")
	public ResponseEntity<Response<UserInfoResponse>> getUserInfo(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		UserInfoResponse userInfo = userService.getUserInfo(principal.getId());

		return Response.ok(userInfo).toResponseEntity();
	}

	@Operation(
		summary = "구독 사용량 조회",
		description = "로그인한 사용자의 프레임 생성 누적 사용량과 영상 다운로드 월간 사용량을 조회합니다."
	)
	@GetMapping("/subscription/usage")
	public ResponseEntity<Response<SubscriptionUsageResponse>> getSubscriptionUsage(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal
	) {
		SubscriptionUsageResponse usage = userService.getSubscriptionUsage(principal.getId());
		return Response.ok(usage).toResponseEntity();
	}

	@Operation(summary = "사용자 이름 변경", description = "로그인한 사용자의 닉네임(Username)을 변경합니다.")
	@PatchMapping("/change/username")
	public ResponseEntity<Response<Void>> changeUsername(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal,
		@Parameter(description = "변경할 새로운 사용자 이름", required = true) @RequestParam String username
	) {
		userService.changeUsername(principal.getId(), username);

		return Response.ok().toResponseEntity();
	}

	@Operation(summary = "프로필 이미지 변경", description = "S3에 업로드된 이미지 키를 사용하여 프로필 이미지를 변경합니다.")
	@PatchMapping("/change/profile-image")
	public ResponseEntity<Response<Void>> changeProfileImage(
		@Parameter(hidden = true) @AuthenticationPrincipal CustomUserPrincipal principal,
		@RequestBody ChangeProfileImageRequest changeProfileImageRequest
	) {
		userService.changeProfileImage(principal.getId(), changeProfileImageRequest.s3Key());

		return Response.ok().toResponseEntity();
	}
}
