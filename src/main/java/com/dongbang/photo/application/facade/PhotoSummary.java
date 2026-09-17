package com.dongbang.photo.application.facade;

public record PhotoSummary(
        Long photoId,
        String title,
        String imageUrl
) {
}
