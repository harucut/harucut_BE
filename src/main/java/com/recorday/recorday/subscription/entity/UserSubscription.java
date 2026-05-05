package com.recorday.recorday.subscription.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.recorday.recorday.subscription.enums.SubscriptionStatus;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.util.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
	name = "user_subscription",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_subscription_user_id", columnNames = {"user_id"})
	}
)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscription extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "subscription_id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "plan_tier", nullable = false, length = 16)
	// 현재 사이클에 적용되는 요금제.
	private PlanTier planTier = PlanTier.BASIC;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "subscription_status", nullable = false, length = 16)
	// 구독 상태(활성/해지/만료).
	private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

	@Builder.Default
	@Column(name = "current_cycle_start_at", nullable = false)
	// 현재 사용량 사이클 시작 시각.
	private LocalDateTime currentCycleStartAt = LocalDateTime.now();

	@Builder.Default
	@Column(name = "current_cycle_end_at", nullable = false)
	// 현재 사용량 사이클 종료 시각(정책 비교 시 상한).
	private LocalDateTime currentCycleEndAt = LocalDateTime.now().plusDays(31);

	@Builder.Default
	@Column(name = "current_video_download_count", nullable = false)
	// 현재 사이클 영상 다운로드 사용 횟수.
	private int currentVideoDownloadCount = 0;

	@Builder.Default
	@Column(name = "current_frame_create_count", nullable = false)
	// 계정 전체 누적 프레임 생성 사용 횟수.
	private int currentFrameCreateCount = 0;

	@Builder.Default
	@Column(name = "auto_renew", nullable = false)
	// true면 사이클 종료 시 자동 갱신, false면 현재 사이클까지만 유지.
	private boolean autoRenew = true;

	@Version
	@Column(name = "version", nullable = false)
	// 동시 요청 충돌 방지용 낙관적 락 버전.
	private Long version;

	@Builder.Default
	@OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
	// 사이클별 사용량 이력.
	private List<SubscriptionUsageCycle> usageCycles = new ArrayList<>();

	public static UserSubscription createDefault(User user) {
		UserSubscription subscription = UserSubscription.builder()
			.user(user)
			.planTier(PlanTier.BASIC)
			.status(SubscriptionStatus.ACTIVE)
			.currentCycleStartAt(LocalDateTime.now())
			.currentCycleEndAt(LocalDateTime.now().plusDays(31))
			.currentVideoDownloadCount(0)
			.currentFrameCreateCount(0)
			.autoRenew(true)
			.version(0L)
			.build();
		user.attachSubscription(subscription);
		return subscription;
	}

	public void changePlanTier(PlanTier planTier) {
		this.planTier = planTier;
	}

	public void syncQuotaCycle(LocalDateTime now) {
		if (now == null) {
			now = LocalDateTime.now();
		}

		if (this.currentCycleStartAt == null || this.currentCycleEndAt == null) {
			startNewQuotaCycle(now);
			return;
		}

		if (now.isBefore(this.currentCycleEndAt)) {
			return;
		}

		while (!now.isBefore(this.currentCycleEndAt)) {
			this.currentCycleStartAt = this.currentCycleEndAt;
			this.currentCycleEndAt = this.currentCycleEndAt.plusDays(31);
		}

		this.currentVideoDownloadCount = 0;
	}

	public void startNewQuotaCycle(LocalDateTime paidAt) {
		LocalDateTime base = paidAt != null ? paidAt : LocalDateTime.now();
		this.currentCycleStartAt = base;
		this.currentCycleEndAt = base.plusDays(31);
		this.currentVideoDownloadCount = 0;
	}

	public void increaseVideoDownloadCount() {
		this.currentVideoDownloadCount++;
	}

	public void increaseFrameCreateCount() {
		this.currentFrameCreateCount++;
	}

	public int getTotalFrameCreateCount() {
		return currentFrameCreateCount;
	}

	public void increaseTotalFrameCreateCount() {
		increaseFrameCreateCount();
	}
}
