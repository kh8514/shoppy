package com.devlab.shoppy.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// password 최대 72: BCrypt 가 72바이트 초과분을 처리하지 못함
public record SignupRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) String password
) {
}
