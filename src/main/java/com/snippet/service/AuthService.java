package com.snippet.service;

import com.snippet.dto.auth.AuthDto;
import com.snippet.entity.RefreshToken;
import com.snippet.entity.User;
import com.snippet.repository.RefreshTokenRepository;
import com.snippet.repository.UserRepository;
import com.snippet.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final EmailVerificationStore verificationStore;
    private final long refreshTokenValidityTime;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailService emailService,
            EmailVerificationStore verificationStore,
            @Value("${jwt.refresh-token-validity-in-milliseconds:2592000000}") long refreshTokenValidityTime) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.verificationStore = verificationStore;
        this.refreshTokenValidityTime = refreshTokenValidityTime;
    }

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
        String refreshToken = issueRefreshToken(savedUser);

        return new AuthDto.AuthResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getName(), token, refreshToken);
    }

    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 만료된 세션 행 정리 (기회적 cleanup)
        refreshTokenRepository.deleteByUserAndExpireDateBefore(user, LocalDateTime.now());

        String token = jwtTokenProvider.createToken(user.getEmail());
        String refreshToken = issueRefreshToken(user);

        return new AuthDto.AuthResponse(user.getId(), user.getEmail(), user.getName(), token, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse getMe(String token) {
        String email = jwtTokenProvider.getUserEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        // 리프레시 토큰은 기기별로 발급되므로 조회 응답에 포함하지 않는다.
        return new AuthDto.AuthResponse(user.getId(), user.getEmail(), user.getName(), token, null);
    }

    /**
     * access token 재발급. 리프레시 토큰은 회전하지 않고 만료만 연장한다(sliding expiry).
     * 회전 방식은 멀티탭 동시 갱신·응답 유실 시 저장된 토큰과 어긋나 세션이 풀리는
     * 문제가 있었다. 토큰이 바뀌지 않으므로 어떤 순서로 갱신 요청이 와도 안전하다.
     */
    @Transactional
    public AuthDto.RefreshResponse refresh(AuthDto.RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.createToken(user.getEmail());
        refreshToken.extendExpiry(LocalDateTime.now().plusSeconds(refreshTokenValidityTime / 1000));

        return new AuthDto.RefreshResponse(newAccessToken, refreshToken.getToken());
    }

    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        // refreshtoken_tb는 FK ON DELETE CASCADE로 함께 삭제된다.
        userRepository.delete(user);
    }

    /** 기기(세션)별 리프레시 토큰 발급 — 불투명 랜덤 문자열, 만료는 DB로 관리 */
    private String issueRefreshToken(User user) {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(token)
                .expireDate(LocalDateTime.now().plusSeconds(refreshTokenValidityTime / 1000))
                .build());

        return token;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
