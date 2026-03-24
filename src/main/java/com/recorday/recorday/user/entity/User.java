package com.recorday.recorday.user.entity;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.frame.entity.Frame;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.enums.PlanTier;
import com.recorday.recorday.util.entity.BasePublicIdEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
	name = "users",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_provider_email",
			columnNames = {"provider", "email"}
		),
		@UniqueConstraint(
			name = "uk_provider_provider_id",
			columnNames = {"provider", "provider_id"}
		)
	},
	indexes = {
		@Index(name = "idx_user_public_id", columnList = "public_id")
	}
)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BasePublicIdEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Provider provider;

	@Column(name = "provider_id", length = 64)
	private String providerId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserRole userRole;

	@Column(nullable = false)
	private String email;

	private String password;

	@Column(nullable = false)
	private String username;

	@Column(nullable = false, length = 1024)
	private String profileUrl;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserStatus userStatus;

	private LocalDateTime deleteRequestedAt;

	@Builder.Default
	@Column(name = "plan_tier", nullable = false, length = 16)
	@Enumerated(EnumType.STRING)
	private PlanTier planTier = PlanTier.BASIC;

	@Builder.Default
	@Column(name = "quota_month", nullable = false, length = 7)
	private String quotaMonth = YearMonth.now().toString();

	@Builder.Default
	@Column(name = "monthly_video_download_count", nullable = false)
	private int monthlyVideoDownloadCount = 0;

	@Builder.Default
	@Column(name = "monthly_frame_create_count", nullable = false)
	private int monthlyFrameCreateCount = 0;

	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Frame> frames = new ArrayList<>();

	// 연관관계 편의 메서드
	public void addFrame(Frame frame) {
		this.frames.add(frame);
		frame.assignUser(this);
	}

	public void removeFrame(Frame frame) {
		this.frames.remove(frame);
	}


	public void deleteRequested() {
		this.userStatus = UserStatus.DELETED_REQUESTED;
		this.deleteRequestedAt = LocalDateTime.now();
	}

	public void reActivate() {
		this.userStatus = UserStatus.ACTIVE;
		this.deleteRequestedAt = null;
	}

	public void delete() {
		this.userStatus = UserStatus.DELETED;
		this.email = "deleted_" + this.id + "@recorday.local";
		this.username = "탈퇴한 사용자";
		this.password = null;
		this.profileUrl = "resources/defaults/userDefaultImage.png";
		this.providerId = null;
	}

	public boolean isReadyForDeletion(LocalDateTime now) {
		if (this.userStatus != UserStatus.DELETED_REQUESTED || this.deleteRequestedAt == null) {
			return false;
		}
		return this.deleteRequestedAt.plusDays(7).isBefore(now);
	}

	public void changePassword(String password) {
		this.password = password;
	}

	public void changeUsername(String username) {
		this.username = username;
	}

	public void changeProfileUrl(String profileUrl) {
		this.profileUrl = profileUrl;
	}

	public void changePlanTier(PlanTier planTier) {
		this.planTier = planTier;
	}

	public void syncQuotaMonth(YearMonth currentMonth) {
		String target = currentMonth.toString();
		if (target.equals(this.quotaMonth)) {
			return;
		}

		this.quotaMonth = target;
		this.monthlyVideoDownloadCount = 0;
		this.monthlyFrameCreateCount = 0;
	}

	public void increaseMonthlyVideoDownloadCount() {
		this.monthlyVideoDownloadCount++;
	}

	public void increaseMonthlyFrameCreateCount() {
		this.monthlyFrameCreateCount++;
	}
}
