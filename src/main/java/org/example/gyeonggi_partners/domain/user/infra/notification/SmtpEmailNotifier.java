package org.example.gyeonggi_partners.domain.user.infra.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gyeonggi_partners.domain.user.domain.notifier.EmailNotifier;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 이메일 발송 구현체
 * 실제 Gmail SMTP를 사용하여 이메일 발송
 */
@Component
@Profile({"local", "dev"})  // local 프로파일에서만 활성화 (테스트용)
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailNotifier implements EmailNotifier {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String email, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("[경기파트너스] 이메일 인증번호");
            message.setText(buildEmailContent(verificationCode));
            
            mailSender.send(message);
            log.info("이메일 발송 성공 - 수신자: {}", email);

            /**
             * 테스트용 로그 출력
             */
            log.info("발송된 인증번호 (테스트용): {}", verificationCode);

        } catch (Exception e) {
            log.error("이메일 발송 실패 - 수신자: {}, 에러: {}", email, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 본문 생성
     */
    private String buildEmailContent(String verificationCode) {
        return String.format("""
                경기파트너스 이메일 인증번호입니다.
                
                인증번호: %s
                
                인증번호는 5분간 유효합니다.
                본인이 요청하지 않은 경우 이 메일을 무시하세요.
                
                감사합니다.
                경기파트너스 팀
                """, verificationCode);
    }
}
