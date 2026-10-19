package com.devrlab.shoppy.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 서비스 로직에서 던지는 예외. ex) throw new BusinessException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.")
@Getter
public class BusinessException extends RuntimeException {
	private final HttpStatus status;

	public BusinessException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
}
