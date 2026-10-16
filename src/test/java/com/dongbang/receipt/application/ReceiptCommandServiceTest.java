package com.dongbang.receipt.application;

import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.receipt.infrastructure.ReceiptOcrClient;
import com.dongbang.receipt.presentation.ReceiptOcrResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReceiptCommandServiceTest {
    @Mock private ReceiptOcrClient receiptOcrClient;
    @Mock private MembershipAccessFacade membershipAccessFacade;

    @Test
    void unknownPaymentAndInvalidOcrDateRemainEditable() {
        var file = new MockMultipartFile("file", "receipt.png", "image/png", new byte[] {1, 2});
        given(membershipAccessFacade.isActiveMember(1L, 2L)).willReturn(true);
        given(receiptOcrClient.recognize(file)).willReturn(new ReceiptOcrResponse(
                "거래일 2026.99.99", null, null, null, null, null,
                null, null, null, null, null, List.of(), List.of()));

        var response = new ReceiptCommandService(receiptOcrClient, membershipAccessFacade).create(1L, 2L, file);

        assertThat(response.paymentMethod()).isEqualTo("기타");
        assertThat(response.spentOn()).isNull();
        assertThat(response.receiptId()).isNull();
        assertThat(response.fileUrl()).isNull();
    }
}
