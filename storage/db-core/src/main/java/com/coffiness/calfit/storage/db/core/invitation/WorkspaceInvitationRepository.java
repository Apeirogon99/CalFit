package com.coffiness.calfit.storage.db.core.invitation;

import com.coffiness.calfit.core.enums.InvitationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInvitationRepository
    extends JpaRepository<WorkspaceInvitationEntity, Long> {

  Optional<WorkspaceInvitationEntity> findByToken(String token);

  boolean existsByWorkspaceIdAndEmailAndInvitationStatus(
      String workspaceId, String email, InvitationStatus invitationStatus);
}
