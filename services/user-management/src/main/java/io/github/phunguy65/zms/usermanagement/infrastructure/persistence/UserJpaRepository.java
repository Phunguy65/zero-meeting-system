package io.github.phunguy65.zms.usermanagement.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    Optional<UserJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<UserJpaEntity> findByGoogleUidAndDeletedAtIsNull(String googleUid);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    Optional<UserJpaEntity> findByUsernameAndDeletedAtIsNull(String username);

    @Query(
            value = "SELECT * FROM users u WHERE u.deleted_at IS NULL "
                    + "AND (CAST(:email AS text) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS text), '%'))) "
                    + "AND (CAST(:provider AS text) IS NULL OR u.auth_provider = CAST(:provider AS text)) "
                    + "ORDER BY u.created_at DESC",
            nativeQuery = true)
    Slice<UserJpaEntity> findActiveFiltered(
            @Param("email") String email, @Param("provider") String provider, Pageable pageable);
}
