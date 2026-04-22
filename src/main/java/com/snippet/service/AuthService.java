package com.snippet.service;

import com.snippet.dto.auth.AuthDto;
import com.snippet.entity.User;
import com.snippet.repository.UserRepository;
import com.snippet.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final EmailVerificationStore verificationStore;

    /** 가입 전 이메일로 인증코드 발송 */
    public void sendEmailCode(AuthDto.EmailCodeRequest request) {
        String email = request.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        String code = generateCode();
        verificationStore.save(email, code, 10);
        emailService.sendVerificationCode(email, code);
    }

    /** 인증코드 검증 후 회원가입 */
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (request.getCode() == null || !verificationStore.verify(request.getEmail(), request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        User savedUser = userRepository.save(user);
        verificationStore.remove(request.getEmail());

        String token = jwtTokenProvider.createToken(savedUser.getEmail());
        return new AuthDto.AuthResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getName(), token);
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.createToken(user.getEmail());
        return new AuthDto.AuthResponse(user.getId(), user.getEmail(), user.getName(), token);
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse getMe(String token) {
        String email = jwtTokenProvider.getUserEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return new AuthDto.AuthResponse(user.getId(), user.getEmail(), user.getName(), token);
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        userRepository.delete(user);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
