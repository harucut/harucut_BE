package com.recorday.recorday.frame.component;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.exception.GlobalErrorCode;
import com.recorday.recorday.frame.dto.request.FrameCreateRequest;
import com.recorday.recorday.frame.enums.BackgroundType;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrameAssetManager {

	private final FileStorageService fileStorageService;
	private static final String TEMP_ROOT = "temp";
	private static final String UPLOAD_ROOT = "uploads";

	public Map<String, String> moveTempFilesToPermanent(User user,
		List<FrameCreateRequest.ComponentRequest> components) {

		if (components == null || components.isEmpty()) {
			return Map.of();
		}

		String userTempPathCheck = String.format("%s/users/%s/", TEMP_ROOT, user.getPublicId());
		Map<String, String> normalizedByOriginal = new HashMap<>();
		Set<String> tempKeys = new HashSet<>();

		for (FrameCreateRequest.ComponentRequest component : components) {
			String source = component.source();
			String normalized = normalizeManagedKey(source);
			if (!StringUtils.hasText(normalized)) {
				continue;
			}

			normalizedByOriginal.put(source, normalized);

			if (normalized.startsWith(userTempPathCheck)) {
				tempKeys.add(normalized);
			}
		}

		Map<String, String> keyMapping = new HashMap<>();

		for (String tempKey : tempKeys) {
			try {
				String targetKey = toPermanentKey(tempKey);

				fileStorageService.moveFile(tempKey, targetKey);
				keyMapping.put(tempKey, targetKey);
			} catch (Exception e) {
				log.error("Failed to move file from temp: {}", tempKey, e);
				throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "이미지 저장 중 오류가 발생했습니다.");
			}
		}

		for (Map.Entry<String, String> entry : normalizedByOriginal.entrySet()) {
			String original = entry.getKey();
			String normalized = entry.getValue();
			String finalKey = keyMapping.getOrDefault(normalized, normalized);

			if (!original.equals(finalKey)) {
				keyMapping.put(original, finalKey);
			}
		}

		return keyMapping;
	}

	public String moveTempFileToPermanent(User user, String tempKey) {
		String normalized = normalizeManagedKey(tempKey);
		if (!StringUtils.hasText(normalized)) {
			return tempKey;
		}

		String userTempPathCheck = String.format("%s/users/%s/", TEMP_ROOT, user.getPublicId());
		if (!normalized.startsWith(userTempPathCheck)) {
			return normalized;
		}

		try {
			String targetKey = toPermanentKey(normalized);

			fileStorageService.moveFile(normalized, targetKey);
			return targetKey;
		} catch (Exception e) {
			log.error("배경 파일 이동 실패: {}", normalized, e);
			throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "배경 파일 저장 중 오류 발생");
		}
	}

	public String resolveSource(ComponentType type, String source) {
		if (source == null)
			return null;

		return switch (type) {
			case PHOTO -> {
				String normalized = normalizeManagedKey(source);
				if (isManagedS3Path(normalized)) {
					yield fileStorageService.generatePresignedGetUrl(normalized);
				}
				yield source;
			}
			default -> source;
		};
	}

	public String resolveSource(BackgroundType type, String source) {
		if (source == null)
			return null;

		return switch (type) {
			case IMAGE, VIDEO -> {
				String normalized = normalizeManagedKey(source);
				if (isManagedS3Path(normalized)) {
					yield fileStorageService.generatePresignedGetUrl(normalized);
				}
				yield source;
			}
			default -> source;
		};
	}

	public void deleteFiles(List<String> keys) {
		for (String key : keys) {
			if (key != null && !key.isBlank()) {
				fileStorageService.delete(key);
			}
		}
	}

	private String normalizeManagedKey(String pathOrKey) {
		if (!StringUtils.hasText(pathOrKey)) {
			return pathOrKey;
		}

		String value = pathOrKey.trim();

		if (value.startsWith("s3://")) {
			URI uri = URI.create(value);
			String key = stripLeadingSlash(uri.getPath());
			return StringUtils.hasText(key) ? key : value;
		}

		if (value.startsWith("http://") || value.startsWith("https://")) {
			URI uri = URI.create(value);
			String key = stripLeadingSlash(uri.getPath());
			if (isManagedS3Path(key)) {
				return key;
			}
			return value;
		}

		return stripLeadingSlash(value);
	}

	private boolean isManagedS3Path(String key) {
		return StringUtils.hasText(key)
			&& (key.startsWith(TEMP_ROOT + "/") || key.startsWith(UPLOAD_ROOT + "/"));
	}

	private String stripLeadingSlash(String value) {
		if (!StringUtils.hasText(value)) {
			return value;
		}
		return value.startsWith("/") ? value.substring(1) : value;
	}

	private String toPermanentKey(String tempKey) {
		return tempKey.replaceFirst("^" + TEMP_ROOT + "/", UPLOAD_ROOT + "/");
	}
}
