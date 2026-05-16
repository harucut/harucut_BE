package com.recorday.recorday.media.controller;

import java.net.URI;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.recorday.recorday.media.dto.AwsSnsMessage;
import com.recorday.recorday.media.service.TranscodingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 반드시 tools.jackson 패키지로 통일해야 합니다!
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/mediaconvert")
@RequiredArgsConstructor
public class WebhookController {

	private final ObjectMapper objectMapper;
	private final TranscodingService transcodingService;

	@PostMapping
	public void handleMediaConvertNotification(@RequestBody String rawBody) {
		try {
			AwsSnsMessage snsMessage = objectMapper.readValue(rawBody, AwsSnsMessage.class);

			if ("SubscriptionConfirmation".equals(snsMessage.type())) {
				log.info("AWS SNS 구독 확인 요청 도착");
				new RestTemplate().getForObject(URI.create(snsMessage.subscribeUrl()), String.class);
				log.info("AWS SNS 구독 승인 완료!");
			} else if ("Notification".equals(snsMessage.type())) {
				processConversionResult(snsMessage.message());
			}

		} catch (Exception e) {
			log.error("Webhook 처리 중 오류 발생: {}", e.getMessage());
		}
	}

	private void processConversionResult(String messageJson) throws JacksonException {
		JsonNode root = objectMapper.readTree(messageJson);
		JsonNode detail = root.path("detail");

		String jobId = detail.path("jobId").asText();
		String state = detail.path("status").asText("UNKNOWN");

		if ("COMPLETE".equals(state)) {
			JsonNode userMetadata = detail.path("userMetadata");
			String userPublicId = userMetadata.path("userPublicId").asText(null);
			String originalFileName = userMetadata.path("originalFileName").asText(null);
			String outputS3Path = detail
				.path("outputGroupDetails").path(0)
				.path("outputDetails").path(0)
				.path("outputFilePaths").path(0).asText(null);

			log.info("✅ 변환 완료! JobID: {}, Path: {}", jobId, outputS3Path);

			try {
				transcodingService.handleCompletedJob(
					jobId,
					userPublicId,
					originalFileName,
					outputS3Path
				);
			} catch (Exception e) {
				log.error("변환 완료 후 DB 저장 실패. jobId={}, error={}", jobId, e.getMessage(), e);
				transcodingService.handleFailedJob(jobId, "Failed to persist transcoded video");
			}

		} else if ("PROGRESSING".equals(state) || "STATUS_UPDATE".equals(state)) {
			transcodingService.markProgressing(jobId);
		} else if ("ERROR".equals(state)) {
			String errorMessage = detail.path("errorMessage").asText("Unknown Error");
			log.error("❌ 변환 실패 JobID: {}", jobId);

			transcodingService.handleFailedJob(jobId, errorMessage);
		}
	}
}
