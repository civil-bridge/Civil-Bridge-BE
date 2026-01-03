package org.example.gyeonggi_partners.domain.discussionRoom.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gyeonggi_partners.common.dto.ApiResponse;
import org.example.gyeonggi_partners.common.jwt.CustomUserDetails;
import org.example.gyeonggi_partners.domain.discussionRoom.api.dto.*;
import org.example.gyeonggi_partners.domain.discussionRoom.api.dto.DiscussionRoomListRes;
import org.example.gyeonggi_partners.domain.discussionRoom.application.DiscussionRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "논의방 API", description = "논의방 생성, 조회, 입장, 삭제 관련 API")
@RestController
@RequestMapping("/api/discussion-rooms")
@RequiredArgsConstructor
public class DiscussionRoomController {

    private final DiscussionRoomService discussionRoomService;

    /**
     * 논의방 생성
     * 
     * @param request 논의방 생성 요청 데이터
     * @param userDetails 현재 로그인한 사용자 정보 (Spring Security)
     * @return 생성된 논의방 정보 (입장 완료 상태)
     */
    @Operation(
            summary = "논의방 생성", 
            description = "새로운 논의방을 생성합니다. 생성자는 자동으로 방에 참여됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<JoinRoomRes>> createRoom(
            @Valid @RequestBody CreateDiscussionRoomReq request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        JoinRoomRes response = discussionRoomService.createRoom(
                request, 
                userDetails.getUserId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "논의방이 생성되었습니다."));
    }

    /**
     * 전체 논의방 목록 조회 (최신순)
     * 
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 15)
     * @return 논의방 목록 및 페이징 정보
     */
    @Operation(
            summary = "전체 논의방 목록 조회",
            description = "전체 논의방을 최신순으로 조회합니다. 페이징을 지원합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("retrieveTotal")
    public ResponseEntity<ApiResponse<DiscussionRoomListRes>> getTotalRooms(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            
            @Parameter(description = "페이지 크기", example = "15")
            @RequestParam(defaultValue = "15") int size
    ) {
        DiscussionRoomListRes response = discussionRoomService.retrieveTotalRooms(page, size);
        
        return ResponseEntity.ok(
                ApiResponse.success(response, "논의방 목록을 조회했습니다.")
        );
    }

    /**
     * 내가 참여한 논의방 목록 조회 (최신 참여순)
     * 
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기 (기본값: 15)
     * @param userDetails 현재 로그인한 사용자 정보 (Spring Security)
     * @return 논의방 목록 및 페이징 정보
     */
    @Operation(
            summary = "내가 참여한 논의방 목록 조회",
            description = "현재 사용자가 참여한 논의방을 최신 참여순으로 조회합니다. 페이징을 지원합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/retrieveMyJoined")
    public ResponseEntity<ApiResponse<DiscussionRoomListRes>> getMyJoinedRooms(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            
            @Parameter(description = "페이지 크기", example = "15")
            @RequestParam(defaultValue = "15") int size,
            
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DiscussionRoomListRes response = discussionRoomService.retrieveJoinedRooms(
                userDetails.getUserId(), 
                page, 
                size
        );
        
        return ResponseEntity.ok(
                ApiResponse.success(response, "참여한 논의방 목록을 조회했습니다.")
        );
    }

    @Operation(
            summary = "논의방 입장",
            description = "논의방에 입장합니다. 입장 시 방 정보와 멤버 목록을 제공합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<JoinRoomRes>> joinRoom(
            @Parameter(description = "논의방 ID", example = "1")
            @PathVariable Long roomId,
            
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        JoinRoomRes response = discussionRoomService.joinRoom(userDetails.getUserId(), roomId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "논의방에 입장했습니다."));
    }

    @Operation(
            summary = "논의방 나가기",
            description = "논의방에서 나갑니다. 마지막 멤버가 나가면 방이 삭제됩니다(Soft Delete).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @Parameter(description = "논의방 ID", example = "1")
            @PathVariable Long roomId,
            
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        discussionRoomService.leaveRoom(userDetails.getUserId(), roomId);
        
        return ResponseEntity.ok(
                ApiResponse.success(null, "논의방 나가기에 성공하였습니다.")
        );
    }
}
