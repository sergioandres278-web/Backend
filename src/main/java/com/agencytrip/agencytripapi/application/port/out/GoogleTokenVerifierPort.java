package com.agencytrip.agencytripapi.application.port.out;

import com.agencytrip.agencytripapi.application.dto.GoogleUserPayload;

public interface GoogleTokenVerifierPort {
    GoogleUserPayload verify(String idToken);
}

