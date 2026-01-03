package org.example.gyeonggi_partners.domain.user.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * User JPA Repository
 * Spring Data JPA 인터페이스
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 로그인 ID 존재 여부 확인
     */
    boolean existsByLoginId(String loginId);

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 전화번호 존재 여부 확인
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 전화번호 존재 여부 확인
     */
    boolean existsByNickname(String nickname);

    /**
     * 로그인 ID로 UserEntity 조회
     */
    Optional<UserEntity> findByLoginId(String loginId);

    Optional<UserEntity> findById(Long id);

    /**
     * ID 목록으로 닉네임만 조회 (추가)
     */
    @Query("SELECT u.nickname FROM UserEntity u WHERE u.id IN :ids")
    List<String> findNicknameByIdIn(@Param("ids") List<Long> ids);



}
