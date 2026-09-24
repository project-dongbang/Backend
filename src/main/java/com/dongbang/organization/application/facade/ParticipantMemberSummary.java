package com.dongbang.organization.application.facade;

public record ParticipantMemberSummary(Long membershipId, Long userId, String memberName,
                                       String studentNumber, boolean active) {}
