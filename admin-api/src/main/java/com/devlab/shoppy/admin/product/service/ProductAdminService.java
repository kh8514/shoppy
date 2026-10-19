package com.devlab.shoppy.admin.product.service;

import com.devlab.shoppy.admin.product.dto.ProductRequest;
import com.devlab.shoppy.common.exception.BusinessException;
import com.devlab.shoppy.product.dto.ProductResponse;
import com.devlab.shoppy.product.entity.Product;
import com.devlab.shoppy.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAdminService {
	private final ProductRepository productRepository;

	public ProductResponse create(ProductRequest r) {
		return ProductResponse.from(productRepository.save(new Product(r.name(), r.description(), r.price(), r.stock())));
	}

	public ProductResponse update(Long id, ProductRequest r) {
		Product product = find(id);
		product.update(r.name(), r.description(), r.price(), r.stock());
		return ProductResponse.from(product);
	}

	// ponytail: 하드 삭제. 주문이 상품을 참조하게 되면 소프트 삭제(deleted_at)로 변경
	public void delete(Long id) {
		productRepository.delete(find(id));
	}

	private Product find(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
	}
}
