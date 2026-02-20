package com.recorday.recorday.auth.jwt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseCookie;

import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.exception.CustomAuthenticationException;
import com.recorday.recorday.auth.jwt.dto.AuthTokenCookies;
import com.recorday.recorday.auth.utils.CookieUtil;
import com.recorday.recorday.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private CookieUtil cookieUtil;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RefreshTokenServiceImpl refreshTokenService;

	private static final long TEST_ACCESS_EXPIRATION = 1800000L;
	private static final long TEST_REFRESH_EXPIRATION = 604800000L;

	@Test
	@DisplayName("유효한 리프레시 토큰으로 재발급 시 새로운 토큰 쿠키가 반환된다")
	void reissue_withValidRefreshToken_returnsNewTokenCookies() {
		// given
		String oldRefreshToken = "old-refresh-token";
		String publicId = "user-public-id-123";
		String key = "REFRESH_TOKEN:USER:" + publicId;
		String newAccessToken = "new-access-token";
		String newRefreshToken = "new-refresh-token";

		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(jwtTokenService.getTokenType(oldRefreshToken)).willReturn("REFRESH");
		given(jwtTokenService.getUserPublicId(oldRefreshToken)).willReturn(publicId);
		given(jwtTokenService.getRefreshTokenValidityMillis()).willReturn(TEST_REFRESH_EXPIRATION);
		given(jwtTokenService.getAccessTokenValidityMillis()).willReturn(TEST_ACCESS_EXPIRATION);
		given(valueOperations.get(key)).willReturn(oldRefreshToken);
		given(jwtTokenService.createAccessToken(publicId)).willReturn(newAccessToken);
		given(jwtTokenService.createRefreshToken(publicId)).willReturn(newRefreshToken);

		ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
			.httpOnly(true).secure(true).path("/")
			.maxAge(Duration.ofMillis(TEST_ACCESS_EXPIRATION))
			.sameSite("Lax").domain("harucut.com").build();
		ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
			.httpOnly(true).secure(true).path("/")
			.maxAge(Duration.ofMillis(TEST_REFRESH_EXPIRATION))
			.sameSite("Lax").domain("harucut.com").build();

		given(cookieUtil.createTokenCookie("accessToken", newAccessToken, TEST_ACCESS_EXPIRATION))
			.willReturn(accessCookie);
		given(cookieUtil.createTokenCookie("refreshToken", newRefreshToken, TEST_REFRESH_EXPIRATION))
			.willReturn(refreshCookie);

		// when
		AuthTokenCookies response = refreshTokenService.reissue(oldRefreshToken);

		// then
		assertThat(response.accessTokenCookie().getValue()).isEqualTo(newAccessToken);
		assertThat(response.refreshTokenCookie().getValue()).isEqualTo(newRefreshToken);

		then(valueOperations).should().set(
			eq(key),
			eq(newRefreshToken),
			eq(Duration.ofMillis(TEST_REFRESH_EXPIRATION))
		);
		then(cookieUtil).should().createTokenCookie("accessToken", newAccessToken, TEST_ACCESS_EXPIRATION);
		then(cookieUtil).should().createTokenCookie("refreshToken", newRefreshToken, TEST_REFRESH_EXPIRATION);
	}

	@Test
	@DisplayName("토큰 검증 실패 시 CustomAuthenticationException이 전파된다")
	void reissue_withInvalidToken_throwsCustomAuthenticationException() {
		// given
		String invalidToken = "invalid-token";

		willThrow(new CustomAuthenticationException(AuthErrorCode.INVALID_TOKEN))
			.given(jwtTokenService).validateToken(invalidToken);

		// when & then
		assertThatThrownBy(() -> refreshTokenService.reissue(invalidToken))
			.isInstanceOf(CustomAuthenticationException.class);
	}

	@Test
	@DisplayName("토큰 타입이 REFRESH가 아니면 BusinessException(INVALID_TOKEN)이 발생한다")
	void reissue_withNonRefreshTokenType_throwsBusinessException() {
		// given
		String accessToken = "access-token";
		given(jwtTokenService.getTokenType(accessToken)).willReturn("ACCESS");

		// when & then
		assertThatThrownBy(() -> refreshTokenService.reissue(accessToken))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN);
			});
	}

	@Test
	@DisplayName("Redis에 저장된 토큰이 없으면 BusinessException(INVALID_TOKEN)이 발생한다")
	void reissue_withNoTokenInRedis_throwsBusinessException() {
		// given
		String requestToken = "token-from-user";
		String publicId = "user-public-id";
		String key = "REFRESH_TOKEN:USER:" + publicId;

		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(jwtTokenService.getTokenType(requestToken)).willReturn("REFRESH");
		given(jwtTokenService.getUserPublicId(requestToken)).willReturn(publicId);
		given(valueOperations.get(key)).willReturn(null);

		// when & then
		assertThatThrownBy(() -> refreshTokenService.reissue(requestToken))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN);
			});
	}

	@Test
	@DisplayName("로그아웃 시 Redis에서 해당 사용자의 리프레시 토큰 키를 삭제한다")
	void logout_deletesRedisKey() {
		// given
		String publicId = "user-public-id";
		String key = "REFRESH_TOKEN:USER:" + publicId;

		// when
		refreshTokenService.logout(publicId);

		// then
		then(stringRedisTemplate).should().delete(key);
	}
}
