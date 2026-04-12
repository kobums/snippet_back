package com.snippet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Snippet] 이메일 인증 코드");
            helper.setText(buildEmailBody(code), true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    private String buildEmailBody(String code) {
        return """
                <div style="font-family: 'Apple SD Gothic Neo', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 24px; background: #ffffff;">
                    <h2 style="font-size: 24px; font-weight: 300; color: #1a1a1a; margin-bottom: 8px;">Snippet</h2>
                    <p style="font-size: 13px; color: #666; margin-bottom: 32px;">블라인드 북 큐레이션</p>
                    <p style="font-size: 15px; color: #1a1a1a; margin-bottom: 24px;">아래 인증 코드를 입력해 주세요.</p>
                    <div style="background: #f5f5f5; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;">
                        <span style="font-size: 36px; font-weight: 600; letter-spacing: 8px; color: #1a1a1a;">%s</span>
                    </div>
                    <p style="font-size: 13px; color: #999;">인증 코드는 10분간 유효합니다.</p>
                    <p style="font-size: 13px; color: #999;">본인이 요청하지 않은 경우 이 이메일을 무시하세요.</p>
                </div>
                """.formatted(code);
    }
}
