package com.danasea.backend.shared.i18n;

import com.danasea.backend.modules.account.application.api.AccountInternalApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;

@Component
public class LocaleContextFilter extends OncePerRequestFilter {
    public static final String REQUEST_LANGUAGE_ATTRIBUTE = LocaleContextFilter.class.getName() + ".language";

    private final AccountInternalApi accountInternalApi;

    @Autowired
    public LocaleContextFilter(ObjectProvider<AccountInternalApi> accountInternalApiProvider) {
        this.accountInternalApi = accountInternalApiProvider.getIfAvailable();
    }

    public LocaleContextFilter(AccountInternalApi accountInternalApi) {
        this.accountInternalApi = accountInternalApi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SupportedLanguage language = resolve(request);
        LocaleContextHolder.setLocale(language.locale());
        request.setAttribute(REQUEST_LANGUAGE_ATTRIBUTE, language);
        response.setHeader("Content-Language", language.code());
        response.setHeader("Vary", mergeVary(response.getHeader("Vary")));
        try {
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    public SupportedLanguage resolve(HttpServletRequest request) {
        return SupportedLanguage.fromAcceptLanguage(request.getHeader("Accept-Language"))
                .or(() -> resolveProfileLanguage(request.getUserPrincipal()))
                .orElse(SupportedLanguage.DEFAULT);
    }

    private java.util.Optional<SupportedLanguage> resolveProfileLanguage(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return java.util.Optional.empty();
        }
        if (accountInternalApi == null) {
            return java.util.Optional.empty();
        }
        return accountInternalApi.findUserByEmail(principal.getName())
                .flatMap(user -> SupportedLanguage.fromTag(user.getLocale()));
    }

    private String mergeVary(String current) {
        if (current == null || current.isBlank()) {
            return "Accept-Language";
        }
        if (current.toLowerCase(java.util.Locale.ROOT).contains("accept-language")) {
            return current;
        }
        return current + ", Accept-Language";
    }
}
