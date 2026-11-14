package com.agencytrip.agencytripapi.application.dto;

public record FacebookUserPayload(
        String id,
        String email,
        String name,
        String firstName,
        String lastName
) {}

