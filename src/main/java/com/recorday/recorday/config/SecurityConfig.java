package com.recorday.recorday.config;

import java.util.List;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.recorday.recorday.auth.jwt.filter.JwtAuthenticationFilter;
import com.recorday.recorday.auth.jwt.service.JwtTokenService;
import com.recorday.recorday.auth.jwt.service.RefreshTokenService;
import com.recorday.recorday.auth.oauth2.service.CustomOAuth2UserService;
import com.recorday.recorday.auth.oauth2.service.CustomOidcUserService;
import com.recorday.recorday.auth.service.UserPrincipalLoader;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private static final List<String> AUTH_EXCLUDED_PATHS = List.of(
		"/",
		"/swagger-ui/**",
		"/v3/api-docs/**",
		"/api/harucut/login",
		"/api/harucut/register",
		"/api/harucut/reissue",
		"/api/harucut/logout",
		"/api/harucut/reset/password",
		"/api/harucut/reset/password/verification",
		"/api/oauth2/**",
		"/api/email-auth/**",
		"/api/webhooks/**"
	);

	private static final List<String> CORS_WHITELIST = List.of(
		"https://harucut.com",
		"https://www.harucut.com",
		"http://localhost:5173",
		"http://localhost:3000",
		"https://recorday.vercel.app"
	);

	private final UserDetailsService userDetailsService;
	private final UserPrincipalLoader userPrincipalLoader;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final CustomOidcUserService customOidcUserService;
	private final AuthenticationSuccessHandler customOAuth2SuccessHandler;
	private final AuthenticationFailureHandler customOAuth2FailureHandler;
	private final AuthenticationEntryPoint customAuthenticationEntryPoint;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authManagerBuilder =
			http.getSharedObject(AuthenticationManagerBuilder.class);

		authManagerBuilder
			.userDetailsService(userDetailsService)
			.passwordEncoder(passwordEncoder());

		return authManagerBuilder.build();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.cors(cors -> cors.configurationSource(corsConfigurationSource()));

		http
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)
					.oidcUserService(customOidcUserService)
				)
				.successHandler(customOAuth2SuccessHandler)
				.failureHandler(customOAuth2FailureHandler)
			);

		http
			.addFilterBefore(
				new JwtAuthenticationFilter(jwtTokenService, refreshTokenService, userPrincipalLoader,
					customAuthenticationEntryPoint),
				UsernamePasswordAuthenticationFilter.class
			);

		http
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(customAuthenticationEntryPoint)
			);

		http
			.authorizeHttpRequests(auth -> auth
				.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
				.requestMatchers("/favicon.ico", "/error", "/default-ui.css").permitAll()
				.requestMatchers(AUTH_EXCLUDED_PATHS.toArray(String[]::new)).permitAll()
				.requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/auth/user/**").hasRole("USER")
				.anyRequest().authenticated()
			);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		// 프론트엔드 Origin 허용
		config.setAllowedOrigins(CORS_WHITELIST);

		// 허용할 HTTP 메서드
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		// 허용할 헤더
		config.setAllowedHeaders(List.of("*"));

		// 인증 정보(쿠키, Authorization 헤더 등) 포함 허용
		config.setAllowCredentials(true);

		// 클라이언트에서 읽을 수 있는 헤더 (옵션)
		config.setExposedHeaders(List.of("Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}
