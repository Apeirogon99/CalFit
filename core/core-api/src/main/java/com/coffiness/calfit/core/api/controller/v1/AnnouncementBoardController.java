package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.AnnouncementBoardCreateRequest;
import com.coffiness.calfit.api.v1.request.AnnouncementBoardUpdateRequest;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardListResponse;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardResponse;
import com.coffiness.calfit.core.api.facade.announcementBoard.AnnouncementBoardFacade;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/announcement-boards")
public class AnnouncementBoardController {

  private final AnnouncementBoardFacade announcementBoardFacade;

  /* 공지사항 생성 */
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  public ApiResponse<AnnouncementBoardResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody AnnouncementBoardCreateRequest request) {
    Long userId = (user != null) ? user.userId() : null;
    AnnouncementBoard board = announcementBoardFacade.createAnnouncement(userId, request);
    return ApiResponse.success(AnnouncementBoardResponse.from(board));
  }

  /* 공지사항 조회 */
  @GetMapping
  public ApiResponse<List<AnnouncementBoardListResponse>> list() {
    List<AnnouncementBoard> result = announcementBoardFacade.getAnnouncementList();
    List<AnnouncementBoardListResponse> responses =
        result.stream().map(AnnouncementBoardListResponse::from).toList();

    return ApiResponse.success(responses);
  }

  @GetMapping("/{id}")
  public ApiResponse<AnnouncementBoardResponse> detail(@PathVariable Long id) {
    AnnouncementBoard board = announcementBoardFacade.getAnnouncement(id);
    return ApiResponse.success(AnnouncementBoardResponse.from(board));
  }

  /* 공지사항  수정 */
  @PutMapping("/{id}")
  public ApiResponse<AnnouncementBoardResponse> update(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long id,
      @Valid @RequestBody AnnouncementBoardUpdateRequest request) {
    Long userId = (user != null) ? user.userId() : null;
    AnnouncementBoard board = announcementBoardFacade.updateAnnouncement(userId, id, request);

    return ApiResponse.success(AnnouncementBoardResponse.from(board));
  }

  /* 공지사항 삭제 */
  @DeleteMapping("/{id}")
  public ApiResponse<?> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
    Long userId = (user != null) ? user.userId() : null;
    announcementBoardFacade.deleteAnnouncement(userId, id);

    return ApiResponse.success();
  }
}
