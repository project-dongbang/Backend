package com.dongbang.photo.domain.repository;

import com.dongbang.photo.domain.Photo;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository {
    Photo save(Photo photo);
    Optional<Photo> findById(Long id);
    Optional<Photo> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);
    List<Photo> findPhotosByCursor(Long organizationId, Long cursor, Pageable pageable);
}
