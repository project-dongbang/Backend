package com.dongbang.auth.presentation.dto.response;

public record CsrfTokenResponse(String token, String headerName) {
}
