package com.edcms.mapper;

import com.edcms.dto.response.UserResponse;
import com.edcms.entity.User;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link User} entities and DTOs.
 *
 * <p><b>Why manual mappers instead of MapStruct?</b><br>
 * MapStruct is an excellent code-generation tool for larger projects.
 * For this project, manual mappers are used because:
 * <ol>
 *   <li>They are fully transparent — you can trace every field mapping in the interview.</li>
 *   <li>The Component mapper needs conditional logic (instanceof checks) that
 *       MapStruct can handle but adds annotation complexity.</li>
 *   <li>No annotation processor configuration is needed.</li>
 * </ol>
 *
 * <p><b>Alternative:</b> MapStruct with {@code @Mapper(componentModel = "spring")}.
 * If this project scaled, MapStruct would be the right choice.
 */
@Component
public class UserMapper {

    /**
     * Convert a User entity to a UserResponse DTO.
     * Deliberately excludes the passwordHash field.
     */
    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
