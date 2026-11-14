package com.agencytrip.agencytripapi.application.dto;

public record GoogleUserPayload(
    String sub,
    String email,
    boolean emailVerified,
    String givenName,
    String familyName,
    String name
) {}

