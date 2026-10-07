package com.sparta.delivery.user.service;

import com.sparta.delivery.user.dto.request.SignupRequest;
import com.sparta.delivery.user.dto.response.UserResponse;
import com.sparta.delivery.user.entity.User;
import com.sparta.delivery.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser);
    }
}
