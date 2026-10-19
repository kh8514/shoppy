package com.devrlab.shoppy.auth.service;

import com.devrlab.shoppy.auth.dto.TokenResponse;
import com.devrlab.shoppy.common.exception.BusinessException;
import com.devrlab.shoppy.user.entity.User;
import com.devrlab.shoppy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
	private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TTL = Duration.ofDays(14);

	private final UserRepository userRepository;
	private final StringRedisTemplate redis;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final SecureRandom random = new SecureRandom();

	public TokenResponse signup(String email, String password) {
		email = normalize(email);
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
		}
		User user = userRepository.save(new User(email, passwordEncoder.encode(password)));
		return issue(user);
	}

	public TokenResponse login(String email, String password) {
		User user = userRepository.findByEmail(normalize(email))
				.filter(u -> passwordEncoder.matches(password, u.getPassword()))
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
		return issue(user);
	}

	// 조회와 삭제를 한 번에 → 같은 토큰으로 동시에 두 번 요청해도 한쪽만 성공
	public TokenResponse refresh(String refreshToken) {
		String userId = redis.opsForValue().getAndDelete(key(refreshToken));
		if (userId == null) {
			throw new BusinessException(HttpStatus.UNAUTHORIZED, "다시 로그인 해주세요.");
		}
		User user = userRepository.findById(Long.valueOf(userId))
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "다시 로그인 해주세요."));
		return issue(user);
	}

	public void logout(String refreshToken) {
		redis.delete(key(refreshToken));
	}

	private TokenResponse issue(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plus(ACCESS_TTL))
				.claim("roles", List.of(user.getRole().name()))
				.build();
		String accessToken = jwtEncoder.encode(
				JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		redis.opsForValue().set(key(refreshToken), user.getId().toString(), REFRESH_TTL);

		return new TokenResponse(accessToken, refreshToken);
	}

	private static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	// A@x.com 과 a@x.com 이 별개 계정이 되지 않도록
	private static String normalize(String email) {
		return email.trim().toLowerCase();
	}

	private static String key(String refreshToken) {
		return "refresh:" + hash(refreshToken);
	}
}
