package com.dongbang.user.application.facade;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.user.domain.User;
import com.dongbang.user.domain.repository.UserRepository;
import com.dongbang.user.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Transactional
public class UserAccountFacade {

    private final UserRepository userRepository;

    public Long createPendingUser() {
        return userRepository.save(User.pendingOnboarding()).getId();
    }

    public void recordLogin(Long userId, Instant loggedInAt) {
        findUser(userId).recordLogin(loggedInAt);
    }

    @Transactional(readOnly = true)
    public UserAccountSummary getAccount(Long userId) {
        return toSummary(findUser(userId));
    }

    public UserAccountSummary completeOnboarding(
            Long userId,
            String name,
            String studentNumber,
            String department,
            String email,
            Instant completedAt
    ) {
        User user = findUser(userId);
        if (!user.requiresOnboarding()) {
            throw new GeneralException(com.dongbang.auth.exception.AuthErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedStudentNumber = studentNumber.trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new GeneralException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByStudentNumber(normalizedStudentNumber)) {
            throw new GeneralException(UserErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }

        user.completeOnboarding(
                name.trim(),
                normalizedStudentNumber,
                department.trim(),
                normalizedEmail,
                completedAt
        );
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw mapDuplicate(ex);
        }
        return toSummary(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private GeneralException mapDuplicate(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uq_users_student_number")) {
            return new GeneralException(UserErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }
        return new GeneralException(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }

    private UserAccountSummary toSummary(User user) {
        return new UserAccountSummary(
                user.getId(),
                user.getName(),
                user.getStudentNumber(),
                user.getDepartment(),
                user.getEmail(),
                user.getStatus(),
                user.getOnboardingCompletedAt()
        );
    }
}
