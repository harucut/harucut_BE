package com.recorday.recorday.frame.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.exception.GlobalErrorCode;
import com.recorday.recorday.frame.component.FrameAssetManager;
import com.recorday.recorday.frame.component.FrameStyleConverter;
import com.recorday.recorday.frame.dto.request.FrameCreateRequest;
import com.recorday.recorday.frame.dto.response.FrameResponse;
import com.recorday.recorday.frame.entity.Frame;
import com.recorday.recorday.frame.entity.FrameComponent;
import com.recorday.recorday.frame.entity.attributes.BackgroundAttributes;
import com.recorday.recorday.frame.entity.attributes.ImageBackgroundAttributes;
import com.recorday.recorday.frame.entity.attributes.VideoBackgroundAttributes;
import com.recorday.recorday.frame.enums.BackgroundType;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.frame.repository.FrameRepository;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.util.user.UserReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FrameServiceImpl implements FrameService {

	private final FrameRepository frameRepository;
	private final UserReader userReader;
	private final FrameAssetManager frameAssetManager;
	private final FrameStyleConverter frameStyleConverter;

	@Override
	public void createFrame(Long userId, FrameCreateRequest request) {

		User user = userReader.getUserById(userId);

		Map<String, String> resolvedUrlMap = frameAssetManager.moveTempFilesToPermanent(user, request.components());
		BackgroundAttributes resolvedBackground = processNewBackground(user, request.background());

		String resolvedPreviewKey = frameAssetManager.moveTempFileToPermanent(user, request.previewKey());

		Frame frame = Frame.builder()
			.title(request.title())
			.description(request.description())
			.previewKey(resolvedPreviewKey)
			.frameType(request.frameType())
			.background(resolvedBackground)
			.build();

		frame.assignUser(user);

		List<FrameComponent> components = createComponents(request.components(), resolvedUrlMap);
		components.forEach(frame::addComponent);

		frameRepository.save(frame);
	}

	@Transactional(readOnly = true)
	@Override
	public List<FrameResponse> getMyFrame(Long userId) {
		User user = userReader.getUserById(userId);

		return frameRepository.findAllByUser(user).stream()
			.map(this::toFrameResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public FrameResponse getFrame(Long frameId, Long userId) {
		Frame frame = findFrameById(frameId);
		validateOwner(frame, userId);

		return toFrameResponse(frame);
	}

	@Override
	public void deleteFrame(Long userId, Long frameId) {

		Frame frame = findFrameById(frameId);
		validateOwner(frame, userId);

		List<String> keysToDelete = new ArrayList<>(extractPhotoKeys(frame.getComponents()));
		String bgKey = extractBackgroundKey(frame.getBackground());
		if (bgKey != null) {
			keysToDelete.add(bgKey);
		}

		if (frame.getPreviewKey() != null) {
			keysToDelete.add(frame.getPreviewKey());
		}

		frameAssetManager.deleteFiles(keysToDelete);

		frameRepository.delete(frame);
	}

	@Override
	public void updateFrame(Long userId, Long frameId, FrameCreateRequest request) {

		Frame frame = findFrameById(frameId);
		validateOwner(frame, userId);

		String oldBackgroundKey = extractBackgroundKey(frame.getBackground());
		String oldPreviewKey = frame.getPreviewKey();

		BackgroundAttributes newBackground = processNewBackground(frame.getUser(), request.background());
		String newBackgroundKey = extractBackgroundKey(newBackground);

		String newPreviewKey = frameAssetManager.moveTempFileToPermanent(frame.getUser(), request.previewKey());

		frame.updateMetadata(
			request.title(), request.description(),
			newBackground,
			newPreviewKey
		);

		if (oldBackgroundKey != null && !oldBackgroundKey.equals(newBackgroundKey)) {
			frameAssetManager.deleteFiles(List.of(oldBackgroundKey));
		}

		if (oldPreviewKey != null && !oldPreviewKey.equals(newPreviewKey)) {
			frameAssetManager.deleteFiles(List.of(oldPreviewKey));
		}

		List<String> oldS3Keys = extractPhotoKeys(frame.getComponents());

		frame.getComponents().clear();

		Map<String, String> resolvedUrlMap = frameAssetManager.moveTempFilesToPermanent(frame.getUser(),
			request.components());
		List<FrameComponent> newComponents = createComponents(request.components(), resolvedUrlMap);

		newComponents.forEach(frame::addComponent);

		Set<String> newS3Keys = Set.copyOf(extractPhotoKeys(newComponents));

		List<String> garbageKeys = oldS3Keys.stream()
			.filter(key -> !newS3Keys.contains(key))
			.toList();

		frameAssetManager.deleteFiles(garbageKeys);

	}

	// =========================================================================
	// Private Helper Methods
	// =========================================================================

	private Frame findFrameById(Long frameId) {
		return frameRepository.findById(frameId)
			.orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));
	}

	private void validateOwner(Frame frame, Long userId) {
		if (!frame.getUser().getId().equals(userId)) {
			throw new BusinessException(GlobalErrorCode.FORBIDDEN, "권한이 없습니다.");
		}
	}

	private List<FrameComponent> createComponents(
		List<FrameCreateRequest.ComponentRequest> componentRequests,
		Map<String, String> resolvedUrlMap
	) {
		if (componentRequests == null)
			return Collections.emptyList();

		return componentRequests.stream()
			.map(dto -> {
				String finalKey = resolvedUrlMap.getOrDefault(dto.source(), dto.source());
				String styleJson = frameStyleConverter.convertToJson(dto.styleJson());

				return FrameComponent.builder()
					.type(dto.type())
					.source(finalKey)
					.x(dto.x()).y(dto.y())
					.width(dto.width()).height(dto.height())
					.scale(dto.scale())
					.rotation(dto.rotation()).zIndex(dto.zIndex())
					.styleJson(styleJson)
					.build();
			}).collect(Collectors.toList());
	}

	private List<String> extractPhotoKeys(List<FrameComponent> components) {
		if (components == null)
			return Collections.emptyList();

		return components.stream()
			.filter(c -> c.getType() == ComponentType.PHOTO)
			.map(FrameComponent::getSource)
			.toList();
	}

	private FrameResponse toFrameResponse(Frame frame) {
		List<FrameResponse.ComponentResponse> componentResponses = frame.getComponents().stream()
			.map(c -> new FrameResponse.ComponentResponse(
				c.getId(),
				c.getType(),
				frameAssetManager.resolveSource(c.getType(), c.getSource()),
				c.getSource(),
				c.getX(), c.getY(), c.getWidth(), c.getHeight(), c.getRotation(), c.getZIndex(),
				frameStyleConverter.convertToMap(c.getStyleJson())
			))
			.toList();

		return new FrameResponse(
			frame.getId(),
			frame.getTitle(),
			frame.getDescription(),
			frameAssetManager.resolveSource(BackgroundType.IMAGE, frame.getPreviewKey()),
			frame.getFrameType(),
			resolveBackgroundUrl(frame.getBackground()),
			componentResponses
		);
	}

	private BackgroundAttributes resolveBackgroundUrl(BackgroundAttributes bg) {
		if (bg instanceof ImageBackgroundAttributes imgBg) {
			String signedUrl = frameAssetManager.resolveSource(BackgroundType.IMAGE, imgBg.getKey());
			return new ImageBackgroundAttributes(signedUrl, imgBg.getOpacity());
		} else if (bg instanceof VideoBackgroundAttributes vidBg) {
			String signedUrl = frameAssetManager.resolveSource(BackgroundType.VIDEO, vidBg.getKey());
			return new VideoBackgroundAttributes(signedUrl, vidBg.isAutoPlay(), vidBg.isLoop());
		}
		return bg;
	}

	private BackgroundAttributes processNewBackground(User user, BackgroundAttributes bg) {
		if (bg instanceof ImageBackgroundAttributes imgBg) {
			String movedKey = frameAssetManager.moveTempFileToPermanent(user, imgBg.getKey());
			if (!movedKey.equals(imgBg.getKey())) {
				return new ImageBackgroundAttributes(movedKey, imgBg.getOpacity());
			}
		} else if (bg instanceof VideoBackgroundAttributes vidBg) {
			String movedKey = frameAssetManager.moveTempFileToPermanent(user, vidBg.getKey());
			if (!movedKey.equals(vidBg.getKey())) {
				return new VideoBackgroundAttributes(movedKey, vidBg.isAutoPlay(), vidBg.isLoop());
			}
		}
		return bg;
	}

	private String extractBackgroundKey(BackgroundAttributes bg) {
		if (bg instanceof ImageBackgroundAttributes imgBg) {
			return imgBg.getKey();
		} else if (bg instanceof VideoBackgroundAttributes vidBg) {
			return vidBg.getKey();
		}
		return null;
	}

}
