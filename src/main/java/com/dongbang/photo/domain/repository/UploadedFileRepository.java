package com.dongbang.photo.domain.repository;

import com.dongbang.photo.domain.UploadedFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UploadedFileRepository {
    UploadedFile save(UploadedFile uploadedFile);
    Optional<UploadedFile> findById(Long id);
    Optional<UploadedFile> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);
    List<UploadedFile> findAllByIdInAndOrganizationIdAndDeletedAtIsNull(Collection<Long> ids, Long organizationId);
}
