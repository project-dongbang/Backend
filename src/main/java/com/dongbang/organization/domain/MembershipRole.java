package com.dongbang.organization.domain;

public enum MembershipRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean isStaff() {
        return this == OWNER || this == ADMIN;
    }

    public boolean isOwner() {
        return this == OWNER;
    }
}
