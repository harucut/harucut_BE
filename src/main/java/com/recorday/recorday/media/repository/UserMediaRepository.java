package com.recorday.recorday.media.repository;

import java.util.List;
import java.util.Optional;

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

	Optional<UserMedia> findByIdAndUser(Long mediaId, User user);

	boolean existsByS3Key(String s3Key);

	Optional<UserMedia> findByS3Key(String s3Key);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM UserMedia um WHERE um.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);
}
