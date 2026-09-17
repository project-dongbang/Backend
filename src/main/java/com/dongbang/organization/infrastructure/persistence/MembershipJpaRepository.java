package com.dongbang.organization.infrastructure.persistence;

import com.dongbang.organization.domain.Membership;
import com.dongbang.organization.domain.repository.MembershipRepository;
import com.dongbang.organization.domain.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipJpaRepository extends JpaRepository<Membership, Long>, com.dongbang.organization.domain.repository.MembershipRepository {

    @Override
    @Query("SELECT m FROM Membership m WHERE m.organization.id = :organizationId AND m.userId = :userId AND m.status = 'ACTIVE'")
    Optional<Membership> findByOrganizationIdAndUserId(@Param("organizationId") Long organizationId, @Param("userId") Long userId);

    @Override
    @Query("SELECT m FROM Membership m JOIN FETCH m.organization WHERE m.userId = :userId AND m.status = 'ACTIVE'")
    List<Membership> findAllByUserId(@Param("userId") Long userId);

    @Override
    @Query("SELECT m FROM Membership m WHERE m.organization.id = :organizationId")
    List<Membership> findAllByOrganizationId(@Param("organizationId") Long organizationId);

    @Override
    @Query("SELECT m FROM Membership m WHERE m.organization.id = :organizationId AND m.status = :status")
    List<Membership> findAllByOrganizationIdAndStatus(@Param("organizationId") Long organizationId, @Param("status") MembershipStatus status);

    @Override
    @Query("SELECT m FROM Membership m WHERE m.userId IS NULL AND m.memberName = :memberName " +
            "AND m.studentNumber = :studentNumber AND m.status = 'ACTIVE'")
    List<Membership> findAllUnlinkedByIdentity(
            @Param("memberName") String memberName,
            @Param("studentNumber") String studentNumber
    );
}
