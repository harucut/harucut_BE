package com.recorday.recorday.media.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.exception.GlobalErrorCode;
import com.recorday.recorday.media.dto.TranscodeTaskState;
import com.recorday.recorday.media.dto.response.TranscodeTaskStatusResponse;
import com.recorday.recorday.media.dto.response.TranscodeTaskSubmitResponse;
import com.recorday.recorday.media.dto.response.UserMediaResponse;
import com.recorday.recorday.media.repository.TranscodeTaskRepository;
import com.recorday.recorday.storage.exception.StorageErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.CreateJobRequest;
import software.amazon.awssdk.services.mediaconvert.model.CreateJobResponse;
import software.amazon.awssdk.services.mediaconvert.model.FileGroupSettings;
import software.amazon.awssdk.services.mediaconvert.model.Input;
import software.amazon.awssdk.services.mediaconvert.model.JobSettings;
import software.amazon.awssdk.services.mediaconvert.model.MediaConvertException;
import software.amazon.awssdk.services.mediaconvert.model.Output;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroup;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroupSettings;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodingService {

	private final MediaConvertClient mediaConvertClient;
	private final UserMediaService userMediaService;
	private final TranscodeTaskRepository transcodeTaskRepository;

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Value("${aws.mediaconvert.role-arn}")
	private String mediaConvertRoleArn;

	@Value("${aws.mediaconvert.template-name}")
	private String templateName;

	public TranscodeTaskSubmitResponse submitTranscodeTask(String userPublicId, String fileName) {
		String taskId = UUID.randomUUID().toString();
		LocalDateTime now = LocalDateTime.now();
		TranscodeTaskState queuedState = TranscodeTaskState.queued(taskId, userPublicId, fileName, now);
		transcodeTaskRepository.save(queuedState);

		String jobId = createConversionJob(userPublicId, fileName);
		LocalDateTime submittedAt = LocalDateTime.now();
		TranscodeTaskState submittedState = queuedState.withSubmitted(jobId, submittedAt);
		transcodeTaskRepository.save(submittedState);
		transcodeTaskRepository.linkJobToTask(jobId, taskId);

		return new TranscodeTaskSubmitResponse(taskId, jobId, submittedState.status(), submittedAt);
	}

	public TranscodeTaskStatusResponse getTaskStatus(String taskId, String userPublicId) {
		TranscodeTaskState state = getTaskState(taskId, userPublicId);
		return new TranscodeTaskStatusResponse(
			state.taskId(),
			state.jobId(),
			state.status(),
			state.errorMessage(),
			state.media(),
			state.createdAt(),
			state.updatedAt()
		);
	}

	public void markProgressing(String jobId) {
		updateStateByJobId(jobId, state -> state.withProgressing(LocalDateTime.now()));
	}

	public void handleCompletedJob(
		String jobId,
		String userPublicId,
		String originalFileName,
		String outputS3Path
	) {
		updateStateByJobId(jobId, state -> {
			UserMediaResponse mediaResponse = userMediaService.saveTranscodedVideo(
				userPublicId,
				originalFileName,
				outputS3Path,
				jobId
			);
			return state.withComplete(mediaResponse, LocalDateTime.now());
		});
	}

	public void handleFailedJob(String jobId, String errorMessage) {
		updateStateByJobId(jobId, state -> state.withError(errorMessage, LocalDateTime.now()));
	}

	private TranscodeTaskState getTaskState(String taskId, String userPublicId) {
		TranscodeTaskState state = transcodeTaskRepository.findByTaskId(taskId)
			.orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND, "변환 작업을 찾을 수 없습니다."));

		if (!state.userPublicId().equals(userPublicId)) {
			throw new BusinessException(GlobalErrorCode.FORBIDDEN, "다른 사용자의 변환 작업입니다.");
		}
		return state;
	}

	private void updateStateByJobId(String jobId, java.util.function.Function<TranscodeTaskState, TranscodeTaskState> updater) {
		transcodeTaskRepository.findTaskIdByJobId(jobId)
			.flatMap(transcodeTaskRepository::findByTaskId)
			.ifPresentOrElse(state -> {
				TranscodeTaskState updated = updater.apply(state);
				transcodeTaskRepository.save(updated);
				log.info("Transcode task updated. taskId={}, status={}", updated.taskId(), updated.status());
			}, () -> log.warn("Transcode task not found by jobId={}", jobId));
	}

	private String createConversionJob(String userPublicId, String fileName) {

		// 1. 경로 조립
		// 입력: s3://my-bucket/uploads/users/{id}/webm/video.webm
		String inputS3Path = String.format("s3://%s/uploads/users/%s/webm/%s", bucketName, userPublicId, fileName);

		// 출력: s3://my-bucket/uploads/users/{id}/mp4/
		String outputS3Path = String.format("s3://%s/uploads/users/%s/mp4/", bucketName, userPublicId);

		// 2. Job 설정 구성
		CreateJobRequest createJobRequest = CreateJobRequest.builder()
			.role(mediaConvertRoleArn)
			.jobTemplate(templateName)
			.settings(JobSettings.builder()
				.inputs(Input.builder().fileInput(inputS3Path).build())
				.outputGroups(OutputGroup.builder()
					.name("File Group")
					.outputs(Output.builder().build())
					.outputGroupSettings(OutputGroupSettings.builder()
						.fileGroupSettings(FileGroupSettings.builder().destination(outputS3Path).build())
						.build())
					.build())
				.build())
			.userMetadata(Map.of("userPublicId", userPublicId, "originalFileName", fileName)) // 메타데이터 추가
			.build();

		try {
			CreateJobResponse response = mediaConvertClient.createJob(createJobRequest);
			String jobId = response.job().id();
			log.info("Transcoding Job Created: {}", jobId);
			return jobId;
		} catch (MediaConvertException e) {
			log.error("AWS MediaConvert Error: {}", e.getMessage());
			throw new BusinessException(StorageErrorCode.TRANSCODE_FAILED, "비디오 변환 요청 실패");
		}
	}
}
