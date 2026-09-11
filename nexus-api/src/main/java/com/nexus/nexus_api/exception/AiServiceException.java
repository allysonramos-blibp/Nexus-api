package com.nexus.nexus_api.exception;

import org.springframework.http.HttpStatus;

/**
 * Erro originado na integração com o provedor de IA (Gemini): chave ausente,
 * rate limit do Free Tier, resposta bloqueada, JSON malformado, etc.
 * Carrega o {@link HttpStatus} mais apropriado para cada caso (em vez de sempre
 * cair em 500), tratado pelo {@link GlobalExceptionHandler}.
 */
public class AiServiceException extends RuntimeException {

    private final HttpStatus status;

    public AiServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AiServiceException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
