package com.dongbang.user.application.facade;

import com.dongbang.global.exception.GeneralException;
import com.dongbang.user.domain.User;
import com.dongbang.user.domain.UserStatus;
import com.dongbang.user.domain.repository.UserRepository;
import com.dongbang.user.exception.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountFacadeTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAccountFacade userAccountFacade;

    @Test
    @DisplayName("최초가입 이메일과 학번을 정규화하고 사용자를 ACTIVE 상태로 전환한다")
    void completeOnboarding() {
        User user = User.pendingOnboarding();
        ReflectionTestUtils.setField(user, "id", 7L);
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("member@example.com")).willReturn(false);
        given(userRepository.existsByStudentNumber("20260001")).willReturn(false);

        UserAccountSummary result = userAccountFacade.completeOnboarding(
                7L,
                " 김동방 ",
                " 20260001 ",
                " 컴퓨터공학과 ",
                " Member@Example.COM ",
                Instant.parse("2026-09-17T10:00:00Z")
        );

        assertThat(result.name()).isEqualTo("김동방");
        assertThat(result.studentNumber()).isEqualTo("20260001");
        assertThat(result.department()).isEqualTo("컴퓨터공학과");
        assertThat(result.email()).isEqualTo("member@example.com");
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).flush();
    }

    @Test
    @DisplayName("이미 가입에 사용한 이메일은 최초가입 정보로 다시 저장할 수 없다")
    void rejectDuplicateEmail() {
        User user = User.pendingOnboarding();
        ReflectionTestUtils.setField(user, "id", 7L);
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("member@example.com")).willReturn(true);

        assertThatThrownBy(() -> userAccountFacade.completeOnboarding(
                7L,
                "김동방",
                "20260001",
                "컴퓨터공학과",
                "member@example.com",
                Instant.now()
        )).isInstanceOfSatisfying(GeneralException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS));
    }
}
