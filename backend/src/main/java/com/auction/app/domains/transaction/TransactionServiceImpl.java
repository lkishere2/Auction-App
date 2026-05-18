package com.auction.app.domains.transaction;

import com.auction.app.domains.transaction.exceptions.TransactionNotFoundException;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Page<TransactionResponse> getUserTransaction(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.getTransactionByUserId(pageable, currentUser().getId())
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        Transaction newTransaction = mapToEntity(transactionRequest);
        return mapToResponse(transactionRepository.save(newTransaction));
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(currentUser().getId())) {
            throw new RuntimeException("Unauthorized: You do not make this transaction.");
        }

        transactionRepository.delete(transaction);
    }

    @Override
    public Page<ClientRequest> getAllTransactionRequest(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findAll(pageable)
                .map(this::mapToClientRequest);
    }

    @Override
    @Transactional
    public void acceptTransaction(ClientRequest clientRequest) {
        Transaction transaction = transactionRepository.findById(clientRequest.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        User user = userRepository.findById(clientRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Only pending transactions can be accepted.");
        }

        BigDecimal currentBalance = user.getBalance();
        if (clientRequest.getType().equals(TransactionType.DEPOSIT)) {
            user.setBalance(currentBalance.add(clientRequest.getAmount()));
        }
        else {
            if (currentBalance.compareTo(clientRequest.getAmount()) < 0) {
                throw new RuntimeException("Insufficient funds for withdrawal.");
            }
            user.setBalance(currentBalance.subtract(clientRequest.getAmount()));
        }

        userRepository.save(user);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void cancelTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // Only PENDING transactions should typically be cancellable
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Only pending transactions can be cancelled.");
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }

    // Helpers
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
    // send to user
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
    //send to admin
    private ClientRequest mapToClientRequest(Transaction transaction) {
        User user = transaction.getUser();

        return ClientRequest.builder()
                .transactionId(transaction.getId())
                .userId(user.getId())
                .username(user.getDisplayName())
                .email(user.getEmail())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private Transaction mapToEntity(TransactionRequest request) {
        return Transaction.builder()
                .user(currentUser())
                .amount(request.getAmount())
                .type(request.getType())
                .build();
    }
}