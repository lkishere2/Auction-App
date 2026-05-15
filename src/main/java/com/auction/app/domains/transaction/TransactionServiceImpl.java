package com.auction.app.domains.transaction;

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
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.getTransactionByUserId(pageable, currentUser.getId())
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        User currentUser = getCurrentUser();

        Transaction newTransaction = new Transaction();
        newTransaction.setUser(currentUser);
        newTransaction.setAmount(transactionRequest.getAmount());
        newTransaction.setType(transactionRequest.getType());
        newTransaction.setStatus(TransactionStatus.PENDING);

        Transaction saved = transactionRepository.save(newTransaction);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        User currentUser = getCurrentUser();
        if (!transaction.getUser().getId().equals(currentUser.getId())) {
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
    public void deposit(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user balance (Adjust setter/getter based on your User entity)
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(currentBalance.add(amount));
        userRepository.save(user);

        // Create and log the successful deposit transaction
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.DEPOSIT); // Assuming TransactionType enum exists
        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void withdraw(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;

        if (currentBalance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds for withdrawal.");
        }

        user.setBalance(currentBalance.subtract(amount));
        userRepository.save(user);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.WITHDRAWAL);
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

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private ClientRequest mapToClientRequest(Transaction transaction) {
        User user = transaction.getUser();

        return ClientRequest.builder()
                .userId(user.getId())
                .username(user.getDisplayName())
                .email(user.getEmail())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}