package com.recorday.recorday.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;

@Configuration
public class MediaConvertConfig {

	@Value("${aws.credentials.access-key}")
	private String accessKey;

	@Value("${aws.credentials.secret-key}")
	private String secretKey;

	@Value("${aws.mediaconvert.endpoint}")
	private String mediaConvertEndpoint;

	@Bean
	public MediaConvertClient mediaConvertClient() {
		// 1. yml에 있는 키 값으로 자격 증명 객체 생성
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

		// 2. 클라이언트에 자격 증명과 엔드포인트 강제 주입
		return MediaConvertClient.builder()
			.region(Region.AP_NORTHEAST_2)
			.endpointOverride(URI.create(mediaConvertEndpoint))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();
	}
}
