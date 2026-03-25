package com.recorday.recorday.storage.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.recorday.recorday.exception.BusinessException;
import com.recorday.recorday.exception.GlobalErrorCode;
import com.recorday.recorday.storage.dto.response.PresignedUploadResponse;
import com.recorday.recorday.storage.enums.ContentType;
import com.recorday.recorday.storage.enums.UploadType;
import com.recorday.recorday.storage.exception.StorageErrorCode;
import com.recorday.recorday.storage.strategy.UploadPathStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final String bucketName;
	private final Map<UploadType, UploadPathStrategy> strategyMap;

	private static final Duration EXPIRY = Duration.ofDays(1);

	public S3FileStorageService(
		S3Client s3Client,
		S3Presigner s3Presigner,
		Set<UploadPathStrategy> strategies,
		String bucketName
	) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.bucketName = bucketName;
		this.strategyMap = strategies.stream()
			.collect(Collectors.toMap(UploadPathStrategy::getUploadType, Function.identity()));
	}

	@Deprecated
	@Override
	public String upload(String dir, String filename, InputStream inputStream, long contentLength, String contentType) {

		String extension = extractExtension(filename);
		String uniqueName = extension.isEmpty()
			? UUID.randomUUID().toString()
			: UUID.randomUUID() + "." + extension;
		String key = (dir != null && !dir.isBlank())
			? dir + "/" + uniqueName
			: uniqueName;

		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.contentType(contentType)
			.contentLength(contentLength)
			.build();

		s3Client.putObject(
			request,
			RequestBody.fromInputStream(inputStream, contentLength)
		);

		log.info("Uploaded file to S3. bucket={}, key={}", bucketName, key);
		return key;
	}

	@Override
	public void delete(String key) {

		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.build();

		s3Client.deleteObject(deleteObjectRequest);

		log.info("Deleted file from S3. bucket={}, key={}", bucketName, key);
	}

	@Override
	public String generatePresignedGetUrl(String key) {

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(EXPIRY)
			.getObjectRequest(getObjectRequest)
			.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}

	@Override
	public String generatePresignedDownloadUrl(String key) {
		return generatePresignedDownloadUrl(key, null);
	}

	@Override
	public String generatePresignedDownloadUrl(String key, String downloadFileName) {
		String filename = StringUtils.hasText(downloadFileName)
			? sanitizeFilename(downloadFileName)
			: extractFilenameFromKey(key);
		String contentDisposition = buildContentDisposition(filename);

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.responseContentDisposition(contentDisposition)
			.responseContentType("application/octet-stream")
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(EXPIRY)
			.getObjectRequest(getObjectRequest)
			.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}

	@Override
	public PresignedUploadResponse generatePresignedUploadUrl(
		UploadType uploadType,
		String originalFilename,
		ContentType contentType,
		String publicId,
		boolean isTemp
	) {

		String extension = extractExtension(originalFilename);
		ContentType validatedContentType = ContentType.validate(
			contentType.getMimeType(),
			extension
		);

		UploadPathStrategy strategy = strategyMap.get(uploadType);
		if (strategy == null) {
			throw new BusinessException(StorageErrorCode.UNSUPPORTED_UPLOAD_TYPE,
				StorageErrorCode.UNSUPPORTED_UPLOAD_TYPE.getMessage() + ": " + uploadType.name());
		}

		String key = strategy.generateKey(publicId, originalFilename, isTemp);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(key)
			.contentType(validatedContentType.getMimeType())
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(EXPIRY)
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
		String url = presigned.url().toString();

		log.info("Generated presigned upload URL. bucket={}, key={}", bucketName, key);

		return new PresignedUploadResponse(key, url, validatedContentType.getMimeType(), EXPIRY);
	}

	@Override
	public String moveFile(String sourceKey, String destinationKey) {

		if (!StringUtils.hasText(sourceKey) || !StringUtils.hasText(destinationKey)) {
			throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE, "Source Key 또는 Destination Key가 비어있습니다.");
		}

		try {
			CopyObjectRequest copyRequest = CopyObjectRequest.builder()
				.sourceBucket(bucketName)
				.sourceKey(sourceKey)
				.destinationBucket(bucketName)
				.destinationKey(destinationKey)
				.build();

			s3Client.copyObject(copyRequest);

			DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(sourceKey)
				.build();

			s3Client.deleteObject(deleteRequest);

			log.info("S3 Moved: {} -> {}", sourceKey, destinationKey);

			return destinationKey;
		} catch (S3Exception e) {
			if (e.statusCode() == 404 || "NoSuchKey".equals(e.awsErrorDetails().errorCode())) {
				log.warn("Temp file not found (Expired?): {}", sourceKey);
				throw new BusinessException(GlobalErrorCode.FILE_EXPIRED, "임시 파일이 만료되었습니다. 이미지를 다시 업로드해주세요.");
			}

			log.error("AWS S3 Error during move: {} -> {}", sourceKey, destinationKey, e);
			throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 이동 실패");
		} catch (Exception e) {
			log.error("Unexpected error during file move", e);
			throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "파일 이동 중 알 수 없는 오류가 발생했습니다.");
		}
	}

	private String extractExtension(String filename) {
		if (filename == null)
			return "";
		int idx = filename.lastIndexOf('.');
		if (idx == -1 || idx == filename.length() - 1)
			return "";
		return filename.substring(idx + 1);
	}

	private String extractFilenameFromKey(String key) {
		if (!StringUtils.hasText(key)) {
			return "file";
		}

		int idx = key.lastIndexOf('/');
		String filename = (idx >= 0 && idx < key.length() - 1) ? key.substring(idx + 1) : key;
		return sanitizeFilename(filename);
	}

	private String sanitizeFilename(String filename) {
		if (!StringUtils.hasText(filename)) {
			return "file";
		}

		String sanitized = filename
			.replace("\\", "")
			.replace("/", "_")
			.replace("\"", "")
			.replace(";", "_")
			.replace(":", "_")
			.replace("\r", "")
			.replace("\n", "")
			.trim();

		return sanitized.isEmpty() ? "file" : sanitized;
	}

	private String buildContentDisposition(String filename) {
		String asciiFallback = buildAsciiFallbackFilename(filename);
		String encodedUtf8Name = encodeRfc5987Value(filename);
		return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedUtf8Name;
	}

	private String buildAsciiFallbackFilename(String filename) {
		int dotIndex = filename.lastIndexOf('.');
		String base = filename;
		String extension = "";

		if (dotIndex > 0 && dotIndex < filename.length() - 1) {
			base = filename.substring(0, dotIndex);
			extension = filename.substring(dotIndex);
		}

		String asciiBase = base
			.replaceAll("[^\\x20-\\x7E]", "_")
			.replaceAll("[^A-Za-z0-9._ -]", "_")
			.replaceAll("\\s+", " ")
			.trim();

		if (!StringUtils.hasText(asciiBase)) {
			asciiBase = "download";
		}

		String asciiExtension = extension.replaceAll("[^A-Za-z0-9.]", "");
		if (".".equals(asciiExtension)) {
			asciiExtension = "";
		}

		return asciiBase + asciiExtension;
	}

	private String encodeRfc5987Value(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		StringBuilder encoded = new StringBuilder(bytes.length * 3);

		for (byte b : bytes) {
			int c = b & 0xFF;
			if (isRfc5987AttrChar(c)) {
				encoded.append((char)c);
			} else {
				encoded.append('%');
				encoded.append(toHexUpper(c >> 4));
				encoded.append(toHexUpper(c));
			}
		}

		return encoded.toString();
	}

	private boolean isRfc5987AttrChar(int c) {
		return (c >= '0' && c <= '9')
			|| (c >= 'A' && c <= 'Z')
			|| (c >= 'a' && c <= 'z')
			|| c == '!'
			|| c == '#'
			|| c == '$'
			|| c == '&'
			|| c == '+'
			|| c == '-'
			|| c == '.'
			|| c == '^'
			|| c == '_'
			|| c == '`'
			|| c == '|'
			|| c == '~';
	}

	private char toHexUpper(int value) {
		int nibble = value & 0x0F;
		return (char)(nibble < 10 ? ('0' + nibble) : ('A' + nibble - 10));
	}
}
