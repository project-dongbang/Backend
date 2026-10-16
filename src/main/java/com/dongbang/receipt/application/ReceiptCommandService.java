package com.dongbang.receipt.application;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.organization.application.facade.MembershipAccessFacade;
import com.dongbang.receipt.exception.ReceiptErrorCode;
import com.dongbang.receipt.infrastructure.ReceiptOcrClient;
import com.dongbang.receipt.presentation.ReceiptOcrResponse;
import com.dongbang.receipt.presentation.ReceiptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptCommandService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Pattern TOTAL_AMOUNT = Pattern.compile(
            "(?:거래\\s*금액|합계|결제\\s*금액|카드\\s*금액|공급대가)\\s*[:：]?\\s*([0-9][0-9,]+)\\s*원?",
            Pattern.MULTILINE);
    private static final Pattern DATE = Pattern.compile("(20\\d{2})[./년\\s]+(\\d{1,2})[./월\\s]+(\\d{1,2})");
    private final ReceiptOcrClient receiptOcrClient;
    private final MembershipAccessFacade membershipAccessFacade;

    public ReceiptResponse create(Long organizationId, Long userId, MultipartFile file) {
        if (!membershipAccessFacade.isActiveMember(organizationId, userId)) throw new GeneralException(ReceiptErrorCode.MEMBER_REQUIRED);
        validate(file);
        ReceiptOcrResponse ocr = receiptOcrClient.recognize(file);
        Parsed parsed = parse(ocr.text());
        String merchant = ocr.merchant() != null ? ocr.merchant() : (ocr.storeName() != null ? ocr.storeName() : parsed.storeName());
        LocalDate spentOn = ocr.spentOn() != null ? ocr.spentOn() : (ocr.purchasedAt() != null ? ocr.purchasedAt() : parsed.date());
        BigDecimal amount = ocr.amount() != null ? ocr.amount() : (ocr.totalAmount() != null ? ocr.totalAmount() : parsed.amount());
        String itemTitle = ocr.itemTitle() != null ? ocr.itemTitle() : (merchant != null ? merchant + " 이용 건" : "영수증 지출");
        String category = ocr.category() != null ? ocr.category() : "기타";
        String paymentMethod = ocr.paymentMethod() != null ? ocr.paymentMethod() : "기타";
        var items = ocr.items() != null ? ocr.items() : java.util.List.<ReceiptOcrResponse.ReceiptItem>of();

        // Recognition is a preview. The ledger endpoint stores evidence only after the user submits it.
        return new ReceiptResponse(
                null, file.getOriginalFilename(), file.getContentType(), file.getSize(),
                null, null, itemTitle, spentOn, amount, merchant, category, paymentMethod,
                ocr.memo(), items, merchant, spentOn, amount, ocr.text(), null);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new GeneralException(ReceiptErrorCode.FILE_EMPTY);
        if (file.getSize() > MAX_FILE_SIZE) throw new GeneralException(ReceiptErrorCode.FILE_TOO_LARGE);
        String type = file.getContentType();
        if (type == null || !(type.equalsIgnoreCase("image/jpeg") || type.equalsIgnoreCase("image/png") || type.equalsIgnoreCase("image/webp")))
            throw new GeneralException(ReceiptErrorCode.INVALID_FILE_TYPE);
    }

    private Parsed parse(String text) {
        if (text == null) return new Parsed(null, null, null);
        Matcher amount = TOTAL_AMOUNT.matcher(text);
        BigDecimal value = null;
        while (amount.find()) {
            value = new BigDecimal(amount.group(1).replace(",", ""));
        }
        Matcher date = DATE.matcher(text);
        LocalDate purchasedAt = null;
        if (date.find()) {
            try {
                purchasedAt = LocalDate.of(Integer.parseInt(date.group(1)), Integer.parseInt(date.group(2)), Integer.parseInt(date.group(3)));
            } catch (DateTimeException ignored) {
                // An OCR typo in a date must not prevent the user from correcting the receipt.
            }
        }
        return new Parsed(null, purchasedAt, value);
    }

    private record Parsed(String storeName, LocalDate date, BigDecimal amount) {}
}
