package com.dongbang.event.domain;

import com.dongbang.event.exception.EventErrorCode;
import com.dongbang.global.entity.BaseTimeEntity;
import com.dongbang.global.exception.GeneralException;
import com.dongbang.global.response.code.GeneralErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "created_by_membership_id", nullable = false)
    private Long createdByMembershipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    private Integer capacity;

    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at")
    private Instant registrationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(nullable = false)
    private long participantVersion;
    private Instant registrationClosedAt;

    public void participantsChanged() { participantVersion++; }
    public void closeRegistration(Instant now) { registrationClosedAt = now; }
    public void cancel(Instant now) { status = EventStatus.CANCELED; canceledAt = now; }
    @Builder
    public Event(Long id, Long organizationId, Long createdByMembershipId,
                 EventType type, EventDetails details) {
        this.id = id;
        this.organizationId = organizationId;
        this.createdByMembershipId = createdByMembershipId;
        this.type = type;
        this.status = EventStatus.SCHEDULED;
        revise(details);
    }

    public void revise(EventDetails details) {
        if (type == null || details.title() == null || details.title().isBlank()
                || details.title().length() > 200
                || details.location() == null || details.location().isBlank()
                || details.location().length() > 255
                || (details.description() != null && details.description().length() > 5000)
                || (details.capacity() != null && details.capacity() < 1)) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR);
        }
        if (type == EventType.SCHEDULE
                && (details.capacity() != null || details.registrationDeadline() != null)) {
            throw new GeneralException(GeneralErrorCode.VALIDATION_ERROR,
                    "일반 일정에는 정원과 신청 마감을 설정할 수 없습니다.");
        }
        if (details.startsAt() == null || details.endsAt() == null
                || !details.endsAt().isAfter(details.startsAt())
                || (details.registrationDeadline() != null
                    && details.registrationDeadline().isAfter(details.startsAt()))) {
            throw new GeneralException(EventErrorCode.INVALID_EVENT_TIME);
        }
        this.title = details.title();
        this.description = details.description();
        this.location = details.location();
        this.startsAt = details.startsAt();
        this.endsAt = details.endsAt();
        this.capacity = details.capacity();
        // 신청 마감 미지정 시 행사 시작 시각 사용
        this.registrationDeadline = type == EventType.EVENT
                ? (details.registrationDeadline() == null ? startsAt : details.registrationDeadline())
                : null;
    }

    public void delete(Instant now) {
        // 논리 삭제: 데이터 유지, deletedAt 기준 조회 제외
        this.deletedAt = now;
    }
}
