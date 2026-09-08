package com.nexus.nexus_api.service;

import com.nexus.nexus_api.dto.FinancialTransactionRequest;
import com.nexus.nexus_api.exception.ResourceNotFoundException;
import com.nexus.nexus_api.model.Category;
import com.nexus.nexus_api.model.FinancialTransaction;
import com.nexus.nexus_api.model.TransactionStatus;
import com.nexus.nexus_api.model.User;
import com.nexus.nexus_api.repository.CategoryRepository;
import com.nexus.nexus_api.repository.FinancialTransactionRepository;
import com.nexus.nexus_api.repository.UserRepository;
import com.nexus.nexus_api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /** Busca a categoria pelo id garantindo que pertence ao mesmo usuário — nunca deixa
     *  o usuário A grudar uma transação numa categoria do usuário B. Null é permitido
     *  (categoria é opcional). */
    private Category resolveCategory(Long categoryId, Long userId) {
        if (categoryId == null) return null;
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com ID: " + categoryId));
        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Você não tem permissão para usar esta categoria.");
        }
        return category;
    }

    @Transactional
    public FinancialTransaction create(FinancialTransactionRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + currentUserId));

        FinancialTransaction transaction = FinancialTransaction.builder()
                .descricao(request.descricao())
                .valor(request.valor())
                .tipo(request.tipo())
                .status(request.status() != null ? request.status() : TransactionStatus.CONCLUIDA)
                .data(request.data())
                .category(resolveCategory(request.categoryId(), currentUserId))
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
        existingTx.setStatus(request.status() != null ? request.status() : TransactionStatus.CONCLUIDA);
        existingTx.setData(request.data());
        existingTx.setCategory(resolveCategory(request.categoryId(), existingTx.getUser().getId()));

        return transactionRepository.save(existingTx);
    }

    /** Marca uma pendência como concluída (usado no botão "Confirmar" de contas a
     *  pagar/receber) sem precisar reenviar o resto dos campos da transação. */
    @Transactional
    public FinancialTransaction markAsConcluded(Long id) {
        FinancialTransaction existingTx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com ID: " + id));

        SecurityUtils.assertOwnership(existingTx.getUser().getId());
        existingTx.setStatus(TransactionStatus.CONCLUIDA);
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
