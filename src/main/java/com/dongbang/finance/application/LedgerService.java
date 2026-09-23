package com.dongbang.finance.application;

import com.dongbang.finance.domain.*;
import com.dongbang.finance.domain.repository.*;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.finance.presentation.dto.FinanceDtos.*;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.global.response.code.FileErrorCode;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.photo.domain.UploadedFile;
import com.dongbang.photo.domain.repository.UploadedFileRepository;
import com.dongbang.photo.infrastructure.storage.FileStorageResult;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.infrastructure.storage.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {
    private static final long EVIDENCE_MAX_BYTES = 3L * 1024 * 1024;
    private final FinancialTransactionRepository transactionRepository;
    private final FeeItemRepository feeItemRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final AuditLogRepository auditLogRepository;
    private final MembershipRepository membershipRepository;
    private final FinanceAccessService accessService;
    private final FileStorageService fileStorageService;

    public CursorResult<LedgerItem> getLedger(Long organizationId, Long userId, LocalDate from, LocalDate to,
                                               TransactionType type, String category, String cursor, int size) {
        accessService.requireMember(organizationId, userId);
        validatePeriod(from, to);
        List<FinancialTransaction> all = transactionRepository
                .findAllByOrganizationIdAndStatusOrderByOccurredOnDescIdDesc(organizationId, TransactionStatus.POSTED);
        LedgerSummary summary = summary(all, from, to);
        List<LedgerItem> entries = buildEntries(all).stream()
                .filter(item -> from == null || !item.occurredOn().isBefore(from))
                .filter(item -> to == null || !item.occurredOn().isAfter(to))
                .filter(item -> type == null || item.transactionType() == type)
                .filter(item -> category == null || category.isBlank() || item.category().equals(category))
                .sorted(Comparator.comparing(LedgerItem::occurredOn).reversed()
                        .thenComparing(item -> Objects.toString(item.transactionId(), Objects.toString(item.feeItemId(), "0")), Comparator.reverseOrder()))
                .toList();
        int offset = decodeOffset(cursor);
        List<LedgerItem> content = entries.stream().skip(offset).limit(size).toList();
        boolean hasNext = offset + content.size() < entries.size();
        return new CursorResult<>(summary, content, size, hasNext ? encodeOffset(offset + content.size()) : null, hasNext);
    }

    @Transactional
    public TransactionResult createExpense(Long organizationId, Long userId, ExpenseRequest request, MultipartFile evidence) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        validateEvidence(evidence);
        FileStorageResult stored = fileStorageService.store(evidence, organizationId, "ledger-evidence");
        try {
            rejectDuplicateEvidence(organizationId, stored.checksum());
            UploadedFile file = uploadedFileRepository.save(UploadedFile.builder()
                    .organizationId(organizationId).uploadedByMembershipId(actor.getId())
                    .storageKey(stored.storageKey()).originalName(stored.originalName())
                    .contentType(stored.contentType()).sizeBytes(stored.sizeBytes()).checksum(stored.checksum()).build());
            FinancialTransaction transaction = transactionRepository.save(new FinancialTransaction(organizationId, null, null,
                    TransactionType.EXPENSE, request.title().trim(), request.category().trim(), request.amount(), request.occurredOn(),
                    request.counterparty().trim(), request.paymentMethod().trim(), trim(request.memo()), file.getId(), actor.getId()));
            transactionRepository.flush();
            return transactionResult(transaction, balance(organizationId));
        } catch (RuntimeException ex) {
            fileStorageService.delete(stored.storageKey());
            throw ex;
        }
    }

    @Transactional
    public TransactionResult createIncome(Long organizationId, Long userId, IncomeRequest request) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        FinancialTransaction transaction = transactionRepository.save(new FinancialTransaction(organizationId, null, null,
                TransactionType.INCOME, request.title().trim(), "입금 내역", request.amount(), request.occurredOn(),
                request.counterparty().trim(), null, trim(request.memo()), null, actor.getId()));
        return transactionResult(transaction, balance(organizationId));
    }

    public TransactionDetail getDetail(Long organizationId, Long userId, Long transactionId) {
        Membership viewer = accessService.requireMember(organizationId, userId);
        FinancialTransaction transaction = findTransaction(organizationId, transactionId);
        if (transaction.getStatus() == TransactionStatus.VOID && !viewer.getRole().isStaff()) {
            throw new GeneralException(FinanceErrorCode.TRANSACTION_NOT_FOUND);
        }
        Membership creator = membershipRepository.findById(transaction.getCreatedByMembershipId()).orElse(null);
        Evidence evidence = null;
        if (transaction.getEvidenceFileId() != null && transaction.getStatus() == TransactionStatus.POSTED) {
            UploadedFile file = uploadedFileRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(
                    transaction.getEvidenceFileId(), organizationId).orElse(null);
            if (file != null) evidence = evidence(file);
        }
        return new TransactionDetail(transaction.getId(), transaction.getFeeItemId(), transaction.getTransactionType(),
                transaction.getTitle(), transaction.getCategory(), transaction.getAmount(), transaction.getOccurredOn(),
                transaction.getVendor(), transaction.getPaymentMethod(), transaction.getMemo(), transaction.getStatus(),
                transaction.getVoidReason(), transaction.getVoidedByMembershipId(), transaction.getVoidedAt(),
                new CreatedBy(transaction.getCreatedByMembershipId(), creator == null ? "알 수 없음" : creator.getMemberName()),
                evidence, transaction.getCreatedAt());
    }

    @Transactional
    public TransactionResult updateIncome(Long organizationId, Long userId, Long transactionId, UpdateIncomeRequest request) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        if (request.isEmpty()) throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        FinancialTransaction transaction = requireType(organizationId, transactionId, TransactionType.INCOME);
        ensurePosted(transaction);
        if (transaction.getFeeTargetId() != null) throw new GeneralException(FinanceErrorCode.AUTO_INCOME);
        String before = snapshot(transaction);
        transaction.update(trim(request.title()), request.amount(), request.occurredOn(), trim(request.counterparty()),
                null, null, trim(request.memo()), request.memoPresent(), null);
        auditLogRepository.save(new AuditLog(organizationId, actor.getId(), "UPDATE", "FINANCIAL_TRANSACTION",
                transaction.getId(), before, snapshot(transaction)));
        return transactionResult(transaction, balance(organizationId));
    }

    @Transactional
    public TransactionResult updateExpense(Long organizationId, Long userId, Long transactionId,
                                            UpdateExpenseRequest request, MultipartFile evidence) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        if ((request == null || request.isEmpty()) && (evidence == null || evidence.isEmpty()))
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        FinancialTransaction transaction = requireType(organizationId, transactionId, TransactionType.EXPENSE);
        ensurePosted(transaction);
        String before = snapshot(transaction);
        FileStorageResult stored = null;
        UploadedFile oldFile = null;
        try {
            Long evidenceId = null;
            if (evidence != null && !evidence.isEmpty()) {
                validateEvidence(evidence);
                stored = fileStorageService.store(evidence, organizationId, "ledger-evidence");
                rejectDuplicateEvidence(organizationId, stored.checksum());
                UploadedFile newFile = uploadedFileRepository.save(UploadedFile.builder()
                        .organizationId(organizationId).uploadedByMembershipId(actor.getId())
                        .storageKey(stored.storageKey()).originalName(stored.originalName())
                        .contentType(stored.contentType()).sizeBytes(stored.sizeBytes()).checksum(stored.checksum()).build());
                evidenceId = newFile.getId();
                oldFile = transaction.getEvidenceFileId() == null ? null : uploadedFileRepository.findById(transaction.getEvidenceFileId()).orElse(null);
            }
            if (request != null) {
                transaction.update(trim(request.title()), request.amount(), request.occurredOn(), trim(request.counterparty()),
                        trim(request.category()), trim(request.paymentMethod()), trim(request.memo()), request.memoPresent(), evidenceId);
            } else if (evidenceId != null) {
                transaction.update(null, null, null, null, null, null, null, false, evidenceId);
            }
            transactionRepository.flush();
            auditLogRepository.save(new AuditLog(organizationId, actor.getId(), "UPDATE", "FINANCIAL_TRANSACTION",
                    transaction.getId(), before, snapshot(transaction)));
            if (oldFile != null) {
                oldFile.delete();
                deleteAfterCommit(oldFile.getStorageKey());
            }
            return transactionResult(transaction, balance(organizationId));
        } catch (RuntimeException ex) {
            if (stored != null) fileStorageService.delete(stored.storageKey());
            throw ex;
        }
    }

    @Transactional
    public DeletedExpenseResult deleteExpense(Long organizationId, Long userId, Long transactionId) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        FinancialTransaction transaction = requireType(organizationId, transactionId, TransactionType.EXPENSE);
        if (transaction.getStatus() == TransactionStatus.VOID) {
            return new DeletedExpenseResult(transaction.getId(), transaction.getStatus(), transaction.getVoidedByMembershipId(),
                    transaction.getVoidedAt(), balance(organizationId));
        }
        Instant now = Instant.now();
        transaction.voidTransaction("운영진 삭제", actor.getId(), now);
        auditLogRepository.save(new AuditLog(organizationId, actor.getId(), "VOID", "FINANCIAL_TRANSACTION",
                transaction.getId(), snapshot(transaction), null));
        return new DeletedExpenseResult(transaction.getId(), transaction.getStatus(), actor.getId(), now, balance(organizationId));
    }

    public byte[] exportLedger(Long organizationId, Long userId, LocalDate from, LocalDate to,
                               TransactionType type, String category, String format) {
        CursorResult<LedgerItem> result = getLedger(organizationId, userId, from, to, type, category, null, 100000);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("구분", "항목", "분류", "금액", "일자", "거래처", "상태", "증빙"));
        result.content().forEach(item -> rows.add(List.of(item.transactionType().name(), item.title(), item.category(),
                item.amount().toPlainString(), item.occurredOn().toString(), Objects.toString(item.counterparty(), ""),
                item.status().name(), item.hasEvidence() ? "있음" : "없음")));
        return "csv".equalsIgnoreCase(format) ? SpreadsheetExport.csv(rows) : SpreadsheetExport.xlsx(rows);
    }

    private List<LedgerItem> buildEntries(List<FinancialTransaction> all) {
        Map<Long, List<FinancialTransaction>> feeIncome = all.stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.INCOME && tx.getFeeItemId() != null && tx.getFeeTargetId() != null)
                .collect(Collectors.groupingBy(FinancialTransaction::getFeeItemId));
        Set<Long> aggregatedIds = feeIncome.values().stream().flatMap(Collection::stream).map(FinancialTransaction::getId).collect(Collectors.toSet());
        Map<Long, Membership> creators = membershipRepository.findAllByIdIn(all.stream().map(FinancialTransaction::getCreatedByMembershipId).toList())
                .stream().collect(Collectors.toMap(Membership::getId, Function.identity()));
        List<LedgerItem> entries = all.stream().filter(tx -> !aggregatedIds.contains(tx.getId())).map(tx -> new LedgerItem(
                "TRANSACTION", tx.getId(), tx.getFeeItemId(), tx.getTransactionType(), tx.getTitle(), tx.getCategory(), tx.getAmount(),
                tx.getOccurredOn(), tx.getVendor(), Optional.ofNullable(creators.get(tx.getCreatedByMembershipId())).map(Membership::getMemberName).orElse("알 수 없음"),
                tx.getStatus(), tx.getEvidenceFileId() != null, null)).collect(Collectors.toCollection(ArrayList::new));
        Map<Long, FeeItem> items = feeItemRepository.findAllById(feeIncome.keySet()).stream().collect(Collectors.toMap(FeeItem::getId, Function.identity()));
        feeIncome.forEach((feeItemId, transactions) -> {
            FeeItem item = items.get(feeItemId);
            if (item == null) return;
            BigDecimal amount = transactions.stream().map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate occurred = transactions.stream().map(FinancialTransaction::getOccurredOn).max(LocalDate::compareTo).orElse(item.getDueDate());
            entries.add(new LedgerItem("FEE_INCOME_SUMMARY", null, feeItemId, TransactionType.INCOME, item.getTitle(), "납부 수입",
                    amount, occurred, "회원 납부", "시스템", TransactionStatus.POSTED, false, (long) transactions.size()));
        });
        return entries;
    }

    private LedgerSummary summary(List<FinancialTransaction> all, LocalDate from, LocalDate to) {
        BigDecimal income = all.stream().filter(tx -> tx.getTransactionType() == TransactionType.INCOME).map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = all.stream().filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE).map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FinancialTransaction> periodExpenses = all.stream().filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE)
                .filter(tx -> from == null || !tx.getOccurredOn().isBefore(from)).filter(tx -> to == null || !tx.getOccurredOn().isAfter(to)).toList();
        BigDecimal periodAmount = periodExpenses.stream().map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LedgerSummary(income.subtract(expense), income, expense, new LedgerPeriod(from, to, periodAmount, periodExpenses.size()));
    }

    private BigDecimal balance(Long organizationId) {
        List<FinancialTransaction> all = transactionRepository.findAllByOrganizationIdAndStatusOrderByOccurredOnDescIdDesc(organizationId, TransactionStatus.POSTED);
        return summary(all, null, null).balance();
    }
    private FinancialTransaction findTransaction(Long orgId, Long id) { return transactionRepository.findByIdAndOrganizationId(id, orgId).orElseThrow(() -> new GeneralException(FinanceErrorCode.TRANSACTION_NOT_FOUND)); }
    private FinancialTransaction requireType(Long orgId, Long id, TransactionType type) {
        FinancialTransaction transaction = findTransaction(orgId, id);
        if (transaction.getTransactionType() != type) throw new GeneralException(FinanceErrorCode.TRANSACTION_NOT_FOUND);
        return transaction;
    }
    private void ensurePosted(FinancialTransaction tx) { if (tx.getStatus() != TransactionStatus.POSTED) throw new GeneralException(FinanceErrorCode.VOID_TRANSACTION); }
    private void validateEvidence(MultipartFile evidence) {
        if (evidence == null || evidence.isEmpty()) throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        if (evidence.getSize() > EVIDENCE_MAX_BYTES) throw new GeneralException(FileErrorCode.TOO_LARGE);
        try { ImageFileValidator.validate(evidence); } catch (GeneralException ex) { throw new GeneralException(FileErrorCode.INVALID_TYPE); }
    }
    private void rejectDuplicateEvidence(Long orgId, String checksum) {
        if (transactionRepository.existsEvidenceChecksum(orgId, checksum, TransactionStatus.POSTED))
            throw new GeneralException(FinanceErrorCode.DUPLICATE_EVIDENCE);
    }
    private Evidence evidence(UploadedFile file) { return new Evidence(file.getId(), file.getOriginalName(), file.getContentType(), file.getSizeBytes(), fileStorageService.getFileUrl(file.getStorageKey())); }
    private TransactionResult transactionResult(FinancialTransaction tx, BigDecimal balance) {
        return new TransactionResult(tx.getId(), tx.getTransactionType(), tx.getTitle(), tx.getCategory(), tx.getAmount(),
                tx.getOccurredOn(), tx.getVendor(), tx.getPaymentMethod(), tx.getMemo(), tx.getStatus(), tx.getEvidenceFileId(),
                balance, tx.getCreatedAt(), tx.getUpdatedAt());
    }
    private String snapshot(FinancialTransaction tx) { return "{\"type\":\"" + tx.getTransactionType() + "\",\"title\":\"" + tx.getTitle().replace("\"", "'") + "\",\"amount\":\"" + tx.getAmount() + "\"}"; }
    private void deleteAfterCommit(String storageKey) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            fileStorageService.delete(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.delete(storageKey);
            }
        });
    }
    private void validatePeriod(LocalDate from, LocalDate to) { if ((from == null) != (to == null) || (from != null && from.isAfter(to))) throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String encodeOffset(int offset) { return Base64.getUrlEncoder().withoutPadding().encodeToString(("offset:" + offset).getBytes(StandardCharsets.UTF_8)); }
    private int decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int offset = Integer.parseInt(value.substring(7));
            if (!value.startsWith("offset:") || offset < 0) throw new IllegalArgumentException();
            return offset;
        }
        catch (Exception ex) { throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR); }
    }
}
