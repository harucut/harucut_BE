package com.recorday.recorday.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.user.dto.response.UserInfoResponse;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserReader userReader;

	@Mock
	private FileStorageService fileStorageService;

	@InjectMocks
	private UserServiceImpl userService;

	@Test
	@DisplayName("사용자 정보 조회 성공 시 presignedUrl 포함 응답 반환")
	void getUserInfo_사용자_정보_조회_성공() {
		// given
		Long userId = 1L;
		String profileUrl = "profile/user-1/profile.jpg";
		String presignedUrl = "https://bucket.s3.amazonaws.com/profile/user-1/profile.jpg?signed=xxx";
		User user = createUser("test@example.com", profileUrl);

		given(userReader.getUserById(userId)).willReturn(user);
		given(fileStorageService.generatePresignedGetUrl(profileUrl)).willReturn(presignedUrl);

		// when
		UserInfoResponse response = userService.getUserInfo(userId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.email()).isEqualTo(user.getEmail());
		assertThat(response.username()).isEqualTo(user.getUsername());
		assertThat(response.profileUrl()).isEqualTo(presignedUrl);

		then(userReader).should(times(1)).getUserById(userId);
		then(fileStorageService).should(times(1)).generatePresignedGetUrl(profileUrl);
	}

	@Test
	@DisplayName("사용자명 변경 성공")
	void changeUsername_사용자명_변경_성공() {
		// given
		Long userId = 1L;
		String newUsername = "newUsername";
		User user = createUser("test@example.com", "profile/user-1/profile.jpg");

		given(userReader.getUserById(userId)).willReturn(user);

		// when
		userService.changeUsername(userId, newUsername);

		// then
		assertThat(user.getUsername()).isEqualTo(newUsername);

		then(userReader).should(times(1)).getUserById(userId);
	}

	@Test
	@DisplayName("프로필 이미지 변경 성공")
	void changeProfileImage_프로필_이미지_변경_성공() {
		// given
		Long userId = 1L;
		String newS3Key = "profile/user-1/new-profile.jpg";
		User user = createUser("test@example.com", "profile/user-1/old-profile.jpg");

		given(userReader.getUserById(userId)).willReturn(user);

		// when
		userService.changeProfileImage(userId, newS3Key);

		// then
		assertThat(user.getProfileUrl()).isEqualTo(newS3Key);

		then(userReader).should(times(1)).getUserById(userId);
	}

	private User createUser(String email, String profileUrl) {
		return User.builder()
			.id(1L)
			.publicId("user-public-id-123")
			.email(email)
			.username("testUser")
			.password("encoded-password")
			.profileUrl(profileUrl)
			.provider(Provider.RECORDAY)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}
}
