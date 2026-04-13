package com.snippet.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring Cache 설정 (Caffeine 기반 - TTL 지원)
 * - 통계 조회, 도서 정보 등 변경 빈도가 낮은 데이터 캐싱
 * - 프로덕션 환경에서는 Redis 등 분산 캐시 고려
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "monthlyStats",      // 월별 통계 캐시
            "yearlyStats",       // 연도별 통계 캐시
            "categoryStats",     // 카테고리별 통계 캐시
            "readingInsights",   // 독서 인사이트 캐시
            "bookDetails",       // 도서 상세 정보 캐시
            "popularBooks"       // 인기 대출 도서 캐시 (6시간 TTL)
        );
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(500)
        );
        return cacheManager;
    }
}
