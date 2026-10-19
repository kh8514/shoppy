package com.devrlab.shoppy.auth.dto;

public record TokenResponse(String accessToken, String refreshToken) {
}
