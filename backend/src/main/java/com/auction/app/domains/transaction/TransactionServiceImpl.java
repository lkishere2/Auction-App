package com.auction.app.domains.transaction;

import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.domains.transaction.dtos.ClientRequest;
import com.auction.app.domains.transaction.dtos.TransactionRequest;
import com.auction.app.domains.transaction.dtos.TransactionResponse;
import com.auction.app.domains.transaction.exceptions.UnauthorizedTransactionException;
import com.auction.app.domains.transaction.exceptions.InsufficientFundsException;
import com.auction.app.domains.transaction.exceptions.TransactionNotFoundException;
import com.auction.app.domains.transaction.exceptions.InvalidTransactionStateException;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
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
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + id));

        if (!transaction.getUser().getId().equals(currentUser().getId())) {
            throw new UnauthorizedTransactionException("Access denied: You do not own this transaction resource.");
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
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + clientRequest.getTransactionId()));

        User user = userRepository.findById(clientRequest.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + clientRequest.getUserId()));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException("Action rejected: Only pending transactions can be accepted.");
        }

        BigDecimal currentBalance = user.getBalance();
        if (clientRequest.getType().equals(TransactionType.DEPOSIT)) {
            user.setBalance(currentBalance.add(clientRequest.getAmount()));
        } else {
            if (currentBalance.compareTo(clientRequest.getAmount()) < 0) {
                throw new InsufficientFundsException("Transaction failed: Insufficient wallet balance for withdrawal.");
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
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + id));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException("Action rejected: Only pending transactions can be cancelled.");
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }

    // Helpers
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new BadCredentialsException("User session is invalid or expired.");
        }
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
                .status(TransactionStatus.PENDING) // Explicitly setting state to PENDING on birth
                .build();
    }
}