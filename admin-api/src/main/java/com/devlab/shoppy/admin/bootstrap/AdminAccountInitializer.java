package com.devlab.shoppy.admin.bootstrap;

import com.devlab.shoppy.user.entity.User;
import com.devlab.shoppy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 시작 시 ADMIN_EMAIL 계정을 ADMIN 으로 보장. 없으면 ADMIN_PASSWORD 로 생성, 있으면 승격만 (비밀번호 유지)
// 토큰은 api 의 /api/auth/login 으로 발급
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${admin.email:}")
	private String email;
	@Value("${admin.password:}")
	private String password;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (email.isBlank()) return;
		String normalized = email.trim().toLowerCase(); // AuthService 와 동일 규칙

		userRepository.findByEmail(normalized).ifPresentOrElse(
				User::promoteToAdmin,
				() -> {
					if (password.length() < 8) {
						throw new IllegalStateException("ADMIN_PASSWORD 는 8자 이상이어야 합니다.");
					}
					User admin = new User(normalized, passwordEncoder.encode(password));
					admin.promoteToAdmin();
					userRepository.save(admin);
				});
		log.info("Admin account ready: {}", normalized);
	}
}
