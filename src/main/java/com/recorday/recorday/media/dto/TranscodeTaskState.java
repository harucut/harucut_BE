package com.recorday.recorday.media.dto;

import java.time.LocalDateTime;

import com.recorday.recorday.media.dto.response.UserMediaResponse;
import com.recorday.recorday.media.enums.TranscodeTaskStatus;

public record TranscodeTaskState(
	String taskId,
	String userPublicId,
	String originalFileName,
	String jobId,
	TranscodeTaskStatus status,
	String errorMessage,
	UserMediaResponse media,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static TranscodeTaskState queued(String taskId, String userPublicId, String originalFileName, LocalDateTime now) {
		return new TranscodeTaskState(
			taskId,
			userPublicId,
			originalFileName,
			null,
			TranscodeTaskStatus.QUEUED,
			null,
			null,
			now,
			now
		);
	}

	public TranscodeTaskState withSubmitted(String newJobId, LocalDateTime now) {
		return new TranscodeTaskState(
			taskId,
			userPublicId,
			originalFileName,
			newJobId,
			TranscodeTaskStatus.SUBMITTED,
			null,
			null,
			createdAt,
			now
		);
	}

	public TranscodeTaskState withProgressing(LocalDateTime now) {
		return new TranscodeTaskState(
			taskId,
			userPublicId,
			originalFileName,
			jobId,
			TranscodeTaskStatus.PROGRESSING,
			null,
			media,
			createdAt,
			now
		);
	}

	public TranscodeTaskState withComplete(UserMediaResponse mediaResponse, LocalDateTime now) {
		return new TranscodeTaskState(
			taskId,
			userPublicId,
			originalFileName,
			jobId,
			TranscodeTaskStatus.COMPLETE,
			null,
			mediaResponse,
			createdAt,
			now
		);
	}

	public TranscodeTaskState withError(String message, LocalDateTime now) {
		return new TranscodeTaskState(
			taskId,
			userPublicId,
			originalFileName,
			jobId,
			TranscodeTaskStatus.ERROR,
			message,
			media,
			createdAt,
			now
		);
	}
}
