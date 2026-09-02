package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.ChatRequest;
import com.nexus.nexus_api.dto.ChatResponse;
import com.nexus.nexus_api.service.AnthropicChatService;
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

    private final AnthropicChatService anthropicChatService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Digite uma pergunta antes de enviar."));
        }

        try {
            String reply = anthropicChatService.ask(request.message(), request.history());
            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            // Não expõe e.getMessage()/stacktrace ao cliente (vazava detalhes internos),
            // mas registra no log do servidor — sem isso não tinha como saber o motivo real
            // (chave da Anthropic ausente/inválida, modelo errado, timeout, etc.).
            log.error("Falha ao consultar a IA em /api/study-chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ChatResponse("Não foi possível processar sua pergunta agora. Tente novamente em instantes."));
        }
    }
}