package com.snippet.util;

import org.springframework.web.util.HtmlUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 국립중앙도서관 서지 레코드의 문자열 필드를 앱이 쓰던 형식(알라딘 시절)으로 정리한다.
 *
 * <p>소장자료 검색 API는 실물 소장 레코드와 디지털화 레코드가 섞여 있고, 같은 필드라도 표기가
 * 제각각이다. 저자만 해도 "앤서니 버지스 지음;박시영 옮김", "지은이: 사이토 다카시 ;옮긴이: 정현",
 * "Burgess, Anthony 지은이 박시영 옮긴이", "임옥희[1956-] 지음"이 모두 온다.
 */
public final class NlRecordNormalizer {

    // book_tb 컬럼 길이 — 책 추가 시 검색 결과를 그대로 저장하므로 여기서 맞춘다
    static final int TITLE_MAX = 200;
    static final int AUTHOR_MAX = 200;
    static final int PUBLISHER_MAX = 100;

    private static final String COVER_BASE_URL = "https://cover.nl.go.kr/";
    private static final String MAIN_AUTHOR = "저자";

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern SPACES = Pattern.compile("[\\s\\u00A0]+");

    // 디지털화 레코드의 발행사항은 "서울 : 민음사, 2005"처럼 발행지·발행년이 붙어 온다
    private static final Pattern PUB_INFO_WITH_YEAR = Pattern.compile("^(.*?)\\s*,\\s*[\\[(]?(?:c|©)?\\d{4}.*$");
    private static final Pattern PLACE_PREFIX = Pattern.compile("^[^:]+?\\s*:\\s*");

