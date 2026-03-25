package com.recorday.recorday.subscription.entity;

import java.time.LocalDateTime;

import com.recorday.recorday.subscription.enums.PaymentEventType;
import com.recorday.recorday.subscription.enums.PaymentStatus;
import com.recorday.recorday.util.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
	name = "payment_transaction",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_payment_provider_event",
			columnNames = {"provider", "provider_event_id"}
		)
	}
)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
// 구독 단위 결제 이벤트 로그.
public class PaymentTransaction extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_tx_id")
	// 결제 트랜잭션 PK.
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subscription_id", nullable = false)
	// 이 결제 이벤트가 속한 구독.
	private UserSubscription subscription;

	@Column(name = "provider", nullable = false, length = 32)
	// 결제사 식별자(예: toss, stripe).
	private String provider;

	@Column(name = "provider_event_id", nullable = false, length = 128)
	// 결제사 이벤트 고유 ID(중복 처리 방지 키).
	private String providerEventId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	// 결제 이벤트 종류(결제/취소/환불 등).
	private PaymentEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 16)
	// 우리 시스템 기준 최종 결제 상태.
	private PaymentStatus paymentStatus;

	@Column(name = "amount", nullable = false)
	// 결제 금액.
	private int amount;

	@Column(name = "currency", nullable = false, length = 8)
	// 통화 코드(KRW, USD 등).
	private String currency;

	@Column(name = "occurred_at", nullable = false)
	// 결제사에서 이벤트가 발생한 시각.
	private LocalDateTime occurredAt;

	@Builder.Default
	@Column(name = "raw_payload", columnDefinition = "TEXT")
	// 원본 결제사 페이로드(장애 분석/감사 용도).
	private String rawPayload = "";
}
