package com.dongbang.finance.application;

import com.dongbang.finance.domain.*;
import com.dongbang.finance.domain.repository.*;
import com.dongbang.finance.exception.FinanceErrorCode;
import com.dongbang.finance.presentation.dto.FinanceDtos.*;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeeService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final FeeItemRepository feeItemRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final FeeTargetRepository feeTargetRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final MembershipRepository membershipRepository;
    private final FinanceAccessService accessService;

    public PageResult<FeeItemListItem> getFeeItems(Long organizationId, Long userId, int page, int size) {
        accessService.requireMember(organizationId, userId);
        var result = feeItemRepository.findAllByOrganizationId(organizationId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Long> ids = result.getContent().stream().map(FeeItem::getId).toList();
        Map<Long, List<FeeTarget>> targets = feeTargetRepository.findAllByFeeItemIdIn(ids).stream()
                .collect(Collectors.groupingBy(FeeTarget::getFeeItemId));
        Map<Long, Long> categoryCounts = feeCategoryRepository.findAllByFeeItemIdIn(ids).stream()
                .collect(Collectors.groupingBy(FeeCategory::getFeeItemId, Collectors.counting()));
        List<FeeItemListItem> content = result.getContent().stream().map(item -> {
            List<FeeTarget> itemTargets = targets.getOrDefault(item.getId(), List.of());
            return new FeeItemListItem(item.getId(), item.getTitle(), categoryCounts.getOrDefault(item.getId(), 0L),
                    itemTargets.size(), total(itemTargets), item.getDueDate());
        }).toList();
        return new PageResult<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public FeeItemCreatedResult createFeeItem(Long organizationId, Long userId, CreateFeeItemRequest request) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        validateCreateRequest(organizationId, request);
        FeeItem item = feeItemRepository.save(new FeeItem(organizationId, request.title().trim(), request.dueDate(),
                trim(request.description()), request.paymentAccount().bankName().trim(),
                request.paymentAccount().accountNumber().trim(),
                request.paymentAccount().accountHolder().trim(), actor.getId()));

        List<CategoryResult> categoryResults = new ArrayList<>();
        int targetCount = 0;
        BigDecimal expectedAmount = BigDecimal.ZERO;
        for (int i = 0; i < request.categories().size(); i++) {
            CreateCategory input = request.categories().get(i);
            FeeCategory category = feeCategoryRepository.save(new FeeCategory(item.getId(), input.name().trim(), input.amount(), i));
            for (Long membershipId : input.targetMembershipIds()) {
                feeTargetRepository.save(new FeeTarget(item.getId(), category.getId(), membershipId, input.amount()));
            }
            int count = input.targetMembershipIds().size();
            targetCount += count;
            expectedAmount = expectedAmount.add(input.amount().multiply(BigDecimal.valueOf(count)));
            categoryResults.add(new CategoryResult(category.getId(), category.getName(), category.getAmount(), count));
        }
        return new FeeItemCreatedResult(item.getId(), item.getTitle(), item.getDueDate(), account(item),
                categoryResults.size(), targetCount, expectedAmount, categoryResults, item.getCreatedAt());
    }

    @Transactional
    public FeeItemUpdatedResult updateFeeItem(Long organizationId, Long userId, Long feeItemId, UpdateFeeItemRequest request) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        if (request.isEmpty()) throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        FeeItem item = findItem(organizationId, feeItemId);
        String before = snapshot(item);
        PaymentAccount account = request.paymentAccount();
        item.update(trim(request.title()), request.dueDate(), trim(request.description()),
                account == null ? null : account.bankName().trim(),
                account == null ? null : account.accountNumber().trim(),
                account == null ? null : account.accountHolder().trim(), request.descriptionPresent());
        auditLogRepository.save(new AuditLog(organizationId, actor.getId(), "UPDATE", "FEE_ITEM", item.getId(), before, snapshot(item)));
        return updated(item);
    }

    @Transactional
    public FeeItemDeletedResult deleteFeeItem(Long organizationId, Long userId, Long feeItemId) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        FeeItem item = findItem(organizationId, feeItemId);
        long categoryCount = feeCategoryRepository.countByFeeItemId(item.getId());
        long targetCount = feeTargetRepository.countByFeeItemId(item.getId());
        Instant now = Instant.now();
        long voided = 0;
        for (FinancialTransaction transaction : transactionRepository.findAllByFeeItemId(item.getId())) {
            if (transaction.getStatus() == TransactionStatus.POSTED) {
                transaction.voidTransaction("납부 항목 삭제", actor.getId(), now);
                voided++;
            }
            transaction.detachFeeReferences();
        }
        auditLogRepository.save(new AuditLog(organizationId, actor.getId(), "DELETE", "FEE_ITEM", item.getId(), deletionSnapshot(item), null));
        feeItemRepository.delete(item);
        return new FeeItemDeletedResult(feeItemId, categoryCount, targetCount, voided, actor.getId(), now);
    }

    public CursorResult<FeeTargetItem> getTargets(Long organizationId, Long userId, Long feeItemId,
                                                   FeeTargetStatus status, String cursor, int size) {
        accessService.requireStaff(organizationId, userId);
        findItem(organizationId, feeItemId);
        List<FeeTarget> targets = status == null ? feeTargetRepository.findAllByFeeItemId(feeItemId)
                : feeTargetRepository.findAllByFeeItemIdAndStatus(feeItemId, status);
        Map<Long, Membership> members = membershipRepository.findAllByIdIn(targets.stream().map(FeeTarget::getMembershipId).toList())
                .stream().collect(Collectors.toMap(Membership::getId, Function.identity()));
        Map<Long, FeeCategory> categories = feeCategoryRepository.findAllByFeeItemIdOrderByDisplayOrderAsc(feeItemId)
                .stream().collect(Collectors.toMap(FeeCategory::getId, Function.identity()));
        targets.sort(Comparator.comparing((FeeTarget target) -> members.get(target.getMembershipId()).getStudentNumber())
                .thenComparing(FeeTarget::getId));
        int offset = decodeOffset(cursor);
        List<FeeTarget> slice = targets.stream().skip(offset).limit(size).toList();
        List<FeeTargetItem> content = slice.stream().map(target -> {
            Membership member = members.get(target.getMembershipId());
            FeeCategory category = categories.get(target.getFeeCategoryId());
            return new FeeTargetItem(target.getId(), member.getId(), member.getMemberName(), member.getStudentNumber(),
                    member.getGeneration(), category.getId(), category.getName(), target.getAmountDue(), target.getStatus(), target.getPaidAt());
        }).toList();
        long targetCount = feeTargetRepository.countByFeeItemId(feeItemId);
        long paidCount = feeTargetRepository.countByFeeItemIdAndStatus(feeItemId, FeeTargetStatus.PAID);
        FeeTargetSummary summary = new FeeTargetSummary(feeItemId, targetCount, paidCount, targetCount - paidCount,
                targetCount == 0 ? 0 : BigDecimal.valueOf(paidCount * 100.0 / targetCount).setScale(1, RoundingMode.HALF_UP).doubleValue());
        boolean hasNext = offset + slice.size() < targets.size();
        return new CursorResult<>(summary, content, size, hasNext ? encodeOffset(offset + slice.size()) : null, hasNext);
    }

    public PageResult<MyFeeTargetItem> getMyTargets(Long organizationId, Long userId, FeeTargetStatus status, int page, int size) {
        Membership member = accessService.requireMember(organizationId, userId);
        List<FeeTarget> all = status == null ? feeTargetRepository.findAllByMembershipId(member.getId())
                : feeTargetRepository.findAllByMembershipIdAndStatus(member.getId(), status);
        Map<Long, FeeItem> items = feeItemRepository.findAllById(all.stream().map(FeeTarget::getFeeItemId).toList()).stream()
                .filter(item -> item.getOrganizationId().equals(organizationId))
                .collect(Collectors.toMap(FeeItem::getId, Function.identity()));
        Map<Long, FeeCategory> categories = feeCategoryRepository.findAllById(all.stream().map(FeeTarget::getFeeCategoryId).toList())
                .stream().collect(Collectors.toMap(FeeCategory::getId, Function.identity()));
        all = all.stream().filter(target -> items.containsKey(target.getFeeItemId()))
                .sorted(Comparator.comparing((FeeTarget target) -> items.get(target.getFeeItemId()).getDueDate()).reversed()
                        .thenComparing(FeeTarget::getId, Comparator.reverseOrder())).toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<MyFeeTargetItem> content = all.subList(from, to).stream().map(target -> {
            FeeItem item = items.get(target.getFeeItemId()); FeeCategory category = categories.get(target.getFeeCategoryId());
            return new MyFeeTargetItem(target.getId(), item.getId(), item.getTitle(), category.getId(), category.getName(),
                    target.getAmountDue(), item.getDueDate(), account(item), item.getDescription(), target.getStatus(), target.getPaidAt());
        }).toList();
        return new PageResult<>(content, page, size, all.size(), (int) Math.ceil(all.size() / (double) size));
    }

    @Transactional
    public FeeTargetStatusResult changeStatus(Long organizationId, Long userId, Long targetId, ChangeFeeTargetStatusRequest request) {
        Membership actor = accessService.requireStaff(organizationId, userId);
        FeeTarget target = feeTargetRepository.findById(targetId)
                .orElseThrow(() -> new GeneralException(FinanceErrorCode.FEE_TARGET_NOT_FOUND));
        FeeItem item = findItem(organizationId, target.getFeeItemId());
        Optional<FinancialTransaction> active = transactionRepository.findFirstByFeeTargetIdAndTransactionTypeAndStatus(
                targetId, TransactionType.INCOME, TransactionStatus.POSTED);
        if (target.getStatus() == request.status()) {
            return statusResult(target, active.map(FinancialTransaction::getId).orElse(null));
        }
        Instant now = Instant.now();
        Long transactionId = null;
        if (request.status() == FeeTargetStatus.PAID) {
            FinancialTransaction income = transactionRepository.save(new FinancialTransaction(organizationId, item.getId(), target.getId(),
                    TransactionType.INCOME, item.getTitle(), "납부 수입", target.getAmountDue(), LocalDate.now(SEOUL),
                    "회원 납부", null, request.memo(), null, actor.getId()));
            transactionId = income.getId();
        } else {
            FinancialTransaction income = active.orElseThrow(() -> new GeneralException(FinanceErrorCode.INVALID_STATUS_CHANGE));
            income.voidTransaction("납부 상태를 미납으로 변경", actor.getId(), now);
        }
        target.changeStatus(request.status(), trim(request.memo()), actor.getId(), now);
        return statusResult(target, transactionId);
    }

    public byte[] exportTargets(Long organizationId, Long userId, Long feeItemId, FeeTargetStatus status, String format) {
        accessService.requireStaff(organizationId, userId); findItem(organizationId, feeItemId);
        @SuppressWarnings("unchecked") CursorResult<FeeTargetItem> result = getTargets(organizationId, userId, feeItemId, status, null, 100000);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("카테고리", "회원명", "학번", "기수", "납부 금액", "납부 상태", "납부 완료 일시"));
        result.content().forEach(item -> rows.add(List.of(item.categoryName(), item.name(), item.studentNumber(),
                Objects.toString(item.generation(), ""), item.amountDue().toPlainString(), item.status().name(), Objects.toString(item.paidAt(), ""))));
        return "csv".equalsIgnoreCase(format) ? SpreadsheetExport.csv(rows) : SpreadsheetExport.xlsx(rows);
    }

    private void validateCreateRequest(Long organizationId, CreateFeeItemRequest request) {
        Set<String> names = new HashSet<>(); Set<Long> targetIds = new HashSet<>();
        for (CreateCategory category : request.categories()) {
            if (!names.add(category.name().trim())) throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
            for (Long id : category.targetMembershipIds()) {
                if (!targetIds.add(id)) throw new GeneralException(FinanceErrorCode.DUPLICATE_TARGET);
            }
        }
        Map<Long, Membership> members = membershipRepository.findAllByIdIn(targetIds).stream()
                .collect(Collectors.toMap(Membership::getId, Function.identity()));
        boolean invalid = members.size() != targetIds.size() || targetIds.stream().anyMatch(id -> {
            Membership member = members.get(id);
            return member.getStatus() != MembershipStatus.ACTIVE || !member.getOrganization().getId().equals(organizationId);
        });
        if (invalid) throw new GeneralException(FinanceErrorCode.INVALID_TARGET);
    }

    private FeeItem findItem(Long organizationId, Long feeItemId) {
        return feeItemRepository.findByIdAndOrganizationId(feeItemId, organizationId)
                .orElseThrow(() -> new GeneralException(FinanceErrorCode.FEE_ITEM_NOT_FOUND));
    }
    private PaymentAccount account(FeeItem item) { return new PaymentAccount(item.getBankName(), item.getAccountNumber(), item.getAccountHolder()); }
    private BigDecimal total(Collection<FeeTarget> targets) { return targets.stream().map(FeeTarget::getAmountDue).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private FeeItemUpdatedResult updated(FeeItem item) {
        return new FeeItemUpdatedResult(item.getId(), item.getTitle(), item.getDueDate(), item.getDescription(), account(item),
                feeCategoryRepository.countByFeeItemId(item.getId()), total(feeTargetRepository.findAllByFeeItemId(item.getId())), item.getUpdatedAt());
    }
    private FeeTargetStatusResult statusResult(FeeTarget target, Long transactionId) {
        return new FeeTargetStatusResult(target.getId(), target.getStatus(), target.getStatusMemo(), target.getPaidAt(),
                target.getStatusChangedAt(), target.getStatusChangedByMembershipId(), transactionId);
    }
    private String snapshot(FeeItem item) { return "{\"title\":\"" + item.getTitle().replace("\"", "'") + "\",\"dueDate\":\"" + item.getDueDate() + "\"}"; }
    private String deletionSnapshot(FeeItem item) {
        String categories = feeCategoryRepository.findAllByFeeItemIdOrderByDisplayOrderAsc(item.getId()).stream()
                .map(category -> "{\"name\":\"" + category.getName().replace("\"", "'") + "\",\"amount\":\"" + category.getAmount() + "\"}")
                .collect(Collectors.joining(","));
        String targets = feeTargetRepository.findAllByFeeItemId(item.getId()).stream()
                .map(target -> Objects.toString(target.getMembershipId()))
                .collect(Collectors.joining(","));
        return "{\"title\":\"" + item.getTitle().replace("\"", "'") + "\",\"dueDate\":\"" + item.getDueDate()
                + "\",\"categories\":[" + categories + "],\"targetMembershipIds\":[" + targets + "]}";
    }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String encodeOffset(int offset) { return Base64.getUrlEncoder().withoutPadding().encodeToString(("offset:" + offset).getBytes(StandardCharsets.UTF_8)); }
    private int decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) return 0;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int offset = Integer.parseInt(decoded.substring(7));
            if (!decoded.startsWith("offset:") || offset < 0) throw new IllegalArgumentException();
            return offset;
        }
        catch (Exception ex) { throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR); }
    }
}
