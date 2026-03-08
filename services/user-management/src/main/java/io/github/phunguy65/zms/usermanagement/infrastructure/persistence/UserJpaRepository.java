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

    @Query("SELECT u FROM UserJpaEntity u WHERE u.deletedAt IS NULL "
            + "AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) "
            + "AND (:provider IS NULL OR u.authProvider = :provider) "
            + "ORDER BY u.createdAt DESC")
    Slice<UserJpaEntity> findActiveFiltered(
            @Param("email") String email, @Param("provider") String provider, Pageable pageable);
}
