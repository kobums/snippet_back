package com.snippet.util;

/**
 * 점(.)으로 구분된 버전 문자열 비교 유틸 (예: "1.0.28").
 *
 * <p>강제 업데이트 판정의 유일한 비교 지점이므로, 파싱 실패는 예외를 던지지 않고
 * 호출부가 fail-open 할 수 있도록 {@link #isValid}로 사전 검증하는 방식을 쓴다.
 */
public final class VersionComparator {

    private VersionComparator() {
    }

    /**
     * 두 버전을 비교한다. a < b 이면 음수, 같으면 0, a > b 이면 양수.
     *
     * <p>자리수가 다르면 짧은 쪽을 0으로 채운다 ("1.0" == "1.0.0").
     * 숫자가 아닌 세그먼트는 0으로 취급한다 ("1.0.28-beta" 의 "28-beta" → 0이 아니라
     * 앞쪽 숫자만 취해 28로 파싱).
     */
    public static int compare(String a, String b) {
        String[] left = split(a);
        String[] right = split(b);
        int length = Math.max(left.length, right.length);

        for (int i = 0; i < length; i++) {
            int leftPart = i < left.length ? parseSegment(left[i]) : 0;
            int rightPart = i < right.length ? parseSegment(right[i]) : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    /**
     * 비교 가능한 버전 문자열인지 확인한다.
     * 최소 1개 이상의 숫자로 시작하는 세그먼트를 가져야 한다.
     */
    public static boolean isValid(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        String[] parts = split(version);
        if (parts.length == 0) {
            return false;
        }
        // 첫 세그먼트가 숫자로 시작해야 유효한 버전으로 본다 ("abc" 같은 값 차단)
        return parts[0].matches("^\\d+.*");
    }

    private static String[] split(String version) {
        if (version == null) {
            return new String[0];
        }
        return version.trim().split("\\.");
    }

    /** 세그먼트 앞부분의 연속된 숫자만 취한다 ("28-beta" → 28, "beta" → 0). */
    private static int parseSegment(String segment) {
        int end = 0;
        while (end < segment.length() && Character.isDigit(segment.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(segment.substring(0, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
