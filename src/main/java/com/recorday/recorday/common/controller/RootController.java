package com.recorday.recorday.common.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.recorday.recorday.util.response.Response;

@RestController
public class RootController {

	@GetMapping("/")
	public ResponseEntity<Response<Map<String, String>>> root() {
		return Response.ok(Map.of(
			"service", "recorday-api",
			"status", "UP"
		)).toResponseEntity();
	}
}
