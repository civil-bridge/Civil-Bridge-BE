package org.example.gyeonggi_partners.domain.user.domain.repository;

import org.example.gyeonggi_partners.domain.user.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * User 도메인 Repository 인터페이스
 * 도메인 계층에서 정의하고, 인프라 계층에서 구현
 */
public interface UserRepository {

    /**
     * 사용자 저장 (회원가입)
     * @param user 저장할 사용자
     * @return 저장된 사용자 (ID 포함)
     */
    User save(User user);

    /**
     * 로그인 ID 중복 확인
     * @param loginId 로그인 ID
     * @return 존재 여부
     */
    boolean existsByLoginId(String loginId);

    /**
     * 이메일 중복 확인
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 전화번호 중복 확인
     * @param phoneNumber 전화번호
     * @return 존재 여부
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 사용자 닉네임 중복 확인
     * @param userNickname 전화번호
     * @return 존재 여부
     */
    boolean existsByUserNickname(String userNickname);

    /**
     * 로그인 ID로 사용자를 조회합니다.
     * @param loginId 로그인 ID
     * @return Optional<User> (사용자가 없을 수도 있으므로)
     */
    Optional<User> findByLoginId(String loginId);


    Optional<User> findById(Long id);


    /**
     * 사용자 ID 목록으로 닉네임 목록을 조회합니다. (추가)
     * @param ids 사용자 ID 목록
     * @return 닉네임 목록
     */
    List<String> findNicknamesByIds(List<Long> ids);


}
