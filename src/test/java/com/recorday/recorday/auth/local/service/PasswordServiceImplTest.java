package com.recorday.recorday.auth.local.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.local.dto.response.EmailAuthVerifyResponse;
import com.recorday.recorday.auth.local.service.mail.MailAuthCodeService;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

	@Mock
	private UserReader userReader;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private MailAuthCodeService mailAuthCodeService;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private PasswordServiceImpl passwordService;

	private static final String KEY_PREFIX = "reset_token:";

	@Test
	@DisplayName("유효한 리셋 토큰으로 비밀번호를 재설정한다")
	void resetPassword_withValidToken_changesPassword() {
		// given
		String resetToken = "valid-reset-token";
		String newPassword = "newPassword123";
		String email = "test@example.com";
		String encodedPassword = "encoded-new-password";
		String key = KEY_PREFIX + resetToken;
		User user = createUser(email);

		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn(email);
		given(userReader.getUserByEmailAndProvider(email, Provider.HARUCUT)).willReturn(user);
		given(passwordEncoder.encode(newPassword)).willReturn(encodedPassword);
		given(stringRedisTemplate.delete(key)).willReturn(true);

		// when
		passwordService.resetPassword(resetToken, newPassword);

		// then
		assertThat(user.getPassword()).isEqualTo(encodedPassword);
		then(stringRedisTemplate).should().delete(key);
	}

	@Test
	@DisplayName("만료된 리셋 토큰으로 비밀번호 재설정 시 INVALID_TOKEN 예외가 발생한다")
	void resetPassword_withExpiredToken_throwsInvalidTokenException() {
		// given
		String resetToken = "expired-token";
		String newPassword = "newPassword123";
		String key = KEY_PREFIX + resetToken;

		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn(null);

		// when & then
		assertThatThrownBy(() -> passwordService.resetPassword(resetToken, newPassword))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN);
			});

		then(userReader).should(never()).getUserByEmailAndProvider(anyString(), any());
	}

	@Test
	@DisplayName("인증코드 검증 성공 시 resetToken을 반환한다")
	void verifyAuthCode_withValidCode_returnsResetToken() {
		// given
		String email = "test@example.com";
		String inputCode = "123456";

		willDoNothing().given(mailAuthCodeService).verifyCode(email, inputCode);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

		// when
		EmailAuthVerifyResponse response = passwordService.verifyAuthCode(email, inputCode);

		// then
		assertThat(response).isNotNull();
		assertThat(response.resetToken()).isNotBlank();
		then(mailAuthCodeService).should().verifyCode(email, inputCode);
		then(valueOperations).should().set(anyString(), eq(email), anyLong(), any());
	}

	@Test
	@DisplayName("기존 비밀번호가 일치하면 검증에 성공한다")
	void verifyOldPassword_withMatchingPassword_succeeds() {
		// given
		Long userId = 1L;
		String oldPassword = "oldPassword123";
		String encodedOldPassword = "encoded-old-password";
		User user = createUserWithPassword("test@example.com", encodedOldPassword);

		given(userReader.getUserById(userId)).willReturn(user);
		given(passwordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(true);

		// when
		passwordService.verifyOldPassword(userId, oldPassword);

		// then
		then(userReader).should().getUserById(userId);
		then(passwordEncoder).should().matches(oldPassword, encodedOldPassword);
	}

	@Test
	@DisplayName("기존 비밀번호가 불일치하면 WRONG_PASSWORD 예외가 발생한다")
	void verifyOldPassword_withWrongPassword_throwsWrongPasswordException() {
		// given
		Long userId = 1L;
		String oldPassword = "wrongPassword";
		String encodedOldPassword = "encoded-old-password";
		User user = createUserWithPassword("test@example.com", encodedOldPassword);

		given(userReader.getUserById(userId)).willReturn(user);
		given(passwordEncoder.matches(oldPassword, encodedOldPassword)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> passwordService.verifyOldPassword(userId, oldPassword))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.WRONG_PASSWORD);
			});
	}

	@Test
	@DisplayName("기존 비밀번호 일치 시 새 비밀번호로 변경된다")
	void changePassword_withCorrectOldPassword_changesPassword() {
		// given
		Long userId = 1L;
		String encodedCurrentPassword = "encoded-current-password";
		String rawOldPassword = "currentPassword";
		String newPassword = "newPassword123";
		String encodedNewPassword = "encoded-new-password";
		User user = createUser("test@example.com");

		given(passwordEncoder.matches(rawOldPassword, encodedCurrentPassword)).willReturn(true);
		given(userReader.getUserById(userId)).willReturn(user);
		given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

		// when
		passwordService.changePassword(userId, encodedCurrentPassword, rawOldPassword, newPassword);

		// then
		assertThat(user.getPassword()).isEqualTo(encodedNewPassword);
		then(passwordEncoder).should().matches(rawOldPassword, encodedCurrentPassword);
		then(userReader).should().getUserById(userId);
	}

	@Test
	@DisplayName("비밀번호 변경 시 기존 비밀번호 불일치로 WRONG_PASSWORD 예외가 발생한다")
	void changePassword_withWrongOldPassword_throwsWrongPasswordException() {
		// given
		Long userId = 1L;
		String encodedCurrentPassword = "encoded-current-password";
		String rawWrongPassword = "wrongPassword";
		String newPassword = "newPassword123";

		given(passwordEncoder.matches(rawWrongPassword, encodedCurrentPassword)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> passwordService.changePassword(userId, encodedCurrentPassword, rawWrongPassword, newPassword))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.WRONG_PASSWORD);
			});

		then(userReader).should(never()).getUserById(anyLong());
	}

	private User createUser(String email) {
		return User.builder()
			.id(1L)
			.publicId("user-public-id-123")
			.email(email)
			.username("testUser")
			.password("encoded-password")
			.profileUrl("http://profile.url")
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}

	private User createUserWithPassword(String email, String password) {
		return User.builder()
			.id(1L)
			.publicId("user-public-id-123")
			.email(email)
			.username("testUser")
			.password(password)
			.profileUrl("http://profile.url")
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}
}
