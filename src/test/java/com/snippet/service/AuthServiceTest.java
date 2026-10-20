package com.snippet.service;

import com.snippet.dto.auth.AuthDto;
import com.snippet.entity.User;
import com.snippet.exception.TooManyAttemptsException;
import com.snippet.repository.RefreshTokenRepository;
import com.snippet.repository.UserRepository;
import com.snippet.security.EmailCodeSendLimiter;
import com.snippet.security.JwtTokenProvider;
import com.snippet.security.LoginAttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 가입 여부가 응답으로 드러나지 않는지 + 인증코드 발송 제한 */
class AuthServiceTest {

    private UserRepository userRepository;
    private EmailService emailService;
    private EmailVerificationStore verificationStore;
    private AuthService authService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        emailService = mock(EmailService.class);
        verificationStore = new EmailVerificationStore();
        authService = new AuthService(userRepository, mock(RefreshTokenRepository.class), encoder,
                mock(JwtTokenProvider.class), emailService, verificationStore,
                new LoginAttemptLimiter(), new EmailCodeSendLimiter(), 2592000000L);
    }

    @Test
    void unknownEmailAndWrongPasswordFailIdentically() {
        User user = User.builder().email("a@b.c").password(encoder.encode("right")).name("n").build();
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("none@b.c")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new AuthDto.LoginRequest("a@b.c", "wrong"), "1.1.1.1"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
        assertThatThrownBy(() -> authService.login(new AuthDto.LoginRequest("none@b.c", "wrong"), "1.1.1.1"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    void emailCodeForRegisteredEmailSucceedsSilentlyAndSendsNotice() {
        when(userRepository.existsByEmail("a@b.c")).thenReturn(true);

        authService.sendEmailCode(new AuthDto.EmailCodeRequest("a@b.c"), "1.1.1.1");

        verify(emailService).sendAlreadyRegisteredNotice("a@b.c");
        verify(emailService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    void registerWithoutValidCodeDoesNotRevealExistingEmail() {
        when(userRepository.existsByEmail("a@b.c")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new AuthDto.RegisterRequest("a@b.c", "pw", "n", "123456")))
                .hasMessage("인증 코드가 올바르지 않거나 만료되었습니다.");
    }

    @Test
    void emailCodeResendWithinCooldownIs429() {
        when(userRepository.existsByEmail("new@b.c")).thenReturn(false);

        authService.sendEmailCode(new AuthDto.EmailCodeRequest("new@b.c"), "1.1.1.1");
        assertThatThrownBy(() -> authService.sendEmailCode(new AuthDto.EmailCodeRequest("new@b.c"), "1.1.1.1"))
                .isInstanceOf(TooManyAttemptsException.class);

        verify(emailService).sendVerificationCode(eq("new@b.c"), anyString());
    }
}
