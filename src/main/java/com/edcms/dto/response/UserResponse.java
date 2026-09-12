package com.edcms.dto.response;

import com.edcms.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API response object for User data.
 *
 * <p>NOTE: passwordHash is deliberately excluded.
 * Never return password hashes (or any secrets) in API responses.
 */
@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
