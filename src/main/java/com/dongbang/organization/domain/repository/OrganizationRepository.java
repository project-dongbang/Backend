package com.dongbang.organization.domain.repository;

import com.dongbang.organization.domain.Organization;

import java.util.Optional;

public interface OrganizationRepository {
    Organization save(Organization organization);
    Optional<Organization> findById(Long id);
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
