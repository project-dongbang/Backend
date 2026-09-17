package com.dongbang.photo.infrastructure.persistence;

import com.dongbang.photo.domain.UploadedFile;
import com.dongbang.photo.domain.repository.UploadedFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileJpaRepository extends JpaRepository<UploadedFile, Long>, UploadedFileRepository {

    @Override
    Optional<UploadedFile> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);

    @Override
    List<UploadedFile> findAllByIdInAndOrganizationIdAndDeletedAtIsNull(Collection<Long> ids, Long organizationId);
}
