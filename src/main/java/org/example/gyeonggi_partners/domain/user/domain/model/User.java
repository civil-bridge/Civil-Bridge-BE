package org.example.gyeonggi_partners.domain.user.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.example.gyeonggi_partners.common.jwt.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * User 도메인 모델
 * JPA와 독립적인 순수 도메인 객체
 */
@Getter
public class User {

    //로그인 아이디 규칙
    private static final int MAX_LOGIN_ID_LENGTH = 30;

    //이름 규칙
    private static final int MAX_NAME_LENGTH = 50;

    //닉네임 규칙
    private static final int MAX_NICKNAME_LENGTH = 50;

    //이메일 규칙
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,30}$");
    private static final int MAX_EMAIL_LENGTH = 50;

    //전화번호 규칙
    // 010: 010-XXXX-XXXX (4자리-4자리)
    // 011~019: 01X-XXX-XXXX (3자리-4자리)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^010-\\d{4}-\\d{4}$");
    private static final int MAX_PHONE_NUMBER_LENGTH = 20;


    private Long id;
    private String loginId;        // 로그인 ID
    private String loginPw;        // 암호화된 비밀번호
    private String name;           // 실명
    private String nickname;       // 닉네임
    private String email;          // 이메일
    private String phoneNumber;    // 전화번호
    private String role;           // USER, ADMIN 등
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private User(Long id, String loginId, String loginPw, String name,
                 String nickname, String email, String phoneNumber, String role,
                 LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.loginId = loginId;
        this.loginPw = loginPw;
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role != null ? role : "USER";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /**
     * 새로운 사용자 생성 (회원가입)
     */
    public static User create(String loginId, String loginPw, String name,
                              String nickname, String email, String phoneNumber) {
        validateLoginId(loginId);
        validatePassword(loginPw);
        validateName(name);
        validateNickname(nickname);
        validateEmail(email);
        validatePhoneNumber(phoneNumber);

        return User.builder()
                .loginId(loginId)
                .loginPw(loginPw)  // Service에서 암호화해서 넘겨줘야 함
                .name(name)
                .nickname(nickname)
                .email(email)
                .phoneNumber(phoneNumber)
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 기존 사용자 복원 (DB에서 조회)
     */
    public static User restore(Long id, String loginId, String loginPw, String name,
                               String nickname, String email, String phoneNumber, String role,
                               LocalDateTime createdAt, LocalDateTime updatedAt, 
                               LocalDateTime deletedAt) {
        return User.builder()
                .id(id)
                .loginId(loginId)
                .loginPw(loginPw)
                .name(name)
                .nickname(nickname)
                .email(email)
                .phoneNumber(phoneNumber)
                .role(role)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }


    /**
     * User 도메인 모델을 Spring Security의 UserDetails 객체로 변환합니다.
     */
    public UserDetails toUserDetails() {
        Collection<GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role));

        return new CustomUserDetails(
                this.id,           // userId
                this.nickname,     // nickname (순서 변경!)
                this.email,        // email (순서 변경!)
                this.role,         // role (순서 변경!)
                this.loginId,      // loginId (순서 변경!)
                this.loginPw,      // role
                authorities        // authorities
        );
    }


    // ==================== Validation Methods ====================

    private static void validateLoginId(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("로그인 ID는 필수입니다.");
        }
        if (loginId.length() > MAX_LOGIN_ID_LENGTH) {
            throw new IllegalArgumentException(
                String.format("로그인 ID는 %d자를 초과할 수 없습니다.", MAX_LOGIN_ID_LENGTH));
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        // 평문 비밀번호 검증은 UserService에서 수행
        // 여기서는 암호화된 비밀번호가 들어오므로 길이 검증 불필요
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                String.format("이름은 %d자를 초과할 수 없습니다.", MAX_NAME_LENGTH));
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException(
                String.format("닉네임은 %d자를 초과할 수 없습니다.", MAX_NICKNAME_LENGTH));
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException(
                String.format("이메일은 %d자를 초과할 수 없습니다.", MAX_EMAIL_LENGTH));
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("전화번호는 필수입니다.");
        }
        if (phoneNumber.length() > MAX_PHONE_NUMBER_LENGTH) {
            throw new IllegalArgumentException(
                String.format("전화번호는 %d자를 초과할 수 없습니다.", MAX_PHONE_NUMBER_LENGTH));
        }
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("올바른 휴대폰 번호 형식이 아닙니다.");
        }
    }
}
