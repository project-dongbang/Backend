package com.dongbang.photo.domain;

import com.dongbang.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "uploaded_file_id", nullable = false)
    private Long uploadedFileId;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    public Photo(Long id, Long organizationId, Long uploadedFileId, Long membershipId, String title) {
        this.id = id;
        this.organizationId = organizationId;
        this.uploadedFileId = uploadedFileId;
        this.membershipId = membershipId;
        this.title = title;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
