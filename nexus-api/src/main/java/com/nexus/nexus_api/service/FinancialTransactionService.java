package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.FinancialTransactionRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.FinancialTransaction;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.FinancialTransactionRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public FinancialTransaction create(FinancialTransactionRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        FinancialTransaction transaction = FinancialTransaction.builder()
                .descricao(request.descricao())
                .valor(request.valor())
                .tipo(request.tipo())
                .data(request.data())
                .user(user)
                .build();

        return transactionRepository.save(transaction);
    }

    public List<FinancialTransaction> listByUser(Long userId) {
        SecurityUtils.assertOwnership(userId);
        return transactionRepository.findByUserId(userId);
    }

    @Transactional
    public FinancialTransaction update(Long id, FinancialTransactionRequest request) {
        FinancialTransaction existingTx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com ID: " + id));

        SecurityUtils.assertOwnership(existingTx.getUser().getId());

        existingTx.setDescricao(request.descricao());
        existingTx.setValor(request.valor());
        existingTx.setTipo(request.tipo());
        existingTx.setData(request.data());

        return transactionRepository.save(existingTx);
    }

    @Transactional
    public void delete(Long id) {
        FinancialTransaction existingTx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com ID: " + id));

        SecurityUtils.assertOwnership(existingTx.getUser().getId());

        transactionRepository.delete(existingTx);
    }
}
