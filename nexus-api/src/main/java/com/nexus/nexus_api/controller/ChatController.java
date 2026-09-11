package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.ChatRequest;
import com.nexus.nexus_api.dto.ChatResponse;
import com.nexus.nexus_api.exception.AiServiceException;
import com.nexus.nexus_api.service.GeminiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/study-chat")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiChatService geminiChatService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Digite uma pergunta antes de enviar."));
        }

        try {
            String reply = geminiChatService.ask(request.message(), request.history());
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (AiServiceException e) {
            // Mensagem já é segura pra expor (escrita por nós em GeminiClient) e o
            // status já reflete a causa real (429 rate limit, 503 chave ausente, etc.)
            // — deixa o GlobalExceptionHandler formatar a resposta.
            throw e;
        } catch (Exception e) {
            // Qualquer outra falha inesperada: não expõe e.getMessage()/stacktrace ao
            // cliente, mas registra no log do servidor.
            log.error("Falha ao consultar a IA em /api/study-chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse("Não foi possível processar sua pergunta agora. Tente novamente em instantes."));
        }
    }
}