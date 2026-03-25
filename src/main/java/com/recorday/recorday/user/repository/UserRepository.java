package com.recorday.recorday.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	boolean existsByProviderAndEmail(Provider provider, String email);

	Optional<User> findByProviderAndEmail(Provider provider, String email);

	Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

	Optional<User> findByPublicId(String publicId);

	@Query("""
        select u.id
        from User u
        where u.userStatus = :userStatus
          and u.deleteRequestedAt <= :cutoffDate
    """)
	List<Long> findExpiredDeleteRequestedUserIds(
		UserStatus userStatus,
		LocalDateTime cutoffDate
	);
}
