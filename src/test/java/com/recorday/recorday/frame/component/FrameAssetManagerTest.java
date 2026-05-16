package com.recorday.recorday.frame.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.frame.dto.request.FrameCreateRequest;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.user.entity.User;

@ExtendWith(MockitoExtension.class)
class FrameAssetManagerTest {

	@Mock
	private FileStorageService fileStorageService;

	@Mock
	private User user;

	@InjectMocks
	private FrameAssetManager frameAssetManager;

	@Test
	@DisplayName("컴포넌트 source가 URL이어도 temp 경로면 uploads로 이동하고 매핑한다")
	void moveTempFilesToPermanent_movesTempSourcesFromUrlAndNonPhoto() {
		given(user.getPublicId()).willReturn("user-1");

		var photoFromUrl = new FrameCreateRequest.ComponentRequest(
			"1",
			ComponentType.PHOTO,
			"https://recorday-bucket.s3.ap-northeast-2.amazonaws.com/temp/users/user-1/components/a.png",
			0, 0, 100, 100, 1, 0, 1, Map.of()
		);

		var stickerFromTemp = new FrameCreateRequest.ComponentRequest(
			"2",
			ComponentType.STICKER,
			"temp/users/user-1/components/b.png",
			0, 0, 100, 100, 1, 0, 2, Map.of()
		);

		var textComponent = new FrameCreateRequest.ComponentRequest(
			"3",
			ComponentType.TEXT,
			"hello world",
			0, 0, 100, 100, 1, 0, 3, Map.of()
		);

		Map<String, String> mapping = frameAssetManager.moveTempFilesToPermanent(
			user,
			List.of(photoFromUrl, stickerFromTemp, textComponent)
		);

		then(fileStorageService).should().moveFile(
			"temp/users/user-1/components/a.png",
			"uploads/users/user-1/components/a.png"
		);
		then(fileStorageService).should().moveFile(
			"temp/users/user-1/components/b.png",
			"uploads/users/user-1/components/b.png"
		);
		then(fileStorageService).should(never()).moveFile(anyString(), org.mockito.ArgumentMatchers.eq("hello world"));

		assertThat(mapping.get(photoFromUrl.source())).isEqualTo("uploads/users/user-1/components/a.png");
		assertThat(mapping.get(stickerFromTemp.source())).isEqualTo("uploads/users/user-1/components/b.png");
	}

	@Test
	@DisplayName("단일 temp key 이동 시 s3 URL 입력을 key로 정규화하고 uploads로 이동한다")
	void moveTempFileToPermanent_movesWhenS3UrlIsProvided() {
		given(user.getPublicId()).willReturn("user-1");

		String result = frameAssetManager.moveTempFileToPermanent(
			user,
			"s3://recorday-bucket/temp/users/user-1/backgrounds/bg.png"
		);

		then(fileStorageService).should().moveFile(
			"temp/users/user-1/backgrounds/bg.png",
			"uploads/users/user-1/backgrounds/bg.png"
		);
		assertThat(result).isEqualTo("uploads/users/user-1/backgrounds/bg.png");
	}

	@Test
	@DisplayName("이미 uploads 경로인 URL은 이동 없이 key만 정규화해 반환한다")
	void moveTempFileToPermanent_normalizesUploadsUrlWithoutMove() {
		given(user.getPublicId()).willReturn("user-1");

		String result = frameAssetManager.moveTempFileToPermanent(
			user,
			"https://recorday-bucket.s3.ap-northeast-2.amazonaws.com/uploads/users/user-1/components/c.png"
		);

		then(fileStorageService).should(never()).moveFile(anyString(), anyString());
		assertThat(result).isEqualTo("uploads/users/user-1/components/c.png");
	}
}
