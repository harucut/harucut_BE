package com.recorday.recorday.frame.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.exception.GlobalErrorCode;
import com.recorday.recorday.frame.component.FrameAssetManager;
import com.recorday.recorday.frame.component.FrameStyleConverter;
import com.recorday.recorday.frame.dto.request.FrameCreateRequest;
import com.recorday.recorday.frame.dto.response.FrameResponse;
import com.recorday.recorday.frame.entity.Frame;
import com.recorday.recorday.frame.entity.FrameComponent;
import com.recorday.recorday.frame.entity.attributes.ColorBackgroundAttributes;
import com.recorday.recorday.frame.enums.BackgroundType;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.frame.enums.FrameType;
import com.recorday.recorday.frame.repository.FrameRepository;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.util.user.UserReader;

@ExtendWith(MockitoExtension.class)
class FrameServiceImplTest {

	@Mock
	private FrameRepository frameRepository;

	@Mock
	private UserReader userReader;

	@Mock
	private FrameAssetManager frameAssetManager;

	@Mock
	private FrameStyleConverter frameStyleConverter;

	@InjectMocks
	private FrameServiceImpl frameService;

	@Test
	@DisplayName("프레임 생성 시 자원 이동 및 스타일 변환이 정상적으로 협력 객체에 위임된다.")
	void createFrame_Success() {
		//given
		Long userId = 1L;
		String tempKey = "temp/photo.jpg";
		String permanentKey = "uploads/users/uuid/assets/original/uuid.jpg";
		String expectedStyleJson = "{\"borderRadius\": 10}";

		User mockUser = createUser("test@test.com");

		var componentReq = new FrameCreateRequest.ComponentRequest(
			"uuid-1", ComponentType.PHOTO, tempKey,
			10.0, 20.0, 100.0, 100.0, 1.0, 0.0, 1, Map.of("borderRadius", 10)
		);

		var request = new FrameCreateRequest(
			"My Frame", "desc1", "frame_base_key", FrameType.CLASSIC,
			800, 1200,
			new ColorBackgroundAttributes("#FFFFFF"),
			List.of(componentReq)
		);

		given(userReader.getUserById(userId)).willReturn(mockUser);
		given(frameAssetManager.moveTempFilesToPermanent(mockUser, request.components()))
			.willReturn(Map.of(tempKey, permanentKey));
		given(frameStyleConverter.convertToJson(any())).willReturn(expectedStyleJson);

		//when
		frameService.createFrame(userId, request);

		//then
		then(userReader).should(times(1)).getUserById(userId);
		then(frameAssetManager).should(times(1)).moveTempFilesToPermanent(mockUser, request.components());
		then(frameStyleConverter).should(times(1)).convertToJson(componentReq.styleJson());

		ArgumentCaptor<Frame> frameCaptor = ArgumentCaptor.forClass(Frame.class);
		then(frameRepository).should(times(1)).save(frameCaptor.capture());

		Frame savedFrame = frameCaptor.getValue();

		assertThat(savedFrame.getComponents()).hasSize(1);
		assertThat(savedFrame.getComponents().get(0).getSource()).isEqualTo(permanentKey);
		assertThat(savedFrame.getComponents().get(0).getStyleJson()).isEqualTo(expectedStyleJson);
		assertThat(savedFrame.getUser()).isEqualTo(mockUser);
	}

