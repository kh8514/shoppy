package com.devrlab.shoppy.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public User(String email, String encodedPassword) {
		this.email = email;
		this.password = encodedPassword;
		this.role = Role.USER;
		this.createdAt = LocalDateTime.now();
	}

	public enum Role { USER, ADMIN }
}
