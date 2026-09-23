package com.dongbang.organization.application.facade;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.organization.exception.OrganizationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationAccessFacade {

    private final OrganizationRepository organizationRepository;

    public void requireActiveOrganization(Long organizationId) {
        organizationRepository.findById(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));
    }
}