	@Test
	@DisplayName("사용자 프레임 목록 조회 성공")
	void getMyFrame_사용자_프레임_목록_조회_성공() {
		// given
		Long userId = 1L;
		User mockUser = createUser("test@test.com");
		Frame frame1 = createFrameWithComponents(1L, "Frame 1", mockUser);
		Frame frame2 = createFrameWithComponents(2L, "Frame 2", mockUser);

		given(userReader.getUserById(userId)).willReturn(mockUser);
		given(frameRepository.findAllByUser(mockUser)).willReturn(List.of(frame1, frame2));
		given(frameAssetManager.resolveSource(any(ComponentType.class), anyString())).willReturn("presigned-url");
		given(frameAssetManager.resolveSource(any(BackgroundType.class), anyString())).willReturn("presigned-bg-url");
		given(frameStyleConverter.convertToMap(anyString())).willReturn(Map.of("borderRadius", 10));

		// when
		List<FrameResponse> result = frameService.getMyFrame(userId);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).title()).isEqualTo("Frame 1");
		assertThat(result.get(1).title()).isEqualTo("Frame 2");

		then(userReader).should(times(1)).getUserById(userId);
		then(frameRepository).should(times(1)).findAllByUser(mockUser);
	}

	@Test
	@DisplayName("프레임 단건 조회 성공")
	void getFrame_프레임_단건_조회_성공() {
		// given
		Long frameId = 1L;
		Long userId = 1L;
		User mockUser = createUser("test@test.com");
		Frame frame = createFrameWithComponents(frameId, "My Frame", mockUser);

		given(frameRepository.findById(frameId)).willReturn(Optional.of(frame));
		given(frameAssetManager.resolveSource(any(ComponentType.class), anyString())).willReturn("presigned-url");
		given(frameAssetManager.resolveSource(any(BackgroundType.class), anyString())).willReturn("presigned-bg-url");
		given(frameStyleConverter.convertToMap(anyString())).willReturn(Map.of("borderRadius", 10));

		// when
		FrameResponse result = frameService.getFrame(frameId, userId);

		// then
		assertThat(result).isNotNull();
		assertThat(result.frameId()).isEqualTo(frameId);
		assertThat(result.title()).isEqualTo("My Frame");

		then(frameRepository).should(times(1)).findById(frameId);
	}

	@Test
	@DisplayName("다른 사용자 프레임 접근 시 FORBIDDEN 예외 발생")
	void getFrame_다른_사용자_프레임_접근시_FORBIDDEN() {
		// given
		Long frameId = 1L;
		Long requestUserId = 2L;
		User frameOwner = createUser("owner@test.com");
		Frame frame = createFrameWithComponents(frameId, "Owner's Frame", frameOwner);

		given(frameRepository.findById(frameId)).willReturn(Optional.of(frame));

		// when & then
		assertThatThrownBy(() -> frameService.getFrame(frameId, requestUserId))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(GlobalErrorCode.FORBIDDEN);
			});

		then(frameRepository).should(times(1)).findById(frameId);
	}

	@Test
	@DisplayName("존재하지 않는 프레임 조회 시 NOT_FOUND 예외 발생")
	void getFrame_존재하지_않는_프레임_NOT_FOUND() {
		// given
		Long frameId = 999L;
		Long userId = 1L;

		given(frameRepository.findById(frameId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> frameService.getFrame(frameId, userId))
			.isInstanceOf(BusinessException.class)
			.satisfies(exception -> {
				BusinessException businessException = (BusinessException) exception;
				assertThat(businessException.getErrorCode()).isEqualTo(GlobalErrorCode.NOT_FOUND);
			});

		then(frameRepository).should(times(1)).findById(frameId);
	}

	@Test
	@DisplayName("프레임 삭제 성공")
	void deleteFrame_프레임_삭제_성공() {
		// given
		Long frameId = 1L;
		Long userId = 1L;
		User mockUser = createUser("test@test.com");
		Frame frame = createFrameWithComponents(frameId, "My Frame", mockUser);

		given(frameRepository.findById(frameId)).willReturn(Optional.of(frame));
		willDoNothing().given(frameAssetManager).deleteFiles(anyList());
		willDoNothing().given(frameRepository).delete(frame);

		// when
		frameService.deleteFrame(userId, frameId);

		// then
		then(frameRepository).should(times(1)).findById(frameId);
		then(frameAssetManager).should(times(1)).deleteFiles(anyList());
		then(frameRepository).should(times(1)).delete(frame);
	}

	@Test
	@DisplayName("프레임 수정 성공")
	void updateFrame_프레임_수정_성공() {
		// given
		Long frameId = 1L;
		Long userId = 1L;
		User mockUser = createUser("test@test.com");
		Frame frame = createFrameWithComponents(frameId, "Old Title", mockUser);

		String newTempKey = "temp/new-photo.jpg";
		String newPermanentKey = "uploads/users/uuid/assets/original/new-uuid.jpg";
		String expectedStyleJson = "{\"borderRadius\": 20}";

		var componentReq = new FrameCreateRequest.ComponentRequest(
			"uuid-2", ComponentType.PHOTO, newTempKey,
			20.0, 30.0, 200.0, 200.0, 1.5, 45.0, 2, Map.of("borderRadius", 20)
		);

		var request = new FrameCreateRequest(
			"New Title", "New Description", "new_preview_key", FrameType.CLASSIC,
			1600, 2400,
			new ColorBackgroundAttributes("#000000"),
			List.of(componentReq)
		);

		given(frameRepository.findById(frameId)).willReturn(Optional.of(frame));
		given(frameAssetManager.moveTempFileToPermanent(mockUser, request.previewKey())).willReturn("new_preview_permanent_key");
		given(frameAssetManager.moveTempFilesToPermanent(mockUser, request.components()))
			.willReturn(Map.of(newTempKey, newPermanentKey));
		given(frameStyleConverter.convertToJson(any())).willReturn(expectedStyleJson);
		willDoNothing().given(frameAssetManager).deleteFiles(anyList());

		// when
		frameService.updateFrame(userId, frameId, request);

		// then
		assertThat(frame.getTitle()).isEqualTo("New Title");
		assertThat(frame.getDescription()).isEqualTo("New Description");

		then(frameRepository).should(times(1)).findById(frameId);
		then(frameAssetManager).should(times(1)).moveTempFileToPermanent(mockUser, request.previewKey());
		then(frameAssetManager).should(times(1)).moveTempFilesToPermanent(mockUser, request.components());
	}

	private User createUser(String email) {
		return User.builder()
			.id(1L)
			.publicId("user-public-id-123")
			.email(email)
			.username("tester")
			.profileUrl("http://profile.url")
			.provider(Provider.RECORDAY)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}

	private Frame createFrameWithComponents(Long frameId, String title, User user) {
		Frame frame = Frame.builder()
			.id(frameId)
			.title(title)
			.description("Test Description")
			.previewKey("preview/frame.jpg")
			.frameType(FrameType.CLASSIC)
			.background(new ColorBackgroundAttributes("#FFFFFF"))
			.user(user)
			.components(new ArrayList<>())
			.build();

		FrameComponent component = FrameComponent.builder()
			.id(1L)
			.type(ComponentType.PHOTO)
			.source("uploads/photo.jpg")
			.x(10.0)
			.y(20.0)
			.width(100.0)
			.height(100.0)
			.scale(1.0)
			.rotation(0.0)
			.zIndex(1)
			.styleJson("{\"borderRadius\": 10}")
			.frame(frame)
			.build();

		frame.getComponents().add(component);

		return frame;
	}

}