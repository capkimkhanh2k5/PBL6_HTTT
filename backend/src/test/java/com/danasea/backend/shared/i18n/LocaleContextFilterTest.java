package com.danasea.backend.shared.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import com.danasea.backend.modules.account.domain.models.User;

class LocaleContextFilterTest {

    private final AccountInternalApi accounts = mock(AccountInternalApi.class);
    private final LocaleContextFilter filter = new LocaleContextFilter(accounts);

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void supportedHeaderOverridesProfile() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("member@example.com", "en-US");
        when(accounts.findUserByEmail("member@example.com")).thenReturn(Optional.of(user("vi")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals("en", LocaleContextHolder.getLocale().getLanguage()));

        assertEquals("en", response.getHeader("Content-Language"));
        assertEquals("Accept-Language", response.getHeader("Vary"));
        assertNull(LocaleContextHolder.getLocaleContext());
    }

    @Test
    void unsupportedHeaderFallsBackToProfile() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("member@example.com", "fr-FR");
        when(accounts.findUserByEmail("member@example.com")).thenReturn(Optional.of(user("en-GB")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertEquals("en", response.getHeader("Content-Language"));
    }

    @Test
    void anonymousWithoutHeaderDefaultsToVietnamese() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertEquals("vi", response.getHeader("Content-Language"));
    }

    private MockHttpServletRequest authenticatedRequest(String email, String acceptLanguage) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", acceptLanguage);
        Principal principal = () -> email;
        request.setUserPrincipal(principal);
        return request;
    }

    private User user(String locale) {
        User user = new User();
        user.setLocale(locale);
        return user;
    }
}
