package org.example.gyeonggi_partners.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    // === 클라이언트에게 전달할 정보 ===
    private Long userId;
    private String nickname;
    private String email;
    private String role;

    // === Spring Security 내부용 (클라이언트에게 안 줌) ===
    private String loginId;          // getUsername()에 사용
    private String password;         // getPassword()에 사용 (비밀번호 검증용)
    private Collection<? extends GrantedAuthority> authorities;

    // ... 생성자

    // UserDetails 구현
    @Override
    public String getUsername() {
        return loginId;  // Spring Security가 사용
    }

    @Override
    public String getPassword() {
        return password;  //  Spring Security가 사용 (비밀번호 검증)
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}