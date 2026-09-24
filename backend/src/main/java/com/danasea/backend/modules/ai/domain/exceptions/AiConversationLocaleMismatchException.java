package com.danasea.backend.modules.ai.domain.exceptions;

import com.danasea.backend.shared.i18n.LocalizedException;

public class AiConversationLocaleMismatchException extends LocalizedException {
    public AiConversationLocaleMismatchException() {
        super("AI_CONVERSATION_LOCALE_MISMATCH", "error.ai_conversation_locale_mismatch");
    }
}
