package org.example.gyeonggi_partners.domain.user.domain.notifier;

/**
 * 이메일 발송 인터페이스
 * 도메인 계층에서 정의하고, 인프라 계층에서 구현
 */
public interface EmailNotifier {

    /**
     * 이메일 인증번호 발송
     * @param email 수신자 이메일
     * @param verificationCode 인증번호
     */
    void sendVerificationCode(String email, String verificationCode);
}
