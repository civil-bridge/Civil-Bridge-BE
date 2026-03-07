package org.example.civilbridge.domain.discussionRoom.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 논의방 멤버 정보 응답 DTO
 */
@Getter
@Builder
@Schema(description = "논의방 멤버 정보")
public class MemberInfo {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "길동이")
    private String nickname;

    @Schema(description = "논의방 내 역할 (LEADER: 방장, PARTICIPANT: 일반 참여자)", example = "LEADER")
    private String role;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true)
    private String profileImageUrl;
}
