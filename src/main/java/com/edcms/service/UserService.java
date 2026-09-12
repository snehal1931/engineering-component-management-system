package com.edcms.service;

import com.edcms.dto.request.CreateUserRequest;
import com.edcms.dto.response.UserResponse;
import com.edcms.entity.User;
import com.edcms.exception.DuplicateUserException;
import com.edcms.exception.UserNotFoundException;
import com.edcms.mapper.UserMapper;
import com.edcms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for all User management operations.
 *
 * <p><b>Layered Architecture:</b><br>
 * This service sits between the Controller layer (HTTP) and the Repository layer (DB).
 * All business logic lives HERE — controllers remain thin and repositories remain generic.
 *
 * <p><b>Dependency Injection:</b><br>
 * {@code @RequiredArgsConstructor} generates a constructor for all {@code final} fields.
 * Spring injects the dependencies at startup — this is Constructor Injection,
 * the recommended form in Spring Boot 3.x for testability and immutability.
 *
 * <p><b>@Transactional:</b><br>
 * Write operations are wrapped in transactions. If a service method throws an exception,
 * the transaction is rolled back. Read operations use readOnly=true for a performance hint.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Create a new user after validating uniqueness of username and email.
     *
     * <p>Password storage note: In a production system, passwords would be
     * hashed with BCrypt here. For this project, we store the plain password
     * in passwordHash for simplicity — easily replaced by adding Spring Security.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with username: '{}'", request.getUsername());

        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("username", request.getUsername());
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("email", request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(request.getPassword()) // TODO: BCrypt.hash(request.getPassword()) with Spring Security
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getId());

        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUserEntityById(id));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserEntityById(id);
        log.warn("Deleting user id: {} (username: '{}')", id, user.getUsername());
        userRepository.delete(user);
    }

    /**
     * Internal method to load a User entity — used by other services.
     * Returns the entity directly (not a DTO) because other services
     * need the entity for JPA relationship binding.
     *
     * <p>Package-visibility pattern: only services in this package should call this.
     * For larger projects, consider a dedicated "finder" or "loader" service.
     */
    @Transactional(readOnly = true)
    public User findUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
