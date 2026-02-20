package com.recorday.recorday.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.jwt.dto.AuthCodePayload;
import com.recorday.recorday.auth.jwt.dto.TokenResponse;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.oauth2.service.AuthCodeService;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class AuthorizationCodeTokenServiceImplTest {

	@Mock
	private AuthCodeService authCodeService;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private UserReader userReader;

	@Mock
	private User user;

	@InjectMocks
	private AuthorizationCodeTokenServiceImpl authorizationCodeTokenService;

	@Test
	@DisplayName("인가 코드 검증 후 Redis에서 삭제하고 JWT 토큰을 발급한다")
	void issueTokens_withValidCode_deletesCodeAndReturnsTokens() {
		// given
		String code = "AUTH_CODE_123";
		String publicId = "publicId";
		AuthCodePayload payload = new AuthCodePayload(publicId, "kakao");

		given(authCodeService.getAuthPayload(code)).willReturn(payload);
		given(userReader.getUserByPublicId(publicId)).willReturn(user);
		given(user.getUserStatus()).willReturn(UserStatus.ACTIVE);
		given(jwtTokenService.createAccessToken(publicId)).willReturn("ACCESS_TOKEN");
		given(jwtTokenService.createRefreshToken(publicId)).willReturn("REFRESH_TOKEN");

		// when
		TokenResponse tokenResponse = authorizationCodeTokenService.issueTokens(code);

		// then
		assertThat(tokenResponse.accessToken()).isEqualTo("ACCESS_TOKEN");
		assertThat(tokenResponse.refreshToken()).isEqualTo("REFRESH_TOKEN");
		assertThat(tokenResponse.userStatus()).isEqualTo(UserStatus.ACTIVE);

		then(authCodeService).should().getAuthPayload(code);
		then(authCodeService).should().deleteAuthCode(code);
		then(jwtTokenService).should().createAccessToken(publicId);
		then(jwtTokenService).should().createRefreshToken(publicId);
		then(refreshTokenService).should().saveRefreshToken(publicId, "REFRESH_TOKEN");
	}
}
