package com.devlab.shoppy.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}
