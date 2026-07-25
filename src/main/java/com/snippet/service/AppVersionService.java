package com.snippet.service;

import com.snippet.dto.AppVersionDto;
import com.snippet.util.VersionComparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 앱 최소/최신 버전 정책 판정 (환경변수 기반).
 *
 * <p><b>fail-open 원칙</b>: 플랫폼을 모르거나, 클라이언트 버전이 없거나, 설정값이
 * 비어있거나 형식이 깨진 경우 전부 "업데이트 불필요"로 응답한다. 설정 오타 하나로
 * 전 사용자가 앱에서 잠기는 사고를 막기 위함이며, 강제 차단은 모든 값이 정상일 때만
 * 발생한다.
 *
 * <p>정책 변경은 서버 .env 수정 + 컨테이너 재생성으로 끝난다 (앱 재배포 불필요).
 */
@Slf4j
@Service
public class AppVersionService {

    private static final String PLATFORM_IOS = "ios";
    private static final String PLATFORM_ANDROID = "android";

    /** 어떤 버전도 이 값보다 작지 않으므로, 미설정 시 강제 업데이트가 발동하지 않는다. */
    private static final String DISABLED_VERSION = "0.0.0";

    @Value("${appversion.ios.min:}")
    private String iosMinVersion;

    @Value("${appversion.ios.latest:}")
    private String iosLatestVersion;

    @Value("${appversion.ios.storeurl:}")
    private String iosStoreUrl;

    @Value("${appversion.android.min:}")
    private String androidMinVersion;

    @Value("${appversion.android.latest:}")
    private String androidLatestVersion;

    @Value("${appversion.android.storeurl:}")
    private String androidStoreUrl;

    @Value("${appversion.message:}")
    private String updateMessage;

    /**
     * 클라이언트 버전을 정책과 대조해 판정한다.
     *
     * @param platform      "ios" 또는 "android" (대소문자 무시). 그 외는 fail-open
     * @param clientVersion 클라이언트 자기 버전 (예: "1.0.28"). null/비어있으면 fail-open
     */
    public AppVersionDto check(String platform, String clientVersion) {
        String normalized = platform == null ? "" : platform.trim().toLowerCase();

        String minVersion;
        String latestVersion;
        String storeUrl;

        switch (normalized) {
            case PLATFORM_IOS -> {
                minVersion = orDisabled(iosMinVersion);
                latestVersion = orDisabled(iosLatestVersion);
                storeUrl = trimToNull(iosStoreUrl);
            }
            case PLATFORM_ANDROID -> {
                minVersion = orDisabled(androidMinVersion);
                latestVersion = orDisabled(androidLatestVersion);
                storeUrl = trimToNull(androidStoreUrl);
            }
            default -> {
                log.warn("알 수 없는 platform으로 버전 조회: '{}' — fail-open 처리", platform);
                return new AppVersionDto(
                        "unknown", trimToNull(clientVersion), DISABLED_VERSION, DISABLED_VERSION,
                        false, false, null, null);
            }
        }

        String current = trimToNull(clientVersion);
        boolean updateRequired = false;
        boolean updateAvailable = false;

        if (!VersionComparator.isValid(current)) {
            // 클라이언트가 버전을 안 보냈거나 형식이 깨짐 → 차단하지 않는다
            log.debug("클라이언트 버전 미전달/무효 ({}) — fail-open", clientVersion);
        } else {
            if (VersionComparator.isValid(minVersion)) {
                updateRequired = VersionComparator.compare(current, minVersion) < 0;
            } else {
                log.warn("appversion.{}.min 값이 무효합니다: '{}' — 강제 업데이트 비활성", normalized, minVersion);
            }

            if (VersionComparator.isValid(latestVersion)) {
                updateAvailable = VersionComparator.compare(current, latestVersion) < 0;
            }
        }

        // 스토어 URL이 없으면 사용자를 보낼 곳이 없으므로 강제 차단하지 않는다
        if (updateRequired && storeUrl == null) {
            log.error("appversion.{}.storeurl 미설정 — 강제 업데이트를 무시합니다", normalized);
            updateRequired = false;
        }

        return new AppVersionDto(
                normalized,
                current,
                minVersion,
                latestVersion,
                updateRequired,
                updateAvailable,
                storeUrl,
                trimToNull(updateMessage));
    }

    private static String orDisabled(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? DISABLED_VERSION : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
