package com.dongbang.photo.presentation.dto.response;

import java.util.List;

public record PhotoCursorResponse(
        List<PhotoListItemResponse> content,
        boolean hasNext,
        Long nextCursor
) {
}
