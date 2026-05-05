package com.recorday.recorday.media.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.exception.GlobalErrorCode;
import com.recorday.recorday.media.dto.request.UserMediaDisplayNameUpdateRequest;
import com.recorday.recorday.media.dto.request.UserMediaRegisterRequest;
import com.recorday.recorday.media.dto.response.UserMediaResponse;
import com.recorday.recorday.media.entity.UserMedia;
import com.recorday.recorday.media.enums.UserMediaType;
import com.recorday.recorday.media.repository.UserMediaRepository;
import com.recorday.recorday.storage.service.FileStorageService;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.service.SubscriptionPolicyService;
import com.recorday.recorday.util.response.PageResponse;
import com.recorday.recorday.util.user.UserReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserMediaServiceImpl implements UserMediaService {

	private static final String FALLBACK_PREFIX = "harucut_";
	private static final DateTimeFormatter DISPLAY_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final UserReader userReader;
	private final UserMediaRepository userMediaRepository;
	private final FileStorageService fileStorageService;
	private final SubscriptionPolicyService subscriptionPolicyService;

	@Override
	public UserMediaResponse registerMedia(Long userId, UserMediaRegisterRequest request) {
		User user = userReader.getUserById(userId);
		String s3Key = normalizeToS3Key(request.s3Key());

		UserMedia media = userMediaRepository.findByS3Key(s3Key)
			.map(existing -> {
				if (!existing.getUser().getId().equals(userId)) {
					throw new BusinessException(GlobalErrorCode.FORBIDDEN, "다른 사용자의 미디어 key입니다.");
				}
				return existing;
			})
			.orElseGet(() -> {
				String displayName = resolveDisplayName(
					null,
					request.mediaType(),
					s3Key,
					null,
					LocalDateTime.now()
				);

				UserMedia newMedia = request.mediaType() == UserMediaType.PHOTO
					? UserMedia.ofPhoto(user, s3Key, displayName)
					: UserMedia.ofVideo(user, s3Key, null, null, displayName, null);
				return userMediaRepository.save(newMedia);
			});

		return toResponse(media);
	}

	@Transactional(readOnly = true)
	@Override
	public PageResponse<UserMediaResponse> getMyMedia(Long userId, UserMediaType mediaType, int page, int size) {
		User user = userReader.getUserById(userId);
		Pageable pageable = createPageable(page, size);
		LocalDateTime cutoff = subscriptionPolicyService.resolveHistoryCutoff(user);
		Page<UserMedia> mediaPage = findMediaPage(user, mediaType, cutoff, pageable);

		return PageResponse.from(mediaPage.map(this::toResponse));
	}

	@Override
	public String getDownloadUrl(Long userId, Long mediaId) {
		User user = userReader.getUserById(userId);

		UserMedia media = userMediaRepository.findByIdAndUser(mediaId, user)
			.orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));
		subscriptionPolicyService.assertHistoryAccessible(user, media.getCreatedAt());

		if (media.getMediaType() == UserMediaType.VIDEO) {
			subscriptionPolicyService.assertAndConsumeVideoDownloadQuota(user);
		}

		String downloadName = resolveDisplayName(
			media.getDisplayName(),
			media.getMediaType(),
			media.getS3Key(),
			media.getOriginalFileName(),
			media.getCreatedAt()
		);

		return fileStorageService.generatePresignedDownloadUrl(media.getS3Key(), downloadName);
	}

	@Override
	public UserMediaResponse updateDisplayName(Long userId, Long mediaId, UserMediaDisplayNameUpdateRequest request) {
		User user = userReader.getUserById(userId);
		UserMedia media = userMediaRepository.findByIdAndUser(mediaId, user)
			.orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));
		subscriptionPolicyService.assertHistoryAccessible(user, media.getCreatedAt());

		String normalized = resolveDisplayName(
			request.displayName(),
			media.getMediaType(),
			media.getS3Key(),
			media.getOriginalFileName(),
			media.getCreatedAt()
		);

		media.changeDisplayName(normalized);
		return toResponse(media);
	}

	@Override
	public UserMediaResponse saveTranscodedVideo(
		String userPublicId,
		String originalFileName,
		String outputS3Path,
		String transcodeJobId
	) {
		if (!StringUtils.hasText(userPublicId)) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "userPublicId가 누락되었습니다.");
		}

		User user = userReader.getUserByPublicId(userPublicId);
		String outputS3Key = normalizeToS3Key(outputS3Path);

		UserMedia existingMedia = userMediaRepository.findByS3Key(outputS3Key).orElse(null);
		if (existingMedia != null) {
			log.info("Transcoded media already saved. return existing. key={}", outputS3Key);
			return toResponse(existingMedia);
		}

		String originalS3Key = StringUtils.hasText(originalFileName)
			? String.format("uploads/users/%s/webm/%s", userPublicId, originalFileName)
			: null;

		String preferredName = buildTranscodedDisplayNameFromOriginal(originalFileName);
		String displayName = resolveDisplayName(
			preferredName,
			UserMediaType.VIDEO,
			outputS3Key,
			originalFileName,
			LocalDateTime.now()
		);

		UserMedia userMedia = UserMedia.ofVideo(
			user,
			outputS3Key,
			originalS3Key,
			originalFileName,
			displayName,
			transcodeJobId
		);

		UserMedia savedMedia = userMediaRepository.save(userMedia);
		log.info("Transcoded media saved. userPublicId={}, outputS3Key={}", userPublicId, outputS3Key);
		return toResponse(savedMedia);
	}

	private UserMediaResponse toResponse(UserMedia media) {
		String displayName = resolveDisplayName(
			media.getDisplayName(),
			media.getMediaType(),
			media.getS3Key(),
			media.getOriginalFileName(),
			media.getCreatedAt()
		);
		String downloadUrl = media.getMediaType() == UserMediaType.VIDEO
			? null
			: fileStorageService.generatePresignedDownloadUrl(media.getS3Key(), displayName);

		return new UserMediaResponse(
			media.getId(),
			media.getMediaType(),
			media.getS3Key(),
			displayName,
			downloadUrl,
			media.getOriginalS3Key(),
			media.getOriginalFileName(),
			media.getTranscodeJobId(),
			media.getCreatedAt()
		);
	}

	private String normalizeToS3Key(String pathOrKey) {
		if (!StringUtils.hasText(pathOrKey)) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "S3 경로 또는 키가 비어있습니다.");
		}

		String value = pathOrKey.trim();
		if (value.startsWith("s3://") || value.startsWith("http://") || value.startsWith("https://")) {
			URI uri = URI.create(value);
			String key = uri.getPath();
			if (!StringUtils.hasText(key) || "/".equals(key)) {
				throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "S3 경로에서 key를 추출할 수 없습니다.");
			}
			return key.startsWith("/") ? key.substring(1) : key;
		}

		return value.startsWith("/") ? value.substring(1) : value;
	}

	private String resolveDisplayName(
		String preferredName,
		UserMediaType mediaType,
		String s3Key,
		String originalFileName,
		LocalDateTime baseTime
	) {
		String keyExt = extractExtensionWithDot(s3Key);
		String requestedBase = sanitizeBaseName(preferredName);

		if (!StringUtils.hasText(requestedBase) && mediaType == UserMediaType.VIDEO) {
			requestedBase = sanitizeBaseName(removeExtension(originalFileName));
		}

		String finalBase = StringUtils.hasText(requestedBase)
			? requestedBase
			: FALLBACK_PREFIX + DISPLAY_NAME_TIME_FORMAT.format(baseTime != null ? baseTime : LocalDateTime.now());

		String ext = StringUtils.hasText(keyExt) ? keyExt : "";
		return truncateFileName(finalBase + ext, 255);
	}

	private String buildTranscodedDisplayNameFromOriginal(String originalFileName) {
		String base = sanitizeBaseName(removeExtension(originalFileName));
		return StringUtils.hasText(base) ? base + ".mp4" : null;
	}

	private Pageable createPageable(int page, int size) {
		if (page < 0) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "page는 0 이상이어야 합니다.");
		}
		if (size < 1) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "size는 1 이상이어야 합니다.");
		}

		return PageRequest.of(page, size);
	}

	private Page<UserMedia> findMediaPage(
		User user,
		UserMediaType mediaType,
		LocalDateTime cutoff,
		Pageable pageable
	) {
		if (cutoff == null) {
			return mediaType == null
				? userMediaRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
				: userMediaRepository.findAllByUserAndMediaTypeOrderByCreatedAtDesc(user, mediaType, pageable);
		}

		return mediaType == null
			? userMediaRepository.findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(user, cutoff, pageable)
			: userMediaRepository.findAllByUserAndMediaTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
				user,
				mediaType,
				cutoff,
				pageable
			);
	}

	private String sanitizeBaseName(String name) {
		if (!StringUtils.hasText(name)) {
			return "";
		}

		String value = name.trim()
			.replace("\\", "/")
			.replace("\"", "")
			.replace("\r", "")
			.replace("\n", "");

		int slashIdx = value.lastIndexOf('/');
		if (slashIdx >= 0 && slashIdx < value.length() - 1) {
			value = value.substring(slashIdx + 1);
		}

		value = removeExtension(value)
			.replaceAll("[\\p{Cntrl}]", "")
			.replaceAll("\\s+", " ")
			.trim();

		return value;
	}

	private String extractExtensionWithDot(String filenameOrKey) {
		if (!StringUtils.hasText(filenameOrKey)) {
			return "";
		}

		int idx = filenameOrKey.lastIndexOf('.');
		if (idx < 0 || idx == filenameOrKey.length() - 1) {
			return "";
		}

		String ext = filenameOrKey.substring(idx).toLowerCase();
		return ext.matches("\\.[a-z0-9]{1,10}") ? ext : "";
	}

	private String removeExtension(String filename) {
		if (!StringUtils.hasText(filename)) {
			return "";
		}

		int idx = filename.lastIndexOf('.');
		if (idx <= 0) {
			return filename;
		}

		return filename.substring(0, idx);
	}

	private String truncateFileName(String fileName, int maxLength) {
		if (!StringUtils.hasText(fileName) || fileName.length() <= maxLength) {
			return fileName;
		}

		String ext = extractExtensionWithDot(fileName);
		int baseLimit = maxLength - ext.length();
		if (baseLimit <= 0) {
			return fileName.substring(0, maxLength);
		}

		String base = removeExtension(fileName);
		if (base.length() > baseLimit) {
			base = base.substring(0, baseLimit);
		}

		return base + ext;
	}
}
