package com.recorday.recorday.media.entity;

import com.recorday.recorday.media.enums.UserMediaType;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.util.entity.BasePublicIdEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
	name = "user_media",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_media_s3_key", columnNames = {"s3_key"})
	},
	indexes = {
		@Index(name = "idx_user_media_public_id", columnList = "public_id"),
		@Index(name = "idx_user_media_user_id", columnList = "user_id"),
		@Index(name = "idx_user_media_type", columnList = "media_type")
	}
)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserMedia extends BasePublicIdEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "media_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", nullable = false, length = 32)
	private UserMediaType mediaType;

	@Column(name = "s3_key", nullable = false, length = 512)
	private String s3Key;

	@Column(name = "original_s3_key", length = 512)
	private String originalS3Key;

	@Column(name = "original_file_name", length = 255)
	private String originalFileName;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(name = "transcode_job_id", length = 128)
	private String transcodeJobId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public static UserMedia ofPhoto(User user, String s3Key, String displayName) {
		return UserMedia.builder()
			.user(user)
			.mediaType(UserMediaType.PHOTO)
			.s3Key(s3Key)
			.displayName(displayName)
			.build();
	}

	public static UserMedia ofVideo(
		User user,
		String s3Key,
		String originalS3Key,
		String originalFileName,
		String displayName,
		String transcodeJobId
	) {
		return UserMedia.builder()
			.user(user)
			.mediaType(UserMediaType.VIDEO)
			.s3Key(s3Key)
			.originalS3Key(originalS3Key)
			.originalFileName(originalFileName)
			.displayName(displayName)
			.transcodeJobId(transcodeJobId)
			.build();
	}

	public void changeDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
