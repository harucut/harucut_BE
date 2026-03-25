package com.recorday.recorday.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.recorday.recorday.auth.dto.response.AuthStatusResponse;
import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.local.service.LocalLoginService;
import com.recorday.recorday.auth.local.service.LocalUserAuthService;
import com.recorday.recorday.auth.local.service.PasswordService;
import com.recorday.recorday.auth.service.UserExitService;
import com.recorday.recorday.auth.utils.CookieUtil;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.response.Response;

@ExtendWith(MockitoExtension.class)
class LocalAuthControllerTest {

	@Mock
	private LocalLoginService localLoginService;

	@Mock
	private LocalUserAuthService localUserAuthService;

	@Mock
	private UserExitService userExitService;

	@Mock
	private PasswordService passwordService;

	@Mock
	private CookieUtil cookieUtil;

	@InjectMocks
	private LocalAuthController localAuthController;

	@Test
	@DisplayName("인증 상태 조회 시 현재 사용자 상태를 반환한다")
	void status_returnsCurrentUserStatus() {
		// given
		CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
		given(principal.getStatus()).willReturn(UserStatus.DELETED_REQUESTED);

		// when
		ResponseEntity<Response<AuthStatusResponse>> response = localAuthController.status(principal);

		// then
		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getData()).isNotNull();
		assertThat(response.getBody().getData().userStatus()).isEqualTo(UserStatus.DELETED_REQUESTED);
	}
}
