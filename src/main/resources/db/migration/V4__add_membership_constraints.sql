-- 한 사용자는 한 동아리에 ACTIVE 상태로 한 번만 가입할 수 있습니다.
CREATE UNIQUE INDEX uq_memberships_active_org_user
    ON memberships (organization_id, user_id)
    WHERE status = 'ACTIVE' AND user_id IS NOT NULL;
