package org.example.gyeonggi_partners.common.jwt;

import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.domain.user.domain.model.User;
import org.example.gyeonggi_partners.domain.user.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)

    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // 로그인 ID로 DB에서 사용자 정보를 조회합니다.
        return userRepository.findByLoginId(loginId)
                .map(this::createUserDetails) // 사용자를 찾으면 UserDetails 객체로 변환
                .orElseThrow(() -> new UsernameNotFoundException(loginId + " -> 데이터베이스에서 찾을 수 없습니다."));
    }

    // DB의 User 모델을 Spring Security가 이해할 수 있는 UserDetails 객체로 변환합니다.
    private UserDetails createUserDetails(User user) {
        return user.toUserDetails();
    }
}