package com.devlab.shoppy.auth;

import com.devlab.shoppy.TestcontainersConfig;
import com.devlab.shoppy.user.entity.User;
import com.devlab.shoppy.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthApiTest {

	@Autowired MockMvc mvc;
	@Autowired UserRepository userRepository;
	@Autowired StringRedisTemplate redis;
	@Autowired PasswordEncoder passwordEncoder;

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
		Set<String> keys = redis.keys("refresh:*");
		if (!keys.isEmpty()) redis.delete(keys);
	}

	// --- 회원가입 ---

	@Test
	void 회원가입_성공시_토큰발급_이메일은_소문자_비밀번호는_해시로_저장() throws Exception {
		signup("New@Shop.com", "password1")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").isNotEmpty());

		User user = userRepository.findByEmail("new@shop.com").orElseThrow();
		assertThat(user.getPassword()).isNotEqualTo("password1");
		assertThat(passwordEncoder.matches("password1", user.getPassword())).isTrue();
		assertThat(user.getRole()).isEqualTo(User.Role.USER);
	}

	@Test
	void 대소문자만_다른_이메일로_재가입하면_409() throws Exception {
		signup("dup@shop.com", "password1").andExpect(status().isCreated());

		signup("DUP@shop.com", "password1")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("이미 가입된 이메일입니다."));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"email\":\"not-an-email\",\"password\":\"password1\"}",
			"{\"email\":\"a@shop.com\",\"password\":\"short\"}",
			// 73자 (BCrypt 한도 72 초과)
			"{\"email\":\"a@shop.com\",\"password\":\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}",
			"{}"
	})
	void 회원가입_입력값이_잘못되면_400(String body) throws Exception {
		postJson("/api/auth/signup", body).andExpect(status().isBadRequest());
		assertThat(userRepository.count()).isZero();
	}

	// --- 로그인 ---

	@Test
	void 로그인은_이메일_대소문자를_구분하지_않음() throws Exception {
		signup("me@shop.com", "password1");

		login("ME@Shop.COM", "password1").andExpect(status().isOk());
	}

	@Test
	void 비밀번호가_틀리거나_없는_계정이면_401() throws Exception {
		signup("me@shop.com", "password1");

		login("me@shop.com", "wrong-password").andExpect(status().isUnauthorized());
		login("nobody@shop.com", "password1").andExpect(status().isUnauthorized());
	}

	// --- Access Token ---

	@Test
	void 보호된_API는_유효한_토큰으로만_접근() throws Exception {
		String accessToken = field(signup("me@shop.com", "password1"), "accessToken");

		mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken + "x"))
				.andExpect(status().isUnauthorized());
		// 404 = 인증 통과 (아직 컨트롤러 없음)
		mvc.perform(get("/api/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isNotFound());
		// USER 권한으로 관리자 API
		mvc.perform(get("/api/admin/x").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isForbidden());
	}

	// --- Refresh Token ---

	@Test
	void 리프레시토큰은_Redis에_해시키로_14일_TTL과_함께_저장() throws Exception {
		signup("me@shop.com", "password1");

		Set<String> keys = redis.keys("refresh:*");
		assertThat(keys).hasSize(1);
		String key = keys.iterator().next();
		assertThat(key).matches("refresh:[0-9a-f]{64}");
		assertThat(redis.getExpire(key)).isBetween(14L * 24 * 3600 - 60, 14L * 24 * 3600);
	}

	@Test
	void 리프레시하면_새_토큰쌍이_발급되고_이전_토큰은_재사용_불가() throws Exception {
		String oldRefresh = field(signup("me@shop.com", "password1"), "refreshToken");

		String newRefresh = field(refresh(oldRefresh).andExpect(status().isOk()), "refreshToken");

		assertThat(newRefresh).isNotEqualTo(oldRefresh);
		refresh(oldRefresh).andExpect(status().isUnauthorized());
		refresh(newRefresh).andExpect(status().isOk());
	}

	@Test
	void 로그아웃한_리프레시토큰은_사용_불가() throws Exception {
		String refreshToken = field(signup("me@shop.com", "password1"), "refreshToken");

		postJson("/api/auth/logout", "{\"refreshToken\":\"" + refreshToken + "\"}")
				.andExpect(status().isNoContent());

		refresh(refreshToken).andExpect(status().isUnauthorized());
		assertThat(redis.keys("refresh:*")).isEmpty();
	}

	// --- helpers ---

	private ResultActions signup(String email, String password) throws Exception {
		return postJson("/api/auth/signup", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
	}

	private ResultActions login(String email, String password) throws Exception {
		return postJson("/api/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return postJson("/api/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}");
	}

	private ResultActions postJson(String url, String body) throws Exception {
		return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static String field(ResultActions result, String name) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$." + name);
	}
}
