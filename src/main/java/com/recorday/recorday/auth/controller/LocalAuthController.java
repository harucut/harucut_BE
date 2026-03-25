package com.recorday.recorday.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.dto.response.AuthStatusResponse;
import com.recorday.recorday.auth.jwt.dto.TokenResponse;
import com.recorday.recorday.auth.local.dto.request.EmailAuthVerifyRequest;
import com.recorday.recorday.auth.local.dto.request.LocalChangePasswordRequest;
import com.recorday.recorday.auth.local.dto.request.LocalLoginRequest;
import com.recorday.recorday.auth.local.dto.request.LocalRegisterRequest;
import com.recorday.recorday.auth.local.dto.request.LocalResetPasswordRequest;
import com.recorday.recorday.auth.local.dto.request.LocalVerifyPasswordRequest;
import com.recorday.recorday.auth.local.dto.response.EmailAuthVerifyResponse;
import com.recorday.recorday.auth.local.dto.response.LoginResponse;
import com.recorday.recorday.auth.local.dto.response.LoginResult;
import com.recorday.recorday.auth.local.service.LocalLoginService;
import com.recorday.recorday.auth.local.service.LocalUserAuthService;
import com.recorday.recorday.auth.local.service.PasswordService;
import com.recorday.recorday.auth.service.UserExitService;
import com.recorday.recorday.auth.utils.CookieUtil;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth (Local)", description = "이메일/비밀번호 기반 자체 로그인 및 회원 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LocalAuthController {

	private final LocalLoginService localLoginService;
	private final LocalUserAuthService localUserAuthService;
	private final UserExitService userExitService;
	private final PasswordService passwordService;
	private final CookieUtil cookieUtil;

	@Operation(summary = "이메일 로그인", description = "등록된 이메일과 비밀번호로 로그인하여 Access/Refresh 토큰을 쿠키로 발급받습니다.")
	@PostMapping("/harucut/login")
	public ResponseEntity<Response<LoginResponse>> login(@RequestBody @Valid LocalLoginRequest request) {
		LoginResult result = localLoginService.login(request);

		LoginResponse responseBody = new LoginResponse(result.userStatus());

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, result.cookies().accessTokenCookie().toString())
			.header(HttpHeaders.SET_COOKIE, result.cookies().refreshTokenCookie().toString())
			.body(Response.ok(responseBody));
	}

	@Operation(summary = "인증 상태 조회", description = "현재 로그인된 사용자의 상태를 반환합니다.")
	@GetMapping("/auth/status")
	public ResponseEntity<Response<AuthStatusResponse>> status(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserPrincipal principal
	) {
		AuthStatusResponse responseBody = new AuthStatusResponse(principal.getStatus());
		return Response.ok(responseBody).toResponseEntity();
	}

	@Operation(summary = "이메일 회원가입", description = "새로운 사용자를 등록합니다.")
	@PostMapping("/harucut/register")
	public ResponseEntity<Response<Void>> register(@RequestBody @Valid LocalRegisterRequest request) {
		localUserAuthService.register(request);
		return Response.ok().toResponseEntity();
	}

	@Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자의 계정을 삭제하고 탈퇴 요청 처리합니다. 7일 뒤 자정에 진짜 삭제됩니다.")
	@DeleteMapping("/harucut/exit")
	public ResponseEntity<Response<Void>> exit(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserPrincipal principal
	) {
		userExitService.requestExit(principal.getId());
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookieUtil.createExpiredCookie("accessToken").toString())
			.header(HttpHeaders.SET_COOKIE, cookieUtil.createExpiredCookie("refreshToken").toString())
			.body(Response.ok());
	}

	@Operation(summary = "회원 탈퇴 취소", description = "현재 로그인된 사용자의 계정을 탈퇴 취소합니다.")
	@PreAuthorize("hasRole('DELETED_REQUESTED')")
	@PostMapping("/harucut/reactivate")
	public ResponseEntity<Response<Void>> reactivate(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserPrincipal principal
	) {
		userExitService.reActivate(principal.getId());

		return Response.ok().toResponseEntity();
	}

	@Operation(
		summary = "인증 코드 검증",
		description = "비밀번호 재설정 인증 코드 검증, 리셋 토큰은 10분 유효"
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "검증 성공"),
		@ApiResponse(responseCode = "400", description = "인증 실패 (코드가 일치하지 않거나 만료됨)"),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
	})
	@PostMapping("/harucut/reset/password/verification")
	public ResponseEntity<Response<EmailAuthVerifyResponse>> verifyAuthCode(@RequestBody @Valid EmailAuthVerifyRequest request) {

		EmailAuthVerifyResponse emailAuthVerifyResponse = passwordService.verifyAuthCode(request.email(),
			request.code());

		return Response.ok(emailAuthVerifyResponse).toResponseEntity();
	}

	@Operation(summary = "비밀번호 재설정 (찾기)", description = "새로운 비밀번호로 변경합니다. 리셋 토큰은 10분 유효")
	@PatchMapping("/harucut/reset/password")
	public ResponseEntity<Response<Void>> resetPassword(
		@RequestBody LocalResetPasswordRequest request
	) {
		passwordService.resetPassword(request.resetToken(), request.newPassword());

		return Response.ok().toResponseEntity();
	}

	@Deprecated
	@Operation(summary = "기존 비밀번호 검증", description = "기존 비밀번호를 검증합니다.")
	@GetMapping("/harucut/verify/password")
	public ResponseEntity<Response<Void>> verifyPassword(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@RequestBody LocalVerifyPasswordRequest request
	) {
		passwordService.verifyOldPassword(principal.getId(), request.password());

		return Response.ok().toResponseEntity();
	}

	@Operation(summary = "비밀번호 변경", description = "기존 비밀번호를 검증한 후, 일치하면 새로운 비밀번호로 변경합니다.")
	@PatchMapping("/harucut/change/password")
	public ResponseEntity<Response<Void>> changePassword(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserPrincipal principal,
		@RequestBody LocalChangePasswordRequest request
	) {
		passwordService.changePassword(principal.getId(), principal.getPassword(), request.oldPassword(),
			request.newPassword());
		return Response.ok().toResponseEntity();
	}
}
