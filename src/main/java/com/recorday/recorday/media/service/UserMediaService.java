package com.recorday.recorday.media.service;

import java.util.List;

import com.recorday.recorday.media.dto.request.UserMediaDisplayNameUpdateRequest;
import com.recorday.recorday.media.dto.request.UserMediaRegisterRequest;
import com.recorday.recorday.media.dto.response.UserMediaResponse;
import com.recorday.recorday.media.enums.UserMediaType;

public interface UserMediaService {

	UserMediaResponse registerMedia(Long userId, UserMediaRegisterRequest request);

	List<UserMediaResponse> getMyMedia(Long userId, UserMediaType mediaType);

	String getDownloadUrl(Long userId, Long mediaId);

	UserMediaResponse updateDisplayName(Long userId, Long mediaId, UserMediaDisplayNameUpdateRequest request);

	UserMediaResponse saveTranscodedVideo(
		String userPublicId,
		String originalFileName,
		String outputS3Path,
		String transcodeJobId
	);
}
