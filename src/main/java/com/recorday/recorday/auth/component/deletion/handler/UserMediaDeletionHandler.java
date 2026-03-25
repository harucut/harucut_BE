package com.recorday.recorday.auth.component.deletion.handler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.recorday.recorday.auth.service.UserDeletionHandler;
import com.recorday.recorday.media.repository.UserMediaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserMediaDeletionHandler implements UserDeletionHandler {

	private final UserMediaRepository userMediaRepository;

	@Override
	@Transactional
	public void handleUserDeletion(Long userId) {
		userMediaRepository.deleteByUserId(userId);
	}
}
