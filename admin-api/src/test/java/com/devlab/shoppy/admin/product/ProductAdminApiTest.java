package com.devlab.shoppy.admin.product;

import com.devlab.shoppy.TestcontainersConfig;
import com.devlab.shoppy.product.entity.Product;
import com.devlab.shoppy.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ProductAdminApiTest {

	private static final String BODY = "{\"name\":\"티셔츠\",\"description\":\"면 100%\",\"price\":19000,\"stock\":5}";

	@Autowired MockMvc mvc;
	@Autowired ProductRepository productRepository;

	@AfterEach
	void cleanUp() {
		productRepository.deleteAll();
	}

	@Test
	void 관리자는_상품을_등록_수정_삭제() throws Exception {
		mvc.perform(asAdmin(post("/api/admin/products"), BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.price").value(19000));
		Long id = productRepository.findAll().get(0).getId();

		mvc.perform(asAdmin(put("/api/admin/products/" + id), "{\"name\":\"후드티\",\"price\":39000,\"stock\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("후드티"));
		Product updated = productRepository.findById(id).orElseThrow();
		assertThat(updated.getPrice()).isEqualTo(39000);
		assertThat(updated.getStock()).isZero();

		mvc.perform(delete("/api/admin/products/" + id).with(admin())).andExpect(status().isNoContent());
		assertThat(productRepository.count()).isZero();
	}

	@Test
	void 가격_누락_음수_이름_공백이면_400() throws Exception {
		for (String body : new String[]{
				"{\"name\":\"a\",\"stock\":1}",
				"{\"name\":\"a\",\"price\":-1,\"stock\":1}",
				"{\"name\":\" \",\"price\":1,\"stock\":1}"}) {
			mvc.perform(asAdmin(post("/api/admin/products"), body)).andExpect(status().isBadRequest());
		}
		assertThat(productRepository.count()).isZero();
	}

	@Test
	void 없는_상품_수정_삭제는_404() throws Exception {
		mvc.perform(asAdmin(put("/api/admin/products/999"), BODY)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/admin/products/999").with(admin())).andExpect(status().isNotFound());
	}

	@Test
	void 토큰없으면_401_USER는_403() throws Exception {
		mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(BODY)
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isForbidden());
		assertThat(productRepository.count()).isZero();
	}

	private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder req, String body) {
		return req.with(admin()).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}
}
