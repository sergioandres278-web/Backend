package com.agencytrip.agencytripapi.application.port.out;

import com.agencytrip.agencytripapi.application.dto.FacebookUserPayload;

public interface FacebookTokenVerifierPort {
    FacebookUserPayload verify(String accessToken);
}

