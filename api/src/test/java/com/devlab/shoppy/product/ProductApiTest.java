package com.devlab.shoppy.product;

import com.devlab.shoppy.TestcontainersConfig;
import com.devlab.shoppy.product.entity.Product;
import com.devlab.shoppy.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ProductApiTest {

	@Autowired MockMvc mvc;
	@Autowired ProductRepository productRepository;

	@AfterEach
	void cleanUp() {
		productRepository.deleteAll();
	}

	@Test
	void 목록은_로그인없이_최신순_페이지로_조회() throws Exception {
		productRepository.saveAll(List.of(
				new Product("A", null, 1000, 1),
				new Product("B", null, 2000, 1),
				new Product("C", null, 3000, 1)));

		mvc.perform(get("/api/products?size=2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].name").value("C"))
				.andExpect(jsonPath("$.page.totalElements").value(3));
	}

	@Test
	void 상세는_로그인없이_조회_없으면_404() throws Exception {
		Long id = productRepository.save(new Product("A", "설명", 1000, 3)).getId();

		mvc.perform(get("/api/products/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("설명"));
		mvc.perform(get("/api/products/999999")).andExpect(status().isNotFound());
	}
}
