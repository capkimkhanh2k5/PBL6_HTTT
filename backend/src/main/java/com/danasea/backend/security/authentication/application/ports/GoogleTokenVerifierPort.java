package com.danasea.backend.security.authentication.application.ports;

import com.danasea.backend.security.authentication.domain.models.GoogleUserInfo;

public interface GoogleTokenVerifierPort {
    GoogleUserInfo verify(String idToken);
}