    private static final Pattern YYYYMMDD = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})");
    private static final Pattern YEAR = Pattern.compile("(1[5-9]\\d{2}|2\\d{3})");

    // ISBN 뒤에 "(세트)" 같은 한정어가 붙을 수 있다
    private static final Pattern ISBN_WITH_QUALIFIER = Pattern.compile("([0-9][0-9-]{8,15}[0-9Xx])\\s*(\\([^)]*\\))?");

    private static final Pattern PAGE_COUNT = Pattern.compile("(\\d{1,5})\\s*(?:p\\b|p\\.|쪽|면|페이지)", Pattern.CASE_INSENSITIVE);

    private static final String ROLE_WORDS = String.join("|",
            "지은이", "지음", "저자", "저", "著者", "著", "원저자", "원저",
            "글·그림", "글과 그림", "글그림", "글",
            "옮긴이", "옮김", "역자", "역", "譯", "訳", "번역", "편역",
            "그린이", "그림", "엮은이", "엮음", "편저", "편집", "편",
            "원작", "감수", "사진", "기획", "해설");
    // "이름 역할" 쌍 — 디지털화 레코드는 한 덩어리에 여러 쌍이 이어 온다 ("A 지은이 B 옮긴이").
    // 역할 뒤에 마침표가 붙기도 한다 ("김진수 옮김.")
    private static final Pattern NAME_THEN_ROLE = Pattern.compile("(.+?)\\s+(" + ROLE_WORDS + ")(?=[\\s.,]|$)");
    // "역할: 이름"
    private static final Pattern ROLE_THEN_NAME = Pattern.compile("^([^:]{1,15}?)\\s*:\\s*(.*)$");
    // 생몰년 표기: [1964-], [1917-1993], []
    private static final Pattern LIFE_DATES = Pattern.compile("\\[\\s*\\d{0,4}\\s*-?\\s*\\d{0,4}\\s*]");
    private static final Pattern PUNCTUATION_ONLY = Pattern.compile("[\\p{Punct}\\s]+");
    // 쉼표 바로 뒤에 공백이 없을 때만 여러 사람으로 나눈다 ("Burgess, Anthony"는 한 사람)
    private static final Pattern NAME_LIST_SEPARATOR = Pattern.compile(",(?=\\S)");

    private static final Map<String, String> ROLE_ALIASES = Map.ofEntries(
            Map.entry("지은이", MAIN_AUTHOR), Map.entry("지음", MAIN_AUTHOR), Map.entry("저", MAIN_AUTHOR),
            Map.entry("著者", MAIN_AUTHOR), Map.entry("著", MAIN_AUTHOR),
            Map.entry("원저자", MAIN_AUTHOR), Map.entry("원저", MAIN_AUTHOR),
            Map.entry("글", MAIN_AUTHOR), Map.entry("글·그림", MAIN_AUTHOR),
            Map.entry("글과 그림", MAIN_AUTHOR), Map.entry("글그림", MAIN_AUTHOR),
            Map.entry("옮김", "옮긴이"), Map.entry("역자", "옮긴이"), Map.entry("역", "옮긴이"),
            Map.entry("譯", "옮긴이"), Map.entry("訳", "옮긴이"), Map.entry("번역", "옮긴이"),
            Map.entry("그린이", "그림"),
            Map.entry("엮음", "엮은이"), Map.entry("편저", "엮은이"), Map.entry("편집", "엮은이"),
            Map.entry("편", "엮은이"));

    private NlRecordNormalizer() {
    }

    /** 검색어 강조 태그(&lt;span class="searching_txt"&gt;)와 HTML 엔티티를 걷어낸다. */
    public static String cleanText(String raw) {
        if (raw == null) {
            return "";
        }
        String unescaped = HtmlUtils.htmlUnescape(TAGS.matcher(raw).replaceAll(""));
        return SPACES.matcher(unescaped).replaceAll(" ").trim();
    }

    /** 대등서명("= Post it")과 책임표시("/ 김영하 글·그림")는 빼고 부제목까지만 남긴다. */
    public static String title(String raw) {
        String title = cutAt(cleanText(raw), " = ");
        title = cutAt(title, " / ");
        return truncate(title, TITLE_MAX);
    }

    /**
     * "저자: 앤서니 버지스, 옮긴이: 박시영" — 알라딘 시절과 같은 형식.
     * 책 추가 시 서버가 첫 항목의 "저자:"를 떼어 저장하므로 저자를 항상 맨 앞에 둔다.
     */
    public static String author(String raw) {
        String text = LIFE_DATES.matcher(cleanText(raw)).replaceAll(" ").replace("[", "").replace("]", "");
        Map<String, List<String>> namesByRole = new LinkedHashMap<>();
        for (String segment : text.split(";")) {
            String part = SPACES.matcher(segment).replaceAll(" ").trim();
            if (part.isEmpty()) {
                continue;
            }
            if (part.contains(":")) {
                addRoleThenName(part, namesByRole);
            } else {
                addNameThenRole(part, namesByRole);
            }
        }

        List<String> roles = new ArrayList<>(namesByRole.keySet());
        roles.sort(Comparator.comparing(role -> !MAIN_AUTHOR.equals(role)));
        List<String> entries = new ArrayList<>();
        for (String role : roles) {
            entries.add(role + ": " + String.join(", ", namesByRole.get(role)));
        }
        return truncate(String.join(", ", entries), AUTHOR_MAX);
    }

    /** 디지털화 레코드의 "발행지 : 출판사, 발행년"에서 출판사만 남긴다. */
    public static String publisher(String raw) {
        String publisher = cleanText(raw);
        Matcher withYear = PUB_INFO_WITH_YEAR.matcher(publisher);
        if (withYear.matches()) {
            publisher = PLACE_PREFIX.matcher(withYear.group(1)).replaceFirst("").trim();
        }
        return truncate(publisher, PUBLISHER_MAX);
    }

    /** "20220427" → "2022-04-27", 연도만 있으면 "2005" (책 추가 시 ISBN 조회로 날짜를 보완한다). */
    public static String pubDate(String raw) {
        String text = raw == null ? "" : raw;
        Matcher full = YYYYMMDD.matcher(text);
        if (full.find()) {
            try {
                return LocalDate.of(Integer.parseInt(full.group(1)), Integer.parseInt(full.group(2)),
                        Integer.parseInt(full.group(3))).toString();
            } catch (DateTimeException ignored) {
                // "1979-99999999" 같은 미상 표기 — 연도로 내려간다
            }
        }
        Matcher year = YEAR.matcher(text);
        return year.find() ? year.group(1) : "";
    }

    /**
     * 국내 도서의 ISBN-13. 국외 도서·세트 ISBN뿐인 레코드·체크섬이 틀린 값은 null.
     * 알라딘 시절에도 외국도서는 검색에서 뺐다. ISBN-10은 13자리로 바꿔 기존 book_tb와 맞춘다.
     */
    public static String domesticIsbn13(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String chosen = null;
        Matcher matcher = ISBN_WITH_QUALIFIER.matcher(raw);
        while (matcher.find()) {
            String qualifier = matcher.group(2);
            if (qualifier != null && qualifier.contains("세트")) {
                continue;
            }
            String isbn13 = toIsbn13(matcher.group(1).replace("-", ""));
            // 여러 개면 "세트 ISBN 낱권 ISBN" 순서가 대부분이라 마지막 것을 쓴다
            if (isbn13 != null) {
                chosen = isbn13;
            }
        }
        if (chosen == null) {
            return null;
        }
        return chosen.startsWith("97889") || chosen.startsWith("97911") ? chosen : null;
    }

    /** 소장자료 검색의 imageUrl은 "kolis_on/2016/..._thumbnail.jpg" 같은 상대 경로다. */
    public static String coverUrl(String imageUrl) {
        String path = imageUrl == null ? "" : imageUrl.trim();
        if (path.isEmpty()) {
            return "";
        }
        if (path.startsWith("https://") || path.startsWith("http://")) {
            return path;
        }
        return COVER_BASE_URL + (path.startsWith("/") ? path.substring(1) : path);
    }

    /** ISBN 서지정보의 PAGE("302 p.", "152"). 전자책의 "1" 같은 값은 버린다. */
    public static Integer pageCount(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        Integer pages = null;
        Matcher matcher = PAGE_COUNT.matcher(text);
        while (matcher.find()) {
            pages = Integer.parseInt(matcher.group(1));
        }
        if (pages == null && text.matches("\\d{1,5}")) {
            pages = Integer.parseInt(text);
        }
        return pages != null && pages >= 10 && pages <= 20000 ? pages : null;
    }

    /** ISBN 서지정보의 "20041224" 형식 날짜. */
    public static LocalDate compactDate(String raw) {
        if (raw == null || !raw.trim().matches("\\d{8}")) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static String toIsbn13(String digits) {
        if (digits.matches("97[89]\\d{10}")) {
            return isbn13CheckDigit(digits.substring(0, 12)) == digits.charAt(12) ? digits : null;
        }
        if (digits.matches("\\d{9}[\\dXx]") && isValidIsbn10(digits)) {
            String body = "978" + digits.substring(0, 9);
            return body + isbn13CheckDigit(body);
        }
        return null;
    }

    private static char isbn13CheckDigit(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = first12.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (char) ('0' + (10 - sum % 10) % 10);
    }

    private static boolean isValidIsbn10(String isbn10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn10.charAt(i);
            int value = (i == 9 && (c == 'X' || c == 'x')) ? 10 : c - '0';
            sum += value * (10 - i);
        }
        return sum % 11 == 0;
    }

    private static void addRoleThenName(String part, Map<String, List<String>> namesByRole) {
        String role = MAIN_AUTHOR;
        for (String token : part.split(",")) {
            Matcher prefixed = ROLE_THEN_NAME.matcher(token.trim());
            if (prefixed.matches()) {
                role = canonicalRole(prefixed.group(1));
                addName(namesByRole, role, prefixed.group(2));
            } else {
                // "스토리: 김난영,박시연"의 두 번째 이름처럼 앞 역할에 이어 붙는다
                addName(namesByRole, role, token);
            }
        }
    }

    private static void addNameThenRole(String part, Map<String, List<String>> namesByRole) {
        Matcher matcher = NAME_THEN_ROLE.matcher(part);
        int consumed = 0;
        while (matcher.find()) {
            String role = canonicalRole(matcher.group(2));
            for (String name : NAME_LIST_SEPARATOR.split(matcher.group(1))) {
                addName(namesByRole, role, name);
            }
            consumed = matcher.end();
        }
        // 역할 표시가 없는 이름은 저자로 본다 ("김영하", "김이숙 옮김 Lyman, Haward F.")
        addName(namesByRole, MAIN_AUTHOR, part.substring(consumed));
    }

    private static void addName(Map<String, List<String>> namesByRole, String role, String rawName) {
        // 마침표는 이니셜("Haward F.")일 수 있어 남긴다
        String name = rawName.trim().replaceAll("^[\\s,:;]+|[\\s,:;]+$", "");
        if (name.isEmpty() || role.isEmpty() || PUNCTUATION_ONLY.matcher(name).matches()) {
            return;
        }
        List<String> names = namesByRole.computeIfAbsent(role, key -> new ArrayList<>());
        if (!names.contains(name)) {
            names.add(name);
        }
    }

    private static String canonicalRole(String role) {
        String trimmed = role.trim();
        return ROLE_ALIASES.getOrDefault(trimmed, trimmed);
    }

    private static String cutAt(String text, String separator) {
        int index = text.indexOf(separator);
        return index > 0 ? text.substring(0, index).trim() : text;
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max).trim();
    }
}
