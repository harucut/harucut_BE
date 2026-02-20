package com.recorday.recorday.auth.local.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.local.dto.request.LocalRegisterRequest;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LocalUserAuthServiceImplTest {

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserRepository userRepository;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private LocalUserAuthServiceImpl localUserAuthService;

	private LocalRegisterRequest localRegisterRequest;

	@BeforeEach
	void setUp() {
		localRegisterRequest = new LocalRegisterRequest(
			"test@example.com",
			"raw-password",
			"testUser"
		);
	}

	@Test
	@DisplayName("이미 등록된 이메일로 회원가입 시 EMAIL_DUPLICATED 예외가 발생한다")
	void register_withDuplicateEmail_throwsEmailDuplicatedException() {
		// given
		given(userRepository.existsByProviderAndEmail(Provider.RECORDAY, localRegisterRequest.email()))
			.willReturn(true);

		// when & then
		assertThatThrownBy(() -> localUserAuthService.register(localRegisterRequest))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_DUPLICATED);
			});

		then(userRepository).should(times(1))
			.existsByProviderAndEmail(Provider.RECORDAY, localRegisterRequest.email());
		then(userRepository).should(never()).save(any(User.class));
	}

	@Test
	@DisplayName("이메일 인증 완료 후 신규 사용자가 정상적으로 등록된다")
	void register_withVerifiedEmail_savesNewUser() {
		// given
		String key = "REGISTER:EMAIL:" + localRegisterRequest.email();

		given(userRepository.existsByProviderAndEmail(Provider.RECORDAY, localRegisterRequest.email()))
			.willReturn(false);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn("verified");
		given(stringRedisTemplate.delete(key)).willReturn(true);
		given(passwordEncoder.encode(localRegisterRequest.password()))
			.willReturn("encoded-password");
		given(userRepository.save(any(User.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		localUserAuthService.register(localRegisterRequest);

		// then
		then(userRepository).should(times(1))
			.existsByProviderAndEmail(Provider.RECORDAY, localRegisterRequest.email());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		then(userRepository).should(times(1)).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getProvider()).isEqualTo(Provider.RECORDAY);
		assertThat(savedUser.getUserRole()).isEqualTo(UserRole.ROLE_USER);
		assertThat(savedUser.getEmail()).isEqualTo(localRegisterRequest.email());
		assertThat(savedUser.getUsername()).isEqualTo(localRegisterRequest.username());
		assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
		assertThat(savedUser.getProfileUrl()).isEqualTo("resources/defaults/userDefaultImage.png");
		assertThat(savedUser.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("이메일 인증이 완료되지 않은 상태에서 회원가입 시 REGISTER_EMAIL_ERROR 예외가 발생한다")
	void register_withoutEmailVerification_throwsRegisterEmailError() {
		// given
		String key = "REGISTER:EMAIL:" + localRegisterRequest.email();

		given(userRepository.existsByProviderAndEmail(Provider.RECORDAY, localRegisterRequest.email()))
			.willReturn(false);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(key)).willReturn(null);

		// when & then
		assertThatThrownBy(() -> localUserAuthService.register(localRegisterRequest))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException ex = (BusinessException) exception;
				assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REGISTER_EMAIL_ERROR);
			});

		then(userRepository).should(never()).save(any(User.class));
	}
}
