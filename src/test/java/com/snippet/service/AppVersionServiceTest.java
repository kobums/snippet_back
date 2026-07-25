package com.snippet.service;

import com.snippet.dto.AppVersionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 강제 업데이트 판정 테스트.
 *
 * <p>설정 오타 하나로 전 사용자가 앱에서 잠기는 기능이므로, "차단되어야 하는 경우"보다
 * "절대 차단되면 안 되는 경우"(fail-open)를 더 촘촘히 검증한다.
 */
class AppVersionServiceTest {

    private AppVersionService service;

    @BeforeEach
    void setUp() {
        service = new AppVersionService();
        configure("1.0.28", "1.0.29", "https://apps.apple.com/kr/app/id6759643636",
                "1.0.23", "1.0.24", "https://play.google.com/store/apps/details?id=com.gowoobro.snippet",
                "");
    }

    private void configure(String iosMin, String iosLatest, String iosStore,
                           String androidMin, String androidLatest, String androidStore,
                           String message) {
        ReflectionTestUtils.setField(service, "iosMinVersion", iosMin);
        ReflectionTestUtils.setField(service, "iosLatestVersion", iosLatest);
        ReflectionTestUtils.setField(service, "iosStoreUrl", iosStore);
        ReflectionTestUtils.setField(service, "androidMinVersion", androidMin);
        ReflectionTestUtils.setField(service, "androidLatestVersion", androidLatest);
        ReflectionTestUtils.setField(service, "androidStoreUrl", androidStore);
        ReflectionTestUtils.setField(service, "updateMessage", message);
    }

    @Nested
    @DisplayName("차단해야 하는 경우")
    class Blocking {

        @Test
        @DisplayName("min 미만 iOS 클라이언트는 강제 업데이트")
        void belowMinIsBlocked() {
            AppVersionDto result = service.check("ios", "1.0.20");

            assertThat(result.isUpdateRequired()).isTrue();
            assertThat(result.isUpdateAvailable()).isTrue();
            assertThat(result.getStoreUrl()).contains("6759643636");
        }

        @Test
        @DisplayName("min 미만 Android 클라이언트는 강제 업데이트")
        void belowMinAndroidIsBlocked() {
            assertThat(service.check("android", "1.0.9").isUpdateRequired()).isTrue();
        }

        @Test
        @DisplayName("platform 대소문자는 무시한다")
        void platformIsCaseInsensitive() {
            assertThat(service.check("iOS", "1.0.20").isUpdateRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("차단하면 안 되는 경우 (fail-open)")
    class FailOpen {

        @Test
        @DisplayName("min과 같은 버전은 통과")
        void equalToMinPasses() {
            assertThat(service.check("ios", "1.0.28").isUpdateRequired()).isFalse();
        }

        @Test
        @DisplayName("min 초과 버전은 통과")
        void aboveMinPasses() {
            AppVersionDto result = service.check("ios", "1.0.29");
            assertThat(result.isUpdateRequired()).isFalse();
            assertThat(result.isUpdateAvailable()).isFalse();
        }

        @Test
        @DisplayName("클라이언트가 버전을 안 보내면 차단하지 않는다")
        void missingClientVersionPasses() {
            assertThat(service.check("ios", null).isUpdateRequired()).isFalse();
            assertThat(service.check("ios", "").isUpdateRequired()).isFalse();
        }

        @Test
        @DisplayName("클라이언트 버전 형식이 깨져도 차단하지 않는다")
        void malformedClientVersionPasses() {
            assertThat(service.check("ios", "unknown").isUpdateRequired()).isFalse();
        }

        @Test
        @DisplayName("알 수 없는 platform은 차단하지 않는다")
        void unknownPlatformPasses() {
            AppVersionDto result = service.check("windows", "0.0.1");
            assertThat(result.isUpdateRequired()).isFalse();
            assertThat(result.getPlatform()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("platform이 null이어도 차단하지 않는다")
        void nullPlatformPasses() {
            assertThat(service.check(null, "0.0.1").isUpdateRequired()).isFalse();
        }

        @Test
        @DisplayName("min 미설정(기본 상태)이면 어떤 버전도 차단하지 않는다")
        void unconfiguredMinPasses() {
            configure("", "", "https://apps.apple.com/kr/app/id6759643636",
                    "", "", "https://play.google.com", "");

            assertThat(service.check("ios", "0.0.1").isUpdateRequired()).isFalse();
        }

        @Test
        @DisplayName("min 값이 깨져 있으면 차단하지 않는다")
        void malformedMinPasses() {
            configure("최신", "", "https://apps.apple.com/kr/app/id6759643636",
                    "", "", "https://play.google.com", "");

            assertThat(service.check("ios", "1.0.1").isUpdateRequired()).isFalse();
        }

        @Test
        @DisplayName("스토어 URL이 없으면 보낼 곳이 없으므로 차단하지 않는다")
        void missingStoreUrlPasses() {
            configure("9.9.9", "9.9.9", "",
                    "9.9.9", "9.9.9", "", "");

            assertThat(service.check("ios", "1.0.28").isUpdateRequired()).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON 응답 계약")
    class Serialization {

        /**
         * iOS/Android가 읽는 키 이름을 고정한다. Lombok이 boolean 게터를 isXxx()로 만들기 때문에
         * 키가 "isUpdateRequired"로 나가면 두 클라이언트 모두 조용히 false로 읽어
         * 강제 업데이트가 영영 발동하지 않는다.
         */
        @Test
        @DisplayName("클라이언트가 기대하는 키로 직렬화된다")
        void serializesWithExpectedKeys() throws Exception {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(service.check("ios", "1.0.20"));

            assertThat(json)
                    .contains("\"platform\":")
                    .contains("\"currentVersion\":")
                    .contains("\"minVersion\":")
                    .contains("\"latestVersion\":")
                    .contains("\"updateRequired\":true")
                    .contains("\"updateAvailable\":true")
                    .contains("\"storeUrl\":");
            assertThat(json).doesNotContain("isUpdateRequired");
        }
    }

    @Nested
    @DisplayName("권장 업데이트")
    class SoftUpdate {

        @Test
        @DisplayName("min은 넘었지만 latest보다 낮으면 권장만")
        void betweenMinAndLatestIsSoft() {
            AppVersionDto result = service.check("ios", "1.0.28");

            assertThat(result.isUpdateRequired()).isFalse();
            assertThat(result.isUpdateAvailable()).isTrue();
        }

        @Test
        @DisplayName("설정된 안내 문구를 그대로 내려준다")
        void messageIsPassedThrough() {
            configure("1.0.28", "1.0.29", "https://apps.apple.com/kr/app/id6759643636",
                    "1.0.23", "1.0.24", "https://play.google.com", "보안 업데이트가 있습니다");

            assertThat(service.check("ios", "1.0.20").getMessage()).isEqualTo("보안 업데이트가 있습니다");
        }
    }
}
