package com.recorday.recorday.auth.oauth2.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.auth.jwt.dto.TokenResponse;
import com.recorday.recorday.auth.service.AuthorizationCodeTokenService;
import com.recorday.recorday.util.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthCodeController {

	private final AuthorizationCodeTokenService authorizationCodeTokenService;

	@Deprecated
	@PostMapping("/oauth2/authorize/complete")
	public ResponseEntity<Response<TokenResponse>> authCode(
		@RequestParam String code
	) {
		TokenResponse tokenResponse = authorizationCodeTokenService.issueTokens(code);

		return Response.ok(tokenResponse).toResponseEntity();
	}
}