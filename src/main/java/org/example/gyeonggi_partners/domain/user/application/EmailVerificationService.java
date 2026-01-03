package org.example.gyeonggi_partners.domain.user.application;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.exception.BusinessException;
import org.example.gyeonggi_partners.domain.user.domain.notifier.EmailNotifier;
import org.example.gyeonggi_partners.domain.user.exception.UserErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 이메일 인증 서비스
 * Redis를 사용한 인증번호 관리
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRATION_MINUTES = 5;
    
    private final StringRedisTemplate redisTemplate;
    private final EmailNotifier emailNotifier;

    /**
     * 인증번호 생성 및 발송
     * @param email 이메일
     */
    public void sendVerificationCode(String email) {
        // 1. 6자리 랜덤 인증번호 생성
        String code = generateVerificationCode();
        
        // 2. Redis에 저장 (5분 TTL)
        String key = EMAIL_VERIFICATION_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(CODE_EXPIRATION_MINUTES));
        
        // 3. 이메일 발송 (인프라에 위임)
        emailNotifier.sendVerificationCode(email, code);
    }

    /**
     * 인증번호 검증
     * @param email 이메일
     * @param code 인증번호
     */
    public void verifyCode(String email, String code) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        // 인증번호 없거나 불일치
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
        }
        
        // 검증 성공 시 Redis에서 삭제
        redisTemplate.delete(key);
    }

    /**
     * 6자리 랜덤 숫자 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;  // 100000 ~ 999999
        return String.valueOf(code);
    }
}
