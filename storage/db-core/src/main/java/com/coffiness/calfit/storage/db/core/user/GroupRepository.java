package com.coffiness.calfit.storage.db.core.user;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

  List<GroupEntity> findByStatus(EntityStatus status);

  List<GroupEntity> findAllByIdInAndStatus(Collection<Long> ids, EntityStatus status);

  boolean existsByNameAndStatus(String name, EntityStatus status);
}
