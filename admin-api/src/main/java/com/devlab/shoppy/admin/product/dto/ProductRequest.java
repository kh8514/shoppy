package com.devlab.shoppy.admin.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotNull @PositiveOrZero Integer price,
		@NotNull @PositiveOrZero Integer stock
) {
}
