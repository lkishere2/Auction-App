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
                .map(this::mapToResponse); // Đổi tên cho đồng bộ thành toResponse
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        User currentUser = getCurrentUser();

        // Sử dụng hàm helper requestToEntity mới tạo ở dưới
        Transaction newTransaction = requestToEntity(transactionRequest, currentUser);
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
                .map(this::toClientRequest); // Đổi tên cho đồng bộ thành toClientRequest
    }

    @Override
    @Transactional
    public void deposit(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(currentBalance.add(amount));
        userRepository.save(user);

        // Tạo nhanh transaction thành công thông qua hàm helper phụ ở dưới
        Transaction transaction = createSuccessTransaction(user, amount, TransactionType.DEPOSIT);
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

        // Tạo nhanh transaction thành công thông qua hàm helper phụ ở dưới
        Transaction transaction = createSuccessTransaction(user, amount, TransactionType.WITHDRAWAL);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void cancelTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

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

    // ==========================================
    // KHU VỰC CÁC HÀM HELPER / MAPPER XUỐNG DƯỚI
    // ==========================================

    // 1. Hàm helper requestToEntity mà bạn cần
    private Transaction requestToEntity(TransactionRequest request, User user) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setStatus(TransactionStatus.PENDING);
        return transaction;
    }

    // 2. Hàm helper phụ để gom code trùng cho hàm deposit và withdraw
    private Transaction createSuccessTransaction(User user, BigDecimal amount, TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        return transaction;
    }

    // 3. Đổi tên từ mapToResponse sang toResponse cho chuẩn convention ngắn gọn
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    // 4. Đổi tên từ mapToClientRequest sang toClientRequest
    private ClientRequest toClientRequest(Transaction transaction) {
        User user = transaction.getUser();
        return ClientRequest.builder()
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getDisplayName() : null)
                .email(user != null ? user.getEmail() : null)
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}