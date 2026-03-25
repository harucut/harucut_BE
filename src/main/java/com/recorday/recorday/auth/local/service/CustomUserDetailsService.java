package com.recorday.recorday.auth.local.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.recorday.recorday.auth.entity.CustomUserPrincipal;
import com.recorday.recorday.auth.exception.AuthErrorCode;
import com.recorday.recorday.auth.exception.CustomAuthenticationException;
import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.auth.service.UserPrincipalLoader;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserStatus;
import com.recorday.recorday.user.repository.UserRepository;
import com.recorday.recorday.util.user.UserReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService, UserPrincipalLoader {

	private final UserReader userReader;
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws CustomAuthenticationException {

		User user = userRepository.findByProviderAndEmail(Provider.HARUCUT, email)
			.orElseThrow(() -> new CustomAuthenticationException(AuthErrorCode.NOT_EXIST_USER));

		return new CustomUserPrincipal(user);
	}

	@Override
	public CustomUserPrincipal loadUserByPublicId(String publicId) throws CustomAuthenticationException {

		User user = userReader.getUserByPublicId(publicId);

		return new CustomUserPrincipal(user);
	}
}
