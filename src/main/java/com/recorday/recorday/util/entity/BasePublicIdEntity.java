package com.recorday.recorday.util.entity;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BasePublicIdEntity extends BaseEntity{

	@Column(name = "public_id", nullable = false, unique = true, length = 12)
	private String publicId;

	@PrePersist
	public void generatePublicId() {
		if (this.publicId == null) {
			this.publicId = NanoIdUtils.randomNanoId(
				NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
				NanoIdUtils.DEFAULT_ALPHABET,
				10
			);
		}
		onPrePersist();
	}

	protected void onPrePersist() {}
}
