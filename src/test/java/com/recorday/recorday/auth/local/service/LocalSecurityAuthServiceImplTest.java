package com.recorday.recorday.auth.local.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
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
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.auth.utils.CookieUtil;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class LocalSecurityAuthServiceImplTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private UserReader userReader;

	@Mock
	private CookieUtil cookieUtil;

	@InjectMocks
	private LocalSecurityLoginServiceImpl localSecurityAuthService;

	private LocalLoginRequest request;
	private User user;

	@BeforeEach
	void setUp() {
		String email = "test@example.com";
		String rawPassword = "1234";

		request = new LocalLoginRequest(email, rawPassword);
		user = createLocalUser(email);
	}

	@Test
	@DisplayName("로그인 성공 시 쿠키와 유저 상태를 포함한 결과를 반환한다")
	void loginSuccess() {
		// given
		String publicId = "user-public-id";
		String accessToken = "new-access-token";
		String refreshToken = "new-refresh-token";

		// 1. Authentication & Principal Mocking
		Authentication authentication = mock(Authentication.class);
		CustomUserPrincipal principal = mock(CustomUserPrincipal.class);

		given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.willReturn(authentication);
		given(authentication.getPrincipal()).willReturn(principal);
		given(principal.getPublicId()).willReturn(publicId);

		// 2. UserReader Mocking (UserStatus 반환 확인용)
		given(userReader.getUserByPublicId(publicId)).willReturn(user);

		// 3. Token Generation Mocking
		given(jwtTokenService.createAccessToken(publicId)).willReturn(accessToken);
		given(jwtTokenService.createRefreshToken(publicId)).willReturn(refreshToken);
		given(jwtTokenService.getAccessTokenValidityMillis()).willReturn(1000L);
		given(jwtTokenService.getRefreshTokenValidityMillis()).willReturn(2000L);

		// 4. CookieUtil Mocking
		ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken).build();
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken).build();

		given(cookieUtil.createTokenCookie(eq("accessToken"), eq(accessToken), anyLong()))
			.willReturn(accessCookie);
		given(cookieUtil.createTokenCookie(eq("refreshToken"), eq(refreshToken), anyLong()))
			.willReturn(refreshCookie);

		// when
		LoginResult result = localSecurityAuthService.login(request);

		// then
		// 1. Redis 저장 호출 검증
		then(refreshTokenService).should().saveRefreshToken(publicId, refreshToken);

		// 2. 반환 값(UserStatus) 검증
		assertThat(result.userStatus()).isEqualTo(UserStatus.ACTIVE);

		// 3. 반환 값(Cookies) 검증
		assertThat(result.cookies().accessTokenCookie().getValue()).isEqualTo(accessToken);
		assertThat(result.cookies().refreshTokenCookie().getValue()).isEqualTo(refreshToken);
	}

	@Test
	@DisplayName("비밀번호 불일치(BadCredentials) 시 BusinessException(INVALID_CREDENTIALS) 발생")
	void loginFail_badCredentials() {
		// given
		given(authenticationManager.authenticate(any(Authentication.class)))
			.willThrow(new BadCredentialsException("bad credentials"));

		// when & then
		assertThatThrownBy(() -> localSecurityAuthService.login(request))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode")
			.isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);

		then(jwtTokenService).shouldHaveNoInteractions();
		then(refreshTokenService).shouldHaveNoInteractions();
	}

	// Helper Method
	private User createLocalUser(String email) {
		return User.builder()
			.id(1L)
			.publicId("user-public-id")
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE) // 상태 명시
			.email(email)
			.password("encoded")
			.username("테스트 유저")
			.profileUrl("resources/defaults/userDefaultImage.png")
			.build();
	}
}