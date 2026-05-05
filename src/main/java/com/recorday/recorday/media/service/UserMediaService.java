package com.recorday.recorday.media.service;

import com.recorday.recorday.media.dto.request.UserMediaDisplayNameUpdateRequest;
import com.recorday.recorday.media.dto.request.UserMediaRegisterRequest;
import com.recorday.recorday.media.dto.response.UserMediaResponse;
import com.recorday.recorday.media.enums.UserMediaType;
import com.recorday.recorday.util.response.PageResponse;

public interface UserMediaService {

	UserMediaResponse registerMedia(Long userId, UserMediaRegisterRequest request);

	PageResponse<UserMediaResponse> getMyMedia(Long userId, UserMediaType mediaType, int page, int size);

	String getDownloadUrl(Long userId, Long mediaId);

	UserMediaResponse updateDisplayName(Long userId, Long mediaId, UserMediaDisplayNameUpdateRequest request);

	UserMediaResponse saveTranscodedVideo(
		String userPublicId,
		String originalFileName,
		String outputS3Path,
		String transcodeJobId
	);
}
