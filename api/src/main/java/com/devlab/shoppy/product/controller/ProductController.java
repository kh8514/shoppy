package com.devlab.shoppy.product.controller;

import com.devlab.shoppy.common.exception.BusinessException;
import com.devlab.shoppy.product.dto.ProductResponse;
import com.devlab.shoppy.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// 조회 전용이라 서비스 계층 없이 리포지토리 직접 사용
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
	private final ProductRepository productRepository;

	// ?page=0&size=20&sort=price,asc
	@GetMapping
	public PagedModel<ProductResponse> list(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return new PagedModel<>(productRepository.findAll(pageable).map(ProductResponse::from));
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable Long id) {
		return productRepository.findById(id)
				.map(ProductResponse::from)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
	}
}
