package com.recorday.recorday.auth.component.deletion.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.recorday.recorday.auth.batch.UserExitBatchService;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {

	private final UserRepository userRepository;
	private final UserExitBatchService userExitBatchService;
	private final Clock clock;

	@Scheduled(cron = "0 0 0 * * *")
	public void run() {

		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime threshold = now.minusDays(7);


		List<Long> userIds = userRepository.findExpiredDeleteRequestedUserIds(
			UserStatus.DELETED_REQUESTED,
			threshold
		);

		for (Long userId : userIds) {
			try {
				log.info("userId={} 탈퇴 배치 처리 시작", userId);
				userExitBatchService.exitInNewTransaction(userId);
			} catch (Exception e) {
				log.warn("[탈퇴 일괄처리 예외] 발생 유저 Id {} ", userId, e);
			}
		}
	}
}
