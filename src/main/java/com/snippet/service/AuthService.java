package com.snippet.service;

import com.snippet.dto.auth.AuthDto;
import com.snippet.entity.RefreshToken;
import com.snippet.entity.User;
import com.snippet.repository.RefreshTokenRepository;
import com.snippet.repository.UserRepository;
import com.snippet.exception.TooManyAttemptsException;
import com.snippet.security.EmailCodeSendLimiter;
import com.snippet.security.JwtTokenProvider;
import com.snippet.security.LoginAttemptLimiter;
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
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final EmailCodeSendLimiter emailCodeSendLimiter;
    private final long refreshTokenValidityTime;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 이메일 없음/비밀번호 불일치를 구분하지 않는다 (가입 여부 노출 방지) */
    static final String INVALID_CREDENTIALS_MESSAGE = "이메일 또는 비밀번호가 올바르지 않습니다.";

    /**
     * 존재하지 않는 이메일일 때도 BCrypt 비교를 한 번 수행해 응답 시간으로 가입 여부가 드러나지 않게 한다.
     * 임의 문자열의 해시라 어떤 입력과도 일치하지 않는다. 첫 로그인 실패 시 지연 생성.
     */
    private volatile String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailService emailService,
            EmailVerificationStore verificationStore,
            LoginAttemptLimiter loginAttemptLimiter,
            EmailCodeSendLimiter emailCodeSendLimiter,
            @Value("${jwt.refresh-token-validity-in-milliseconds:2592000000}") long refreshTokenValidityTime) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
        this.verificationStore = verificationStore;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.emailCodeSendLimiter = emailCodeSendLimiter;
        this.refreshTokenValidityTime = refreshTokenValidityTime;
    }

    /**
     * 가입 전 이메일로 인증코드 발송.
     * 이미 가입된 이메일이어도 같은 200 응답을 주고, 코드 대신 "이미 가입된 계정" 안내 메일을 보낸다.
     * (응답으로 가입 여부를 구분할 수 없게 하면서, 실제 메일 주인에게는 로그인하라고 알려준다)
     */
    public void sendEmailCode(AuthDto.EmailCodeRequest request, String clientIp) {
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        // 가입 여부 확인보다 먼저 제한해 두 경우의 응답이 같게 한다.
        EmailCodeSendLimiter.Rejection rejection = emailCodeSendLimiter.tryAcquire(email, clientIp);
        if (rejection != null) {
            throw new TooManyAttemptsException(rejection.message(), rejection.retryAfterSeconds());
        }
        if (userRepository.existsByEmail(email)) {
            emailService.sendAlreadyRegisteredNotice(email);
            return;
        }
        String code = generateCode();
        verificationStore.save(email, code, 10);
        emailService.sendVerificationCode(email, code);
    }

    /** 인증코드 검증 후 회원가입 */
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        // 코드 검증을 먼저 한다: 가입된 이메일엔 코드가 발급되지 않으므로 여기서 걸러지고,
        // 코드 없이 register를 호출해 가입 여부를 알아내는 경로가 막힌다.
        if (request.getEmail() == null || request.getCode() == null
                || !verificationStore.verify(request.getEmail(), request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        // 코드 발급 후 다른 경로로 먼저 가입된 경우(경합) 대비. 이 시점엔 메일함 소유가 증명됐으므로 노출 문제 없음.
        if (userRepository.existsByEmail(request.getEmail())) {
            verificationStore.remove(request.getEmail());
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
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
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request, String clientIp) {
        String email = request.getEmail();
        // 무차별 대입 방지: 이메일+IP 조합으로 15분 내 5회 실패 시 15분 잠금
        if (loginAttemptLimiter.isLocked(email, clientIp)) {
            throw new TooManyAttemptsException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        User user = email == null ? null : userRepository.findByEmail(email).orElse(null);
        String rawPassword = request.getPassword() == null ? "" : request.getPassword();
        // 이메일이 없어도 BCrypt 비교를 수행해 두 실패 경로의 응답 시간·상태·메시지를 같게 한다.
        boolean matches;
        if (user != null) {
            matches = passwordEncoder.matches(rawPassword, user.getPassword());
        } else {
            passwordEncoder.matches(rawPassword, dummyPasswordHash()); // 결과는 버린다 (시간 맞추기용)
            matches = false;
        }
        if (!matches) {
            loginAttemptLimiter.recordFailure(email, clientIp);
            throw new IllegalArgumentException(INVALID_CREDENTIALS_MESSAGE);
        }
        loginAttemptLimiter.recordSuccess(email, clientIp);

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

    private String dummyPasswordHash() {
        String hash = dummyPasswordHash;
        if (hash == null) {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            hash = passwordEncoder.encode(Base64.getEncoder().encodeToString(bytes));
            dummyPasswordHash = hash;
        }
        return hash;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
