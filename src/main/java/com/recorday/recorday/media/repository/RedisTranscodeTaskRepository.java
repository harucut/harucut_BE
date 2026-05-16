package com.recorday.recorday.media.repository;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.recorday.recorday.media.dto.TranscodeTaskState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisTranscodeTaskRepository implements TranscodeTaskRepository {

	private static final String TASK_KEY_PREFIX = "media:transcode:task:";
	private static final String JOB_KEY_PREFIX = "media:transcode:job:";
	private static final Duration TASK_TTL = Duration.ofDays(7);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public void save(TranscodeTaskState state) {
		String key = TASK_KEY_PREFIX + state.taskId();
		try {
			String payload = objectMapper.writeValueAsString(state);
			redisTemplate.opsForValue().set(key, payload, TASK_TTL);
		} catch (JacksonException e) {
			throw new IllegalStateException("Transcode task state serialize 실패", e);
		}
	}

	@Override
	public Optional<TranscodeTaskState> findByTaskId(String taskId) {
		String key = TASK_KEY_PREFIX + taskId;
		String payload = redisTemplate.opsForValue().get(key);
		if (payload == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(payload, TranscodeTaskState.class));
		} catch (JacksonException e) {
			log.error("Transcode task state deserialize 실패. key={}", key, e);
			return Optional.empty();
		}
	}

	@Override
	public void linkJobToTask(String jobId, String taskId) {
		String key = JOB_KEY_PREFIX + jobId;
		redisTemplate.opsForValue().set(key, taskId, TASK_TTL);
	}

	@Override
	public Optional<String> findTaskIdByJobId(String jobId) {
		String key = JOB_KEY_PREFIX + jobId;
		return Optional.ofNullable(redisTemplate.opsForValue().get(key));
	}
}
