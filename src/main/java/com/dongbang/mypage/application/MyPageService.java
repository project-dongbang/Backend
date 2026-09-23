package com.dongbang.mypage.application;

import com.dongbang.auth.domain.OAuthAccount;
import com.dongbang.auth.domain.OAuthProvider;
import com.dongbang.auth.domain.repository.OAuthAccountRepository;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import com.dongbang.mypage.presentation.dto.request.UpdateMyProfileRequest;
import com.dongbang.mypage.presentation.dto.response.CurrentMembershipResponse;
import com.dongbang.mypage.presentation.dto.response.MyProfileResponse;
import com.dongbang.mypage.presentation.dto.response.ProfileImageResponse;
import com.dongbang.mypage.presentation.dto.response.UpdateMyProfileResponse;
import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.MembershipStatus;
import com.dongbang.organization.domain.OrganizationStatus;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.repository.OrganizationRepository;
import com.dongbang.user.domain.User;
import com.dongbang.user.domain.repository.UserRepository;
import com.dongbang.user.exception.UserErrorCode;
import com.dongbang.photo.infrastructure.storage.FileStorageResult;
import com.dongbang.photo.infrastructure.storage.FileStorageService;
import com.dongbang.photo.infrastructure.storage.ImageFileValidator;
import com.dongbang.global.response.code.FileErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final FileStorageService fileStorageService;

    public MyProfileResponse getMyProfile(Long userId, Long organizationId) {
        User user = findUser(userId);
        CurrentMembershipResponse currentMembership = organizationId == null
                ? null
                : getCurrentMembership(organizationId, userId);

        List<OAuthProvider> providers = oauthAccountRepository.findAllByUserId(userId).stream()
                .map(OAuthAccount::getProvider)
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        return new MyProfileResponse(
                user.getId(),
                user.getName(),
                user.getStudentNumber(),
                user.getDepartment(),
                user.getEmail(),
                resolveProfileImageUrl(user),
                providers,
                currentMembership
        );
    }

    @Transactional
    public UpdateMyProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        User user = findUser(userId);
        String name = normalizeOptional(request.name());
        String studentNumber = normalizeOptional(request.studentNumber());
        String department = normalizeOptional(request.department());
        String email = normalizeEmail(request.email());

        if (email != null && userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new GeneralException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (studentNumber != null && userRepository.existsByStudentNumberAndIdNot(studentNumber, userId)) {
            throw new GeneralException(UserErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }

        user.updateProfile(name, studentNumber, department, email);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw mapDuplicate(ex);
        }

        return new UpdateMyProfileResponse(
                user.getId(),
                user.getName(),
                user.getStudentNumber(),
                user.getDepartment(),
                user.getEmail(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public ProfileImageResponse updateProfileImage(Long userId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        if (image.getSize() > 10L * 1024 * 1024) {
            throw new GeneralException(FileErrorCode.TOO_LARGE);
        }
        try {
            ImageFileValidator.validate(image);
        } catch (GeneralException ex) {
            throw new GeneralException(FileErrorCode.INVALID_TYPE);
        }

        User user = findUser(userId);
        String oldStorageKey = user.getProfileImageStorageKey();
        FileStorageResult stored = fileStorageService.storeForUser(image, userId, "profile-images");
        try {
            String imageUrl = fileStorageService.getFileUrl(stored.storageKey());
            user.updateProfileImage(stored.storageKey());
            userRepository.flush();
            if (oldStorageKey != null && !oldStorageKey.equals(stored.storageKey())) {
                deleteAfterCommit(oldStorageKey);
            }
            return new ProfileImageResponse(imageUrl);
        } catch (RuntimeException ex) {
            fileStorageService.delete(stored.storageKey());
            throw ex;
        }
    }

    @Transactional
    public ProfileImageResponse useDefaultProfileImage(Long userId) {
        User user = findUser(userId);
        String oldStorageKey = user.getProfileImageStorageKey();
        if (oldStorageKey == null && user.getProfileImageUrl() == null) {
            return new ProfileImageResponse(null);
        }
        user.useDefaultProfileImage();
        userRepository.flush();
        deleteAfterCommit(oldStorageKey);
        return new ProfileImageResponse(null);
    }

    private CurrentMembershipResponse getCurrentMembership(Long organizationId, Long userId) {
        organizationRepository.findById(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        Membership membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(item -> item.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.FORBIDDEN));

        String role = "ADMIN".equals(membership.getRole().name())
                ? "MANAGER"
                : membership.getRole().name();
        return new CurrentMembershipResponse(
                organizationId,
                role,
                membership.getGeneration(),
                membership.getPosition()
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private String resolveProfileImageUrl(User user) {
        if (user.getProfileImageStorageKey() != null) {
            return fileStorageService.getFileUrl(user.getProfileImageStorageKey());
        }
        return user.getProfileImageUrl();
    }

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

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private GeneralException mapDuplicate(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uq_users_student_number")) {
            return new GeneralException(UserErrorCode.STUDENT_NUMBER_ALREADY_EXISTS);
        }
        return new GeneralException(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
