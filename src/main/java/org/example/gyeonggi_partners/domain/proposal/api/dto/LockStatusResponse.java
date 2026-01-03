package org.example.gyeonggi_partners.domain.proposal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LockStatusResponse {

    private boolean isLocked;
    private Long lockOwnerId;
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
