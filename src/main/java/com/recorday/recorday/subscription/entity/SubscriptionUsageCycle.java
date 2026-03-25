package com.recorday.recorday.subscription.entity;

import java.time.LocalDateTime;

import com.recorday.recorday.util.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
	name = "subscription_usage_cycle",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_subscription_cycle_start",
			columnNames = {"subscription_id", "cycle_start_at"}
		)
	}
)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
// 구독 사이클별 사용량 이력 스냅샷.
public class SubscriptionUsageCycle extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "usage_cycle_id")
	// 사용량 사이클 PK.
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription_id", nullable = false)
	// 대상 구독.
	private UserSubscription subscription;

	@Column(name = "cycle_start_at", nullable = false)
	// 이 사용량 버킷의 시작 시각.
	private LocalDateTime cycleStartAt;

	@Column(name = "cycle_end_at", nullable = false)
	// 이 사용량 버킷의 종료 시각.
	private LocalDateTime cycleEndAt;

	@Builder.Default
	@Column(name = "video_download_count", nullable = false)
	// 해당 사이클 영상 다운로드 사용 횟수.
	private int videoDownloadCount = 0;

	@Builder.Default
	@Column(name = "frame_create_count", nullable = false)
	// 해당 사이클 프레임 생성 사용 횟수.
	private int frameCreateCount = 0;

	@Version
	@Column(name = "version", nullable = false)
	// 동시 사용량 증가 충돌 방지용 낙관적 락 버전.
	private Long version;
}
