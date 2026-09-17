package com.dongbang.photo.infrastructure.persistence;

import com.dongbang.photo.domain.Photo;
import com.dongbang.photo.domain.repository.PhotoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoJpaRepository extends JpaRepository<Photo, Long>, PhotoRepository {

    @Override
    Optional<Photo> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);

    @Override
    @Query("""
        SELECT p FROM Photo p
        WHERE p.organizationId = :organizationId
          AND (:cursor IS NULL OR p.id < :cursor)
          AND p.deletedAt IS NULL
        ORDER BY p.id DESC
    """)
    List<Photo> findPhotosByCursor(
            @Param("organizationId") Long organizationId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
