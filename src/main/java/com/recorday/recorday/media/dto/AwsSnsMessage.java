package com.recorday.recorday.media.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AwsSnsMessage(
	@JsonProperty("Type") String type,
	@JsonProperty("MessageId") String messageId,
	@JsonProperty("TopicArn") String topicArn,
	@JsonProperty("Message") String message,
	@JsonProperty("Timestamp") String timestamp,
	@JsonProperty("SubscribeURL") String subscribeUrl
) {}
