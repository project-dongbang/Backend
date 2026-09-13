package com.dongbang.organization.infrastructure.persistence;

import com.dongbang.organization.domain.Organization;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<Organization, Long>, OrganizationRepository {
    @Override
    Optional<Organization> findBySlug(String slug);

    @Override
    boolean existsBySlug(String slug);
}
