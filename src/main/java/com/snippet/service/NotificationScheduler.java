package com.snippet.service;

import com.snippet.entity.UserBook;
import com.snippet.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);
    private final UserBookRepository userBookRepository;
    private final FcmService fcmService;

    // 매일 오전 9시 실행 — 반납 기한 알림
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendReturnDateReminders() {
        LocalDate today = LocalDate.now();

        // D-day 오늘, D-1, D-3 에 해당하는 책들에 알림
        int[] daysAhead = {0, 1, 3};
        for (int days : daysAhead) {
            LocalDate target = today.plusDays(days);
            LocalDateTime from = target.atStartOfDay();
            LocalDateTime to = target.plusDays(1).atStartOfDay();

            for (UserBook ub : userBookRepository.findBorrowedBooksWithReturnDateBetween(from, to)) {
                String token = ub.getUser().getFcmToken();
                String title;
                String body;
                if (days == 0) {
                    title = "📚 오늘 반납일이에요!";
                    body = String.format("「%s」 오늘 반납해야 합니다.", ub.getBook().getTitle());
                } else {
                    title = String.format("📚 반납일이 %d일 남았어요", days);
                    body = String.format("「%s」 %d일 후 반납 기한입니다.", ub.getBook().getTitle(), days);
                }
                fcmService.sendNotification(token, title, body);
            }
        }
        log.info("Return date notifications sent for {}", today);
    }
}
