package com.dongbang.finance.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeeDetailIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    private Long userId;
    private Long membershipId;
    private Long organizationId;
    private Long feeItemId;

    @BeforeEach
    void setUp() {
        userId = jdbc.queryForObject("INSERT INTO users (status) VALUES ('ACTIVE') RETURNING user_id", Long.class);
        organizationId = jdbc.queryForObject("INSERT INTO organizations (name, slug) VALUES ('회비 테스트', ?) RETURNING organization_id",
                Long.class, UUID.randomUUID().toString());
        membershipId = jdbc.queryForObject("""
                INSERT INTO memberships (organization_id, user_id, member_name, student_number, generation, role)
                VALUES (?, ?, '조회 회원', '20260001', '1', 'MEMBER') RETURNING membership_id
                """, Long.class, organizationId, userId);
        Long otherMember = jdbc.queryForObject("""
                INSERT INTO memberships (organization_id, member_name, student_number, generation, role)
                VALUES (?, '다른 회원', '20260002', '1', 'MEMBER') RETURNING membership_id
                """, Long.class, organizationId);
        feeItemId = jdbc.queryForObject("""
                INSERT INTO fee_items (organization_id, title, due_date, bank_name, bank_account_number,
                                      account_holder, created_by_membership_id)
                VALUES (?, '정기 납부', '2026-09-10', '은행', '123', '동방', ?) RETURNING fee_item_id
                """, Long.class, organizationId, membershipId);
        Long categoryId = jdbc.queryForObject("""
                INSERT INTO fee_categories (fee_item_id, name, amount, display_order)
                VALUES (?, '기본', 40000, 0) RETURNING fee_category_id
                """, Long.class, feeItemId);
        jdbc.update("""
                INSERT INTO fee_targets (fee_item_id, fee_category_id, membership_id, amount_due, status)
                VALUES (?, ?, ?, 20000, 'UNPAID'), (?, ?, ?, 40000, 'PAID')
                """, feeItemId, categoryId, membershipId, feeItemId, categoryId, otherMember);
    }

    @Test
    void memberGetsAssignedAmountInsteadOfCategoryAmount() throws Exception {
        mvc.perform(get(path()).with(user(userId.toString()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewerType").value("MEMBER"))
                .andExpect(jsonPath("$.result.myPayment.amountDue").value(20000))
                .andExpect(jsonPath("$.result.staffSummary").value(nullValue()));
    }

    @Test
    void staffGetsCurrentPaidAndExpectedAmountsFromDatabase() throws Exception {
        jdbc.update("UPDATE memberships SET role = 'ADMIN' WHERE membership_id = ?", membershipId);
        mvc.perform(get(path()).with(user(userId.toString()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewerType").value("STAFF"))
                .andExpect(jsonPath("$.result.staffSummary.targetCount").value(2))
                .andExpect(jsonPath("$.result.staffSummary.paidCount").value(1))
                .andExpect(jsonPath("$.result.staffSummary.collectedAmount").value(40000))
                .andExpect(jsonPath("$.result.staffSummary.expectedAmount").value(60000))
                .andExpect(jsonPath("$.result.myPayment").value(nullValue()));
    }

    private String path() {
        return "/api/v1/organizations/" + organizationId + "/fee-items/" + feeItemId;
    }
}
