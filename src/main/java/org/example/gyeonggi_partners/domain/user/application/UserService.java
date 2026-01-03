package org.example.gyeonggi_partners.domain.user.application;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.exception.BusinessException;
import org.example.gyeonggi_partners.domain.user.api.dto.SignUpRequest;
import org.example.gyeonggi_partners.domain.user.api.dto.SignUpResponse;
import org.example.gyeonggi_partners.domain.user.domain.model.User;
import org.example.gyeonggi_partners.domain.user.domain.repository.UserRepository;
import org.example.gyeonggi_partners.domain.user.exception.UserErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User 애플리케이션 서비스
 * 회원가입 등 유즈케이스 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    /**
     * 이메일 인증번호 발송
     * @param email 이메일
     */
    public void sendEmailVerification(String email) {
        emailVerificationService.sendVerificationCode(email);
    }

    /**
     * 이메일 인증번호 검증
     * @param email 이메일
     * @param code 인증번호
     */
    public void verifyEmail(String email, String code) {
        emailVerificationService.verifyCode(email, code);
    }

    /**
     * 회원가입
     * @param req 회원가입 요청 DTO
     * @return 회원가입 응답 DTO
     */
    @Transactional
    public SignUpResponse signUp(SignUpRequest req) {
        // 1. 중복 검증
        validateDuplicateLoginId(req.getLoginId());
        validateDuplicateEmail(req.getEmail());
        validateDuplicatePhoneNumber(req.getPhoneNumber());
        validateDuplicateUserNickname(req.getNickname());

        // 2. 평문 비밀번호 검증 (암호화 전)
        validatePlainPassword(req.getPassword());

        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(req.getPassword());

        // 4. User 도메인 생성 (도메인 validation 수행)
        User user = User.create(
                req.getLoginId(),
                encodedPassword,
                req.getName(),
                req.getNickname(),
                req.getEmail(),
                req.getPhoneNumber()
        );

        // 5. 저장
        User savedUser = userRepository.save(user);

        // 6. 응답 DTO 생성
        return new SignUpResponse(
                savedUser.getId(),
                savedUser.getNickname(),
                savedUser.getEmail(),
                savedUser.getRole() // Enum 타입이라면 .name() 등으로 String 변환
        );
    }

    /**
     * 로그인 ID 중복 검증
     */
    private void validateDuplicateLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    /**
     * 이메일 중복 검증
     */
    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * 전화번호 중복 검증
     */
    private void validateDuplicatePhoneNumber(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_PHONE_NUMBER);
        }
    }

    /**
     * 이메일 중복 검증
     */
    private void validateDuplicateUserNickname(String nickname) {
        if (userRepository.existsByUserNickname(nickname)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_USER_NICKNAME);
        }
    }
    /**
     * 평문 비밀번호 검증 (암호화 전)
     */
    private void validatePlainPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
        if (password.length() > 50) {
            throw new IllegalArgumentException("비밀번호는 50자를 초과할 수 없습니다.");
        }
    }
}
