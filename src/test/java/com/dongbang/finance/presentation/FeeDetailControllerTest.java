package com.dongbang.finance.presentation;

import com.dongbang.finance.application.FeeDetailQueryService;
import com.dongbang.finance.application.FeeDetailResult.*;
import com.dongbang.finance.application.FeeService;
import com.dongbang.finance.domain.FeeTargetStatus;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.global.config.SecurityConfig;
import com.dongbang.global.config.WebMvcConfig;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.exception.GeneralExceptionAdvice;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.global.security.ApiSecurityExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeeController.class)
@Import({SecurityConfig.class, ApiSecurityExceptionHandler.class, WebMvcConfig.class, GeneralExceptionAdvice.class})
class FeeDetailControllerTest {
    private static final String PATH = "/api/v1/organizations/1/fee-items/201";
    @Autowired MockMvc mvc;
    @MockitoBean FeeService fees;
    @MockitoBean FeeDetailQueryService details;

    @Test
    void staffResponseContainsSummaryAndExplicitNullPayment() throws Exception {
        when(details.detail(1L, 7L, 201L)).thenReturn(new FeeItemDetail(201L, FeeViewerType.STAFF,
                "정기 납부", LocalDate.of(2026, 9, 10), "설명",
                new FeeStaffSummary(2, 1, 1, new BigDecimal("40000"), new BigDecimal("60000")), null));
        mvc.perform(get(PATH).with(user("7").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewerType").value("STAFF"))
                .andExpect(jsonPath("$.result.staffSummary.expectedAmount").value(60000))
                .andExpect(jsonPath("$.result.myPayment").value(nullValue()))
                .andExpect(jsonPath("$.errorDetail").hasJsonPath());
    }

    @Test
    void memberResponseContainsOwnPaymentAndNoStaffSummary() throws Exception {
        when(details.detail(1L, 7L, 201L)).thenReturn(new FeeItemDetail(201L, FeeViewerType.MEMBER,
                "정기 납부", LocalDate.of(2026, 9, 10), null, null,
                new MyFeePayment(91L, new BigDecimal("20000"), FeeTargetStatus.UNPAID, null)));
        mvc.perform(get(PATH).with(user("7").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.myPayment.amountDue").value(20000))
                .andExpect(jsonPath("$.result.myPayment.status").value("UNPAID"))
                .andExpect(jsonPath("$.result.staffSummary").hasJsonPath())
                .andExpect(jsonPath("$.result.staffSummary").value(nullValue()));
    }

    @Test
    void nonTargetMemberReceivesNullPayment() throws Exception {
        when(details.detail(1L, 7L, 201L)).thenReturn(new FeeItemDetail(201L, FeeViewerType.MEMBER,
                "정기 납부", LocalDate.of(2026, 9, 10), null, null, null));
        mvc.perform(get(PATH).with(user("7").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.myPayment").hasJsonPath())
                .andExpect(jsonPath("$.result.myPayment").value(nullValue()));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_001"));
        verifyNoInteractions(details);
    }

    @Test
    void invalidIdIsRejectedBeforeQuery() throws Exception {
        mvc.perform(get("/api/v1/organizations/1/fee-items/0").with(user("7").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"));
        verifyNoInteractions(details);
    }

    @Test
    void forbiddenMembershipUsesCommonErrorResponse() throws Exception {
        when(details.detail(1L, 7L, 201L)).thenThrow(new GeneralException(GeneralErrorCode.FORBIDDEN));
        mvc.perform(get(PATH).with(user("7").roles("USER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_403_001"));
    }

    @Test
    void missingItemUsesFeeErrorResponse() throws Exception {
        when(details.detail(1L, 7L, 201L)).thenThrow(new GeneralException(FinanceErrorCode.FEE_ITEM_NOT_FOUND));
        mvc.perform(get(PATH).with(user("7").roles("USER")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("FEE_404_001"))
                .andExpect(jsonPath("$.result").hasJsonPath())
                .andExpect(jsonPath("$.errorDetail").value(nullValue()));
    }
}
