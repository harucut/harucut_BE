package com.recorday.recorday.media.repository;

import java.util.Optional;

import com.recorday.recorday.media.dto.TranscodeTaskState;

public interface TranscodeTaskRepository {

	void save(TranscodeTaskState state);

	Optional<TranscodeTaskState> findByTaskId(String taskId);

	void linkJobToTask(String jobId, String taskId);

	Optional<String> findTaskIdByJobId(String jobId);
}
