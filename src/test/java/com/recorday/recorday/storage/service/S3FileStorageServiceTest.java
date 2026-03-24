package com.recorday.recorday.storage.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.recorday.recorday.storage.dto.response.PresignedUploadResponse;
import com.recorday.recorday.storage.enums.ContentType;
import com.recorday.recorday.storage.enums.UploadType;
import com.recorday.recorday.storage.strategy.UploadPathStrategy;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

	private static final String BUCKET = "recorday-bucket";
	private static final String BUCKET_URL = "https://recorday-bucket.s3.ap-northeast-2.amazonaws.com";

	@Mock
	private S3Client s3Client;

	@Mock
	private S3Presigner s3Presigner;

	@Mock
	private UploadPathStrategy profileStrategy;

	private FileStorageService fileStorageService;

	@BeforeEach
	void setUp() {
		given(profileStrategy.getUploadType()).willReturn(UploadType.PROFILE);

		Set<UploadPathStrategy> strategies = Set.of(profileStrategy);

		fileStorageService = new S3FileStorageService(
			s3Client,
			s3Presigner,
			strategies,
			BUCKET
		);
	}

	@Test
	@DisplayName("파일 업로드에 성공하면 S3 key를 반환하고, S3Client.putObject를 올바르게 호출한다")
	void uploadFile_success() {
		//given
		String dir = "profile";
		String filename = "test.png";
		byte[] bytes = "dummy-image".getBytes();
		InputStream is = new ByteArrayInputStream(bytes);

		given(s3Client.putObject(
			any(PutObjectRequest.class),
			any(RequestBody.class)
		)).willReturn(PutObjectResponse.builder().build());

		//when
		String key = fileStorageService.upload(
			dir,
			filename,
			is,
			bytes.length,
			"image/png"
		);

		//then
		assertThat(key).startsWith(dir + "/").endsWith(".png");

		ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		then(s3Client).should().putObject(
			requestCaptor.capture(),
			any(RequestBody.class)
		);

		PutObjectRequest captured = requestCaptor.getValue();
		assertThat(captured.bucket()).isEqualTo(BUCKET);
		assertThat(captured.key()).isEqualTo(key);
		assertThat(captured.contentType()).isEqualTo("image/png");
	}

	@Test
	@DisplayName("파일 삭제 시 S3Client.deleteObject가 올바른 bucket과 key로 호출된다")
	void deleteFile_success() {
		//given
		String key = "profile/test.png";

		//when
		fileStorageService.delete(key);

		//then
		ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

		then(s3Client).should().deleteObject(captor.capture());

		assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
		assertThat(captor.getValue().key()).isEqualTo(key);
	}

	@Test
	@DisplayName("파일 key와 만료 시간을 넘기면 presigned URL을 생성해서 반환한다")
	void generatePresignedGetUrl() {
		//given
		String key = "profile/test.png";
		Duration duration = Duration.ofMinutes(10);
		String expected = "https://example.com/" + key;

		SdkHttpRequest httpRequest = SdkHttpRequest.builder()
			.method(SdkHttpMethod.GET)
			.uri(URI.create(expected))
			.build();

		PresignedGetObjectRequest presignedGetObjectRequest =
			PresignedGetObjectRequest.builder()
				.httpRequest(httpRequest)
				.expiration(Instant.now().plus(duration))
				.signedHeaders(Map.of("Host", List.of("example.com")))
				.isBrowserExecutable(true)
				.build();

		given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
			.willReturn(presignedGetObjectRequest);

		//when
		String url = fileStorageService.generatePresignedGetUrl(key);

		//then
		assertThat(url).isEqualTo(expected);

		then(s3Presigner).should().presignGetObject(any(GetObjectPresignRequest.class));
	}

	@Test
	@DisplayName("한글 커스텀 파일명으로 다운로드 URL 생성 시 Content-Disposition에 filename* UTF-8 인코딩이 포함된다")
	void generatePresignedDownloadUrl_withKoreanFilename_usesRfc5987() {
		// given
		String key = "uploads/users/publicId/mp4/test.mp4";
		String downloadFilename = "한글.mp4";
		String expected = "https://example.com/" + key;

		SdkHttpRequest httpRequest = SdkHttpRequest.builder()
			.method(SdkHttpMethod.GET)
			.uri(URI.create(expected))
			.build();

		PresignedGetObjectRequest presignedGetObjectRequest =
			PresignedGetObjectRequest.builder()
				.httpRequest(httpRequest)
				.expiration(Instant.now().plus(Duration.ofMinutes(10)))
				.signedHeaders(Map.of("Host", List.of("example.com")))
				.isBrowserExecutable(true)
				.build();

		given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
			.willReturn(presignedGetObjectRequest);

		// when
		String url = fileStorageService.generatePresignedDownloadUrl(key, downloadFilename);

		// then
		assertThat(url).isEqualTo(expected);

		ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
		then(s3Presigner).should().presignGetObject(captor.capture());

		String contentDisposition = captor.getValue().getObjectRequest().responseContentDisposition();
		assertThat(contentDisposition).contains("attachment;");
		assertThat(contentDisposition).contains("filename=\"__.mp4\"");
		assertThat(contentDisposition).contains("filename*=UTF-8''%ED%95%9C%EA%B8%80.mp4");
		assertThat(captor.getValue().getObjectRequest().responseContentType()).isEqualTo("application/octet-stream");
	}

	@Test
	@DisplayName("업로드용 presigned URL을 생성하고, key와 URL을 함께 반환한다")
	void generatePresignedUploadUrl() {
		// given
		UploadType uploadType = UploadType.PROFILE;
		String originalFilename = "test.png";
		ContentType contentType = ContentType.PNG;
		Duration expiry = Duration.ofDays(1);
		String publicId = "publicId";
		boolean isTemp = false;

		// 전략이 생성할 것으로 예상되는 키
		String expectedKey = "uploads/users/publicId/profile/generated-uuid.png";
		String expectedUrl = "https://example.com/" + expectedKey;

		// 1. Mock Strategy 동작 정의
		given(profileStrategy.generateKey(publicId, originalFilename, isTemp))
			.willReturn(expectedKey);

		// 2. Mock S3Presigner 동작 정의
		SdkHttpRequest httpRequest = SdkHttpRequest.builder()
			.method(SdkHttpMethod.PUT)
			.uri(URI.create(expectedUrl))
			.build();

		PresignedPutObjectRequest presigned = PresignedPutObjectRequest.builder()
			.httpRequest(httpRequest)
			.expiration(Instant.now().plus(expiry))
			.signedHeaders(Map.of("Host", List.of("example.com")))
			.isBrowserExecutable(true)
			.build();

		given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
			.willReturn(presigned);

		// when
		PresignedUploadResponse response = fileStorageService.generatePresignedUploadUrl(
			uploadType, originalFilename, contentType, publicId, isTemp
		);

		// then
		assertThat(response.uploadUrl()).isEqualTo(expectedUrl);
		assertThat(response.key()).isEqualTo(expectedKey);
		assertThat(response.expiresIn()).isEqualTo(expiry);

		// Verify: 전략이 올바르게 호출되었는지 확인
		then(profileStrategy).should().generateKey(publicId, originalFilename, isTemp);

		// Verify: S3Presigner에 올바른 Key가 전달되었는지 확인
		ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
		then(s3Presigner).should().presignPutObject(captor.capture());

		PutObjectRequest putRequest = captor.getValue().putObjectRequest();
		assertThat(putRequest.bucket()).isEqualTo(BUCKET);
		assertThat(putRequest.key()).isEqualTo(expectedKey);
	}
}
