package de.terrestris.shogun.lib.repository.security.permission;

import de.terrestris.shogun.lib.model.security.permission.UserInstancePermission;
import de.terrestris.shogun.lib.repository.BaseCrudRepository;
import java.util.List;
import java.util.Optional;
import javax.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInstancePermissionRepository extends BaseCrudRepository<UserInstancePermission, Long>, JpaSpecificationExecutor<UserInstancePermission> {

    @Query("Select uip from userinstancepermissions uip where uip.user.id = ?1 and uip.entityId = ?2")
    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    Optional<UserInstancePermission> findByUserIdAndEntityId(Long userId, Long entityId);

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    List<UserInstancePermission> findByEntityId(Long entityId);

}
