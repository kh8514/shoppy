package com.devlab.shoppy.product.dto;

import com.devlab.shoppy.product.entity.Product;

import java.time.LocalDateTime;

public record ProductResponse(Long id, String name, String description, int price, int stock, LocalDateTime createdAt) {
	public static ProductResponse from(Product p) {
		return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.getCreatedAt());
	}
}
