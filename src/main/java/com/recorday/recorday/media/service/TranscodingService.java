package com.recorday.recorday.media.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.storage.exception.StorageErrorCode;
import com.recorday.recorday.util.response.Response;

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

	private final Map<String, DeferredResult<ResponseEntity<Response<Void>>>> pendingRequests = new ConcurrentHashMap<>();

	@Value("${aws.s3.bucket-name}")
	private String bucketName;

	@Value("${aws.mediaconvert.role-arn}")
	private String mediaConvertRoleArn;

	@Value("${aws.mediaconvert.template-name}")
	private String templateName;

	public String createConversionJob(String userPublicId, String fileName) {

		// 1. 경로 조립
		// 입력: s3://my-bucket/uploads/users/{id}/webm/video.webm
		String inputS3Path = String.format("s3://%s/uploads/users/%s/webm/%s", bucketName, userPublicId, fileName);

		// 출력: s3://my-bucket/uploads/users/{id}/mp4/
		String outputS3Path = String.format("s3://%s/uploads/users/%s/mp4/", bucketName, userPublicId);

		Map<String, String> userMetadata = new HashMap<>();
		userMetadata.put("userPublicId", userPublicId);
		userMetadata.put("originalFileName", fileName);

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
			throw new RuntimeException("비디오 변환 요청 실패", e);
		}
	}

	public void registerDeferredResult(String jobId, DeferredResult<ResponseEntity<Response<Void>>> deferredResult) {
		pendingRequests.put(jobId, deferredResult);

		deferredResult.onCompletion(() -> pendingRequests.remove(jobId));
		deferredResult.onTimeout(() -> {
			pendingRequests.remove(jobId);
			deferredResult.setErrorResult(new BusinessException(StorageErrorCode.TRANSCODE_FAILED));
		});
	}

	public void completeJob(String jobId, boolean isSuccess, String message) {
		DeferredResult<ResponseEntity<Response<Void>>> result = pendingRequests.remove(jobId);

		if (result != null && !result.isSetOrExpired()) {
			if (isSuccess) {
				result.setResult(Response.ok().toResponseEntity());
			} else {
				result.setErrorResult(new BusinessException(StorageErrorCode.TRANSCODE_FAILED));
				log.error("Job Failed: {}", message);
			}
		}
	}
}
