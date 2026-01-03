package org.example.gyeonggi_partners.domain.discussionRoom.api.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.AccessLevel;
import org.example.gyeonggi_partners.domain.discussionRoom.domain.model.Region;

@Getter
@Schema(description = "논의방 생성 요청")
@NoArgsConstructor
public class CreateDiscussionRoomReq {

    @NotBlank(message = "방 제목은 필수입니다.")
    @Size(max = 100)
    @Schema(description = "논의방 제목", example = "부천시 BJ로 인한 지역 상권문제")
    String title;

    @Size(max = 255)
    @Schema(description = "논의방 설명", example = "현재 부천시 BJ로 인한 .... 등의 내용설명")
    String description;

    @NotNull
    @Schema(description = "문제발생 지역", example = "POCHEON")
    Region region;


    @NotNull
    @Schema(description = "방 입장조건", example = "PUBLIC")
    AccessLevel accessLevel;


}
