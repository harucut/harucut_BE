package com.recorday.recorday.auth.local.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.local.dto.request.LocalLoginRequest;
import com.recorday.recorday.auth.local.dto.response.LoginResult;
import com.recorday.recorday.auth.utils.CookieUtil;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class LocalSecurityLoginServiceImplTest {

	@Mock
	private UserReader userReader;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private CookieUtil cookieUtil;

	@Mock
	private Authentication authentication;

	@Mock
	private CustomUserPrincipal principal;

	@Mock
	private User user;

	@InjectMocks
	private LocalSecurityLoginServiceImpl localLoginService;

	private static final long ACCESS_EXPIRATION = 1800000L;
	private static final long REFRESH_EXPIRATION = 604800000L;

	@Test
	@DisplayName("로그인 성공 시 토큰 쿠키와 유저 상태를 포함한 LoginResult를 반환한다")
	void login_withValidCredentials_returnsLoginResultWithCookiesAndUserStatus() {
		// given
		LocalLoginRequest request = new LocalLoginRequest("test@email.com", "password");
		String publicId = "user-public-id";
		String accessToken = "access-token";
		String refreshToken = "refresh-token";

		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.willReturn(authentication);
		given(authentication.getPrincipal()).willReturn(principal);
		given(principal.getPublicId()).willReturn(publicId);
		given(userReader.getUserByPublicId(publicId)).willReturn(user);
		given(user.getUserStatus()).willReturn(UserStatus.ACTIVE);
		given(jwtTokenService.createAccessToken(publicId)).willReturn(accessToken);
		given(jwtTokenService.createRefreshToken(publicId)).willReturn(refreshToken);
		given(jwtTokenService.getAccessTokenValidityMillis()).willReturn(ACCESS_EXPIRATION);
		given(jwtTokenService.getRefreshTokenValidityMillis()).willReturn(REFRESH_EXPIRATION);

		ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
			.httpOnly(true).secure(true).path("/")
			.maxAge(Duration.ofMillis(ACCESS_EXPIRATION))
			.sameSite("Lax").domain("harucut.com").build();
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true).secure(true).path("/")
			.maxAge(Duration.ofMillis(REFRESH_EXPIRATION))
			.sameSite("Lax").domain("harucut.com").build();

		given(cookieUtil.createTokenCookie("accessToken", accessToken, ACCESS_EXPIRATION))
			.willReturn(accessCookie);
		given(cookieUtil.createTokenCookie("refreshToken", refreshToken, REFRESH_EXPIRATION))
			.willReturn(refreshCookie);

		// when
		LoginResult result = localLoginService.login(request);

		// then
		then(refreshTokenService).should().saveRefreshToken(publicId, refreshToken);
		assertThat(result.userStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(result.cookies().accessTokenCookie().getValue()).isEqualTo(accessToken);
		assertThat(result.cookies().refreshTokenCookie().getValue()).isEqualTo(refreshToken);
	}

	@Test
	@DisplayName("비밀번호 불일치 시 BusinessException(INVALID_CREDENTIALS)이 발생한다")
	void login_withWrongPassword_throwsBusinessException() {
		// given
		LocalLoginRequest request = new LocalLoginRequest("test@email.com", "wrong-password");

		given(authenticationManager.authenticate(any()))
			.willThrow(new BadCredentialsException("Bad credentials"));

		// when & then
		assertThatThrownBy(() -> localLoginService.login(request))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
			});

		then(jwtTokenService).shouldHaveNoInteractions();
	}
}
