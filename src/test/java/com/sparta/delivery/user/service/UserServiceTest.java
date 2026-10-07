package com.sparta.delivery.user.service;

import com.sparta.delivery.global.security.JwtUtil;
import com.sparta.delivery.user.dto.request.LoginRequest;
import com.sparta.delivery.user.dto.request.SignupRequest;
import com.sparta.delivery.user.dto.response.LoginResponse;
import com.sparta.delivery.user.dto.response.UserResponse;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.Optional;

import static com.sparta.delivery.support.ServiceFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtUtil);
    }

    @ParameterizedTest
    @EnumSource(User.Role.class)
    void signupStoresEncodedPasswordAndSelectedRole(User.Role role) {
        SignupRequest request = fields(
            new SignupRequest(),
            "loginId", "testuser",
            "password", "password123",
            "role", role
        );
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation ->
            fields(invocation.getArgument(0), "id", 1L)
        );

        UserResponse response = userService.signup(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertNotEquals(request.getPassword(), saved.getValue().getPassword());
        assertTrue(passwordEncoder.matches(
            request.getPassword(), saved.getValue().getPassword()
        ));
        assertEquals("testuser", response.getLoginId());
        assertEquals(role, response.getRole());
        assertEquals(1L, response.getId());
    }

    @Test
    void duplicateSignupDoesNotSaveAnotherUser() {
        SignupRequest request = fields(
            new SignupRequest(), "loginId", "duplicate"
        );
        when(userRepository.existsByLoginId("duplicate")).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> userService.signup(request));

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void databaseUniqueConflictAfterPrecheckReturnsConflict() {
        SignupRequest request = signupRequest();
        DataIntegrityViolationException conflict = new DataIntegrityViolationException(
            "unique constraint violation",
            new SQLException("duplicate login ID", "23505")
        );
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(conflict);

        assertStatus(HttpStatus.CONFLICT, () -> userService.signup(request));

        verify(userRepository).existsByLoginId("testuser");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void unrelatedDatabaseIntegrityFailureIsNotReportedAsDuplicateSignup() {
        SignupRequest request = signupRequest();
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
            "not-null constraint violation",
            new SQLException("required column missing", "23502")
        );
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(failure);

        assertSame(failure, assertThrows(
            DataIntegrityViolationException.class,
            () -> userService.signup(request)
        ));
    }

    @Test
    void loginReturnsTokenForMatchingCredentials() {
        User user = new User(
            "testuser", passwordEncoder.encode("password123"), User.Role.CUSTOMER
        );
        LoginRequest request = loginRequest("testuser", "password123");
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
        when(jwtUtil.createToken(user)).thenReturn("test-token");

        LoginResponse response = userService.login(request);

        assertEquals("test-token", response.getToken());
    }

    @Test
    void unknownLoginDoesNotIssueToken() {
        LoginRequest request = loginRequest("missing", "password123");
        when(userRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        assertStatus(HttpStatus.UNAUTHORIZED, () -> userService.login(request));

        verifyNoInteractions(jwtUtil);
    }

    @Test
    void wrongPasswordDoesNotIssueToken() {
        User user = new User(
            "testuser", passwordEncoder.encode("password123"), User.Role.CUSTOMER
        );
        LoginRequest request = loginRequest("testuser", "wrong-password");
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));

        assertStatus(HttpStatus.UNAUTHORIZED, () -> userService.login(request));

        verifyNoInteractions(jwtUtil);
    }

    private SignupRequest signupRequest() {
        return fields(
            new SignupRequest(), "loginId", "testuser",
            "password", "password123", "role", User.Role.CUSTOMER
        );
    }

    private LoginRequest loginRequest(String loginId, String password) {
        return fields(new LoginRequest(), "loginId", loginId, "password", password);
    }
}
