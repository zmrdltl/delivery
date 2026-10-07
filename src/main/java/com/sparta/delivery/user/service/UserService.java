package com.sparta.delivery.user.service;

import com.sparta.delivery.global.security.JwtUtil;
import com.sparta.delivery.user.dto.request.LoginRequest;
import com.sparta.delivery.user.dto.request.SignupRequest;
import com.sparta.delivery.user.dto.response.LoginResponse;
import com.sparta.delivery.user.dto.response.UserResponse;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signup(SignupRequest request) {

        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 아이디입니다."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getLoginId(),
                encodedPassword,
                request.getRole()
        );

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new UserResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 아이디입니다.",
                    exception
                );
            }

            throw exception;
        }
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
        }

        return false;
    }

    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "아이디 또는 비밀번호가 올바르지 않습니다."
            ));

        if (!passwordEncoder.matches(
            request.getPassword(), user.getPassword())) {
                throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "아이디 또는 비밀번호가 올바르지 않습니다."
                );
            }

        return new LoginResponse(jwtUtil.createToken(user));
    }
}
