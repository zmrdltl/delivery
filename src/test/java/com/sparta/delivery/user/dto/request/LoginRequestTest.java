package com.sparta.delivery.user.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static com.sparta.delivery.support.ServiceFixtures.fields;
import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @MethodSource("passwordsAtByteLimit")
    void acceptsAsciiKoreanAndMixedPasswordsAt72Bytes(String password) {
        assertTrue(validator.validate(request(password)).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("passwordsOverByteLimit")
    void rejectsPasswordsOver72Utf8Bytes(String password) {
        var violations = validator.validate(request(password));

        assertEquals(1, violations.size());
        assertEquals(
            "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.",
            violations.iterator().next().getMessage()
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "")
    void missingPasswordStillUsesRequiredValidation(String password) {
        var violations = validator.validate(request(password));

        assertEquals(1, violations.size());
        assertEquals("password", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void nonblankShortPasswordCanReachCredentialVerification() {
        assertTrue(validator.validate(request("wrong")).isEmpty());
    }

    private static Stream<String> passwordsAtByteLimit() {
        return Stream.of("a".repeat(72), "가".repeat(24), "가".repeat(23) + "abc");
    }

    private static Stream<String> passwordsOverByteLimit() {
        return Stream.of("a".repeat(73), "가".repeat(25), "가".repeat(24) + "a");
    }

    private LoginRequest request(String password) {
        return fields(new LoginRequest(), "loginId", "testuser", "password", password);
    }
}
