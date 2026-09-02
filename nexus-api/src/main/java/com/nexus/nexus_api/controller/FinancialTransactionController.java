package com.nexus.nexus_api.controller;

import com.nexus.nexus_api.dto.FinancialTransactionRequest;
import com.nexus.nexus_api.dto.FinancialTransactionResponse;
import com.nexus.nexus_api.model.FinancialTransaction;
import com.nexus.nexus_api.service.FinancialTransactionService;
import com.nexus.nexus_api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialTransactionResponse create(@PathVariable Long userId, @Valid @RequestBody FinancialTransactionRequest request) {
        // userId da URL precisa ser o do próprio usuário autenticado — o dono real da
        // transação sempre vem do token, nunca do corpo da requisição.
        SecurityUtils.assertOwnership(userId);
        FinancialTransaction created = transactionService.create(request, SecurityUtils.getCurrentUserId());
        return FinancialTransactionResponse.from(created);
    }

    @GetMapping("/user/{userId}")
    public List<FinancialTransactionResponse> listByUser(@PathVariable Long userId) {
        return transactionService.listByUser(userId).stream()
                .map(FinancialTransactionResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinancialTransactionResponse> update(@PathVariable Long id, @Valid @RequestBody FinancialTransactionRequest request) {
        FinancialTransaction updated = transactionService.update(id, request);
        return ResponseEntity.ok(FinancialTransactionResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
