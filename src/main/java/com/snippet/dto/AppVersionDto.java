package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * GET /api/appversion 응답 — 앱 버전 정책 및 판정 결과.
 *
 * <p>버전 비교는 전부 서버에서 수행하고, 클라이언트는 boolean 두 개만 보고 동작한다.
 * (비교 로직이 iOS/Android 양쪽에 중복되면 판정이 갈리므로 서버를 단일 진실 원천으로 둔다)
 */
@Getter
@AllArgsConstructor
public class AppVersionDto {

    /** 요청된 플랫폼 (ios / android). 알 수 없으면 "unknown" */
    private String platform;

    /** 클라이언트가 보낸 자기 버전. 미전달 시 null */
    private String currentVersion;

    /** 이 버전 미만이면 사용 차단 */
    private String minVersion;

    /** 스토어에 올라간 최신 버전 */
    private String latestVersion;

    /** true면 앱을 차단하고 업데이트를 강제해야 한다 */
    private boolean updateRequired;

    /** true면 최신 버전이 있으나 강제는 아니다 (스킵 가능한 안내) */
    private boolean updateAvailable;

    /** 스토어 이동 URL */
    private String storeUrl;

    /** 사용자에게 보여줄 안내 문구. 미설정 시 null → 클라이언트 기본 문구 사용 */
    private String message;
}
