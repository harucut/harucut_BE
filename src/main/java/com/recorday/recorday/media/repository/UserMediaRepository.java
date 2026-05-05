package com.recorday.recorday.media.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.recorday.recorday.media.entity.UserMedia;
import com.recorday.recorday.media.enums.UserMediaType;
import com.recorday.recorday.user.entity.User;

@Repository
public interface UserMediaRepository extends JpaRepository<UserMedia, Long> {

	List<UserMedia> findAllByUserOrderByCreatedAtDesc(User user);

	List<UserMedia> findAllByUserAndMediaTypeOrderByCreatedAtDesc(User user, UserMediaType mediaType);

	Page<UserMedia> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

	Page<UserMedia> findAllByUserAndMediaTypeOrderByCreatedAtDesc(
		User user,
		UserMediaType mediaType,
		Pageable pageable
	);

	Page<UserMedia> findAllByUserAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
		User user,
		LocalDateTime cutoff,
		Pageable pageable
	);

	Page<UserMedia> findAllByUserAndMediaTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
		User user,
		UserMediaType mediaType,
		LocalDateTime cutoff,
		Pageable pageable
	);

	Optional<UserMedia> findByIdAndUser(Long mediaId, User user);

	boolean existsByS3Key(String s3Key);

	Optional<UserMedia> findByS3Key(String s3Key);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM UserMedia um WHERE um.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);
}
