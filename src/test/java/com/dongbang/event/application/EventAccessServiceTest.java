package com.dongbang.event.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.exception.OrganizationErrorCode;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.organization.application.facade.OrganizationAccessFacade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventAccessServiceTest {

    @Test
    void nonMemberIsDeniedBeforeOrganizationLookup() {
        MembershipAccessFacade memberships = mock(MembershipAccessFacade.class);
        OrganizationAccessFacade organizations = mock(OrganizationAccessFacade.class);
        EventAccessService access = new EventAccessService(memberships, organizations);
        assertThatThrownBy(() -> access.requireMember(1L, 2L))
                .isInstanceOfSatisfying(GeneralException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(OrganizationErrorCode.MEMBER_REQUIRED));
        verifyNoInteractions(organizations);
    }

    @Test
    void ordinaryMemberCannotWrite() {
        MembershipAccessFacade memberships = mock(MembershipAccessFacade.class);
        OrganizationAccessFacade organizations = mock(OrganizationAccessFacade.class);
        EventAccessService access = new EventAccessService(memberships, organizations);
        assertThatThrownBy(() -> access.requireStaff(1L, 2L))
                .isInstanceOfSatisfying(GeneralException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(OrganizationErrorCode.STAFF_REQUIRED));
        verifyNoInteractions(organizations);
    }
}
