package com.devlab.shoppy.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	// 원 단위
	@Column(nullable = false)
	private int price;

	@Column(nullable = false)
	private int stock;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Product(String name, String description, int price, int stock) {
		update(name, description, price, stock);
		this.createdAt = LocalDateTime.now();
	}

	public void update(String name, String description, int price, int stock) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.stock = stock;
	}
}
