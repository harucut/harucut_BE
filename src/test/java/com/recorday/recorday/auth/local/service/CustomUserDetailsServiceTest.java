package com.recorday.recorday.auth.local.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.exception.CustomAuthenticationException;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.repository.UserRepository;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserReader userReader;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	@Test
	@DisplayName("이메일로 사용자 조회 성공 시 CustomUserPrincipal 반환")
	void loadUserByUsername_이메일로_사용자_조회_성공() {
		// given
		String email = "test@example.com";
		User user = createActiveUser(email);

		given(userRepository.findByProviderAndEmail(Provider.RECORDAY, email))
			.willReturn(Optional.of(user));

		// when
		CustomUserPrincipal result = (CustomUserPrincipal) customUserDetailsService.loadUserByUsername(email);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getEmail()).isEqualTo(email);
		assertThat(result.getId()).isEqualTo(user.getId());

		then(userRepository).should(times(1)).findByProviderAndEmail(Provider.RECORDAY, email);
	}

	@Test
	@DisplayName("존재하지 않는 이메일로 조회 시 NOT_EXIST_USER 예외 발생")
	void loadUserByUsername_존재하지_않는_이메일_예외발생() {
		// given
		String email = "notfound@example.com";

		given(userRepository.findByProviderAndEmail(Provider.RECORDAY, email))
			.willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
			.isInstanceOf(CustomAuthenticationException.class)
			.satisfies(exception -> {
				CustomAuthenticationException authException = (CustomAuthenticationException) exception;
				assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.NOT_EXIST_USER);
			});

		then(userRepository).should(times(1)).findByProviderAndEmail(Provider.RECORDAY, email);
	}

	@Test
	@DisplayName("publicId로 사용자 조회 성공 시 CustomUserPrincipal 반환")
	void loadUserByPublicId_publicId로_사용자_조회_성공() {
		// given
		String publicId = "user-public-id-123";
		User user = createActiveUser("test@example.com");

		given(userReader.getUserByPublicId(publicId)).willReturn(user);

		// when
		CustomUserPrincipal result = customUserDetailsService.loadUserByPublicId(publicId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getPublicId()).isEqualTo(publicId);
		assertThat(result.getEmail()).isEqualTo(user.getEmail());

		then(userReader).should(times(1)).getUserByPublicId(publicId);
	}

	@Test
	@DisplayName("탈퇴 요청된 사용자 조회 시 DELETED_REQUEST_USER 예외 발생")
	void loadUserByPublicId_탈퇴요청된_사용자_예외발생() {
		// given
		String publicId = "deleted-user-public-id";
		User deletedRequestUser = createDeletedRequestedUser("deleted@example.com");

		given(userReader.getUserByPublicId(publicId)).willReturn(deletedRequestUser);

		// when & then
		assertThatThrownBy(() -> customUserDetailsService.loadUserByPublicId(publicId))
			.isInstanceOf(CustomAuthenticationException.class)
			.satisfies(exception -> {
				CustomAuthenticationException authException = (CustomAuthenticationException) exception;
				assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.DELETED_REQUEST_USER);
			});

		then(userReader).should(times(1)).getUserByPublicId(publicId);
	}

	private User createActiveUser(String email) {
		return User.builder()
			.id(1L)
			.publicId("user-public-id-123")
			.email(email)
			.username("testUser")
			.password("encoded-password")
			.profileUrl("http://profile.url")
			.provider(Provider.RECORDAY)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}

	private User createDeletedRequestedUser(String email) {
		return User.builder()
			.id(2L)
			.publicId("deleted-user-public-id")
			.email(email)
			.username("deletedUser")
			.password("encoded-password")
			.profileUrl("http://profile.url")
			.provider(Provider.RECORDAY)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.DELETED_REQUESTED)
			.build();
	}
}
