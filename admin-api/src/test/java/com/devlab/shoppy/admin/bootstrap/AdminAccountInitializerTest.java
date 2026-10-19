package com.devlab.shoppy.admin.bootstrap;

import com.devlab.shoppy.TestcontainersConfig;
import com.devlab.shoppy.user.entity.User;
import com.devlab.shoppy.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"admin.email=Boss@Shop.com", "admin.password=admin-pass1"})
@Import(TestcontainersConfig.class)
class AdminAccountInitializerTest {

	@Autowired AdminAccountInitializer initializer;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	@Test
	void 없는_계정이면_소문자_이메일의_ADMIN으로_생성() throws Exception {
		userRepository.deleteAll(); // 앱 시작 시 생성된 계정 제거 후 직접 실행
		initializer.run(null);

		User admin = userRepository.findByEmail("boss@shop.com").orElseThrow();
		assertThat(admin.getRole()).isEqualTo(User.Role.ADMIN);
		assertThat(passwordEncoder.matches("admin-pass1", admin.getPassword())).isTrue();
	}

	@Test
	void 이미_가입된_계정은_비밀번호_유지하고_ADMIN으로_승격() throws Exception {
		userRepository.deleteAll();
		userRepository.save(new User("boss@shop.com", passwordEncoder.encode("my-own-pass")));

		initializer.run(null);

		User admin = userRepository.findByEmail("boss@shop.com").orElseThrow();
		assertThat(admin.getRole()).isEqualTo(User.Role.ADMIN);
		assertThat(passwordEncoder.matches("my-own-pass", admin.getPassword())).isTrue();
		assertThat(userRepository.count()).isEqualTo(1);
	}
}
