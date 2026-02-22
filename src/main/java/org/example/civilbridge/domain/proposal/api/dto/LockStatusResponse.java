package org.example.civilbridge.domain.proposal.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "제안서 잠금 상태 응답")
public class LockStatusResponse {

    @Schema(description = "잠금 여부", example = "true")
    private boolean isLocked;

    @Schema(description = "잠금 소유자 ID", example = "1")
    private Long lockOwnerId;

    @Schema(description = "잠금 소유자 닉네임", example = "홍길동")
    private String lockOwnerNickname;

    public static LockStatusResponse unlocked() {
        return LockStatusResponse.builder()
                .isLocked(false)
                .build();
    }

    public static LockStatusResponse locked(Long ownerId, String ownerNickname) {
        return LockStatusResponse.builder()
                .isLocked(true)
                .lockOwnerId(ownerId)
                .lockOwnerNickname(ownerNickname)
                .build();
    }
}
