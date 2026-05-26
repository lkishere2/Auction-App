package com.auction.app.transaction;

import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.domains.transaction.dtos.ClientRequest;
import com.auction.app.domains.transaction.model.Transaction;
import com.auction.app.domains.transaction.TransactionRepository;
import com.auction.app.domains.transaction.dtos.TransactionRequest;
import com.auction.app.domains.transaction.dtos.TransactionResponse;
import com.auction.app.domains.transaction.TransactionServiceImpl;
import com.auction.app.domains.transaction.model.TransactionStatus;
import com.auction.app.domains.transaction.model.TransactionType;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = createUser(1L, "client", "client@example.com", "100.00");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // METHOD 1: getUserTransaction (5 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void getUserTransaction_WhenCalled_ShouldQueryCurrentUserTransactions() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        when(transactionRepository.getTransactionByUserId(any(Pageable.class), eq(1L)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        List<TransactionResponse> responses = transactionService.getUserTransaction(0, 10).getContent();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAmount()).isEqualByComparingTo("50.00");
        assertThat(responses.get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(responses.get(0).getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void getUserTransaction_WhenNoTransactions_ShouldReturnEmptyPage() {
        when(transactionRepository.getTransactionByUserId(any(Pageable.class), eq(1L)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<TransactionResponse> responses = transactionService.getUserTransaction(0, 10);

        assertThat(responses.getContent()).isEmpty();
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void getUserTransaction_WhenPageIsNegative_ShouldThrowBeforeRepositoryCall() {
        assertThatThrownBy(() -> transactionService.getUserTransaction(-1, 10))
                .isInstanceOf(IllegalArgumentException.class);

        verify(transactionRepository, never()).getTransactionByUserId(any(), any());
    }

    @Test
    void getUserTransaction_WhenSizeIsZero_ShouldThrowBeforeRepositoryCall() {
        assertThatThrownBy(() -> transactionService.getUserTransaction(0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verify(transactionRepository, never()).getTransactionByUserId(any(), any());
    }

    @Test
    void getUserTransaction_WhenAuthenticationIsMissing_ShouldThrowException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> transactionService.getUserTransaction(0, 10))
                .isInstanceOf(NullPointerException.class);
    }

    // =========================================================================
    // METHOD 2: createTransaction (5 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void createTransaction_WhenDepositRequestIsValid_ShouldSaveTransactionForCurrentUser() {
        TransactionRequest request = createTransactionRequest("25.00", TransactionType.DEPOSIT);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();

        assertThat(savedTransaction.getUser()).isEqualTo(currentUser);
        assertThat(savedTransaction.getAmount()).isEqualByComparingTo("25.00");
        assertThat(savedTransaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void createTransaction_WhenWithdrawalBoundaryAmount_ShouldSaveTransactionForCurrentUser() {
        TransactionRequest request = createTransactionRequest("0.01", TransactionType.WITHDRAWAL);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getAmount()).isEqualByComparingTo("0.01");
        assertThat(response.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void createTransaction_WhenRequestIsNull_ShouldThrowException() {
        assertThatThrownBy(() -> transactionService.createTransaction(null))
                .isInstanceOf(NullPointerException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WhenAmountIsNull_CurrentServiceStillPassesNullAmountToRepository() {
        TransactionRequest request = createTransactionRequest(null, TransactionType.DEPOSIT);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getAmount()).isNull();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WhenTypeIsNull_CurrentServiceStillPassesNullTypeToRepository() {
        TransactionRequest request = createTransactionRequest("10.00", null);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getType()).isNull();
        verify(transactionRepository).save(any(Transaction.class));
    }

    // =========================================================================
    // METHOD 3: deleteTransaction (4 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void deleteTransaction_WhenTransactionBelongsToCurrentUser_ShouldDeleteTransaction() {
        Transaction transaction = createTransaction(10L, currentUser, "25.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(10L);

        verify(transactionRepository).delete(transaction);
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void deleteTransaction_WhenTransactionDoesNotExist_ShouldThrowTransactionNotFoundException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(99L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction not found");

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransaction_WhenTransactionBelongsToAnotherUser_ShouldThrowAuthorizedException() {
        User otherUser = createUser(2L, "other", "other@example.com", "100.00");
        Transaction transaction = createTransaction(10L, otherUser, "25.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.deleteTransaction(10L))
                .isInstanceOf(AuthorizedException.class)
                .hasMessage("Unauthorized: You do not make this transaction.");

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransaction_WhenTransactionOwnerIdIsNull_ShouldThrowNullPointerException() {
        User ownerWithoutId = createUser(null, "other", "other@example.com", "100.00");
        Transaction transaction = createTransaction(10L, ownerWithoutId, "25.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.deleteTransaction(10L))
                .isInstanceOf(NullPointerException.class);
    }

    // =========================================================================
    // METHOD 4: getAllTransactionRequest (4 Tests)
    // =========================================================================

    // --- Happy Paths (2 Tests) ---

    @Test
    void getAllTransactionRequest_WhenTransactionsExist_ShouldReturnClientRequestPage() {
        Transaction transaction = createTransaction(10L, currentUser, "100.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));

        List<ClientRequest> responses = transactionService.getAllTransactionRequest(0, 10).getContent();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTransactionId()).isEqualTo(10L);
        assertThat(responses.get(0).getUserId()).isEqualTo(1L);
        assertThat(responses.get(0).getUsername()).isEqualTo("client");
    }

    @Test
    void getAllTransactionRequest_WhenNoTransactions_ShouldReturnEmptyPage() {
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Page<ClientRequest> responses = transactionService.getAllTransactionRequest(0, 10);

        assertThat(responses.getContent()).isEmpty();
    }

    // --- Edge Cases (2 Tests) ---

    @Test
    void getAllTransactionRequest_WhenPageIsNegative_ShouldThrowBeforeRepositoryCall() {
        assertThatThrownBy(() -> transactionService.getAllTransactionRequest(-1, 10))
                .isInstanceOf(IllegalArgumentException.class);

        verify(transactionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllTransactionRequest_WhenSizeIsZero_ShouldThrowBeforeRepositoryCall() {
        assertThatThrownBy(() -> transactionService.getAllTransactionRequest(0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verify(transactionRepository, never()).findAll(any(Pageable.class));
    }

    // =========================================================================
    // METHOD 5: acceptTransaction (11 Tests)
    // =========================================================================

    // --- Happy Paths (3 Tests) ---

    @Test
    void acceptTransaction_WhenDepositIsPending_ShouldIncreaseBalanceAndMarkSuccess() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        ClientRequest request = createClientRequest(10L, 1L, "50.00", TransactionType.DEPOSIT);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        transactionService.acceptTransaction(request);

        assertThat(currentUser.getBalance()).isEqualByComparingTo("150.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(userRepository).save(currentUser);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void acceptTransaction_WhenWithdrawalIsPendingAndBalanceIsGreaterThanAmount_ShouldDecreaseBalanceAndMarkSuccess() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.WITHDRAWAL, TransactionStatus.PENDING);
        ClientRequest request = createClientRequest(10L, 1L, "40.00", TransactionType.WITHDRAWAL);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        transactionService.acceptTransaction(request);

        assertThat(currentUser.getBalance()).isEqualByComparingTo("60.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void acceptTransaction_WhenWithdrawalAmountEqualsBalance_ShouldSetBalanceToZeroAndMarkSuccess() {
        Transaction transaction = createTransaction(10L, currentUser, "100.00", TransactionType.WITHDRAWAL, TransactionStatus.PENDING);
        ClientRequest request = createClientRequest(10L, 1L, "100.00", TransactionType.WITHDRAWAL);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        transactionService.acceptTransaction(request);

        assertThat(currentUser.getBalance()).isEqualByComparingTo("0.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    // --- Edge Cases (8 Tests) ---

    @Test
    void acceptTransaction_WhenRequestIsNull_ShouldThrowException() {
        assertThatThrownBy(() -> transactionService.acceptTransaction(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptTransaction_WhenTransactionDoesNotExist_ShouldThrowTransactionNotFoundException() {
        ClientRequest request = createClientRequest(99L, 1L, "50.00", TransactionType.DEPOSIT);
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void acceptTransaction_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
        ClientRequest request = createClientRequest(10L, 99L, "50.00", TransactionType.DEPOSIT);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.PENDING)));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void acceptTransaction_WhenTransactionIsSuccess_ShouldThrowTransactionNotPendingException() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.SUCCESS);
        ClientRequest request = createClientRequest(10L, 1L, "50.00", TransactionType.DEPOSIT);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(TransactionNotPendingException.class)
                .hasMessage("Only pending transactions can be accepted.");
    }

    @Test
    void acceptTransaction_WhenTransactionIsFailed_ShouldThrowTransactionNotPendingException() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.FAILED);
        ClientRequest request = createClientRequest(10L, 1L, "50.00", TransactionType.DEPOSIT);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(TransactionNotPendingException.class);
    }

    @Test
    void acceptTransaction_WhenWithdrawBalanceIsInsufficient_ShouldThrowPoorException() {
        Transaction transaction = createTransaction(10L, currentUser, "150.00", TransactionType.WITHDRAWAL, TransactionStatus.PENDING);
        ClientRequest request = createClientRequest(10L, 1L, "150.00", TransactionType.WITHDRAWAL);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(PoorException.class)
                .hasMessage("Insufficient funds for withdrawal.");

        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void acceptTransaction_WhenAmountIsNull_ShouldThrowException() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        ClientRequest request = createClientRequest(10L, 1L, null, TransactionType.DEPOSIT);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptTransaction_WhenTypeIsNull_ShouldThrowException() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        ClientRequest request = createClientRequest(10L, 1L, "50.00", null);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> transactionService.acceptTransaction(request))
                .isInstanceOf(NullPointerException.class);
    }

    // =========================================================================
    // METHOD 6: cancelTransaction (5 Tests)
    // =========================================================================

    // --- Happy Path (1 Test) ---

    @Test
    void cancelTransaction_WhenTransactionIsPending_ShouldMarkFailed() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        transactionService.cancelTransaction(10L);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(transactionRepository).save(transaction);
    }

    // --- Edge Cases (4 Tests) ---

    @Test
    void cancelTransaction_WhenTransactionDoesNotExist_ShouldThrowTransactionNotFoundException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.cancelTransaction(99L))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void cancelTransaction_WhenTransactionIsSuccess_ShouldThrowTransactionNotPendingException() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.SUCCESS);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.cancelTransaction(10L))
                .isInstanceOf(TransactionNotPendingException.class)
                .hasMessage("Only pending transactions can be cancelled.");
    }

    @Test
    void cancelTransaction_WhenTransactionIsFailed_ShouldThrowTransactionNotPendingException() {
        Transaction transaction = createTransaction(10L, currentUser, "50.00", TransactionType.DEPOSIT, TransactionStatus.FAILED);
        when(transactionRepository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.cancelTransaction(10L))
                .isInstanceOf(TransactionNotPendingException.class);
    }

    @Test
    void cancelTransaction_WhenIdIsNull_ShouldThrowTransactionNotFoundException() {
        when(transactionRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.cancelTransaction(null))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    private User createUser(Long id, String username, String email, String balance) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("password")
                .balance(new BigDecimal(balance))
                .build();
    }

    private TransactionRequest createTransactionRequest(String amount, TransactionType type) {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(amount == null ? null : new BigDecimal(amount));
        request.setType(type);
        return request;
    }

    private ClientRequest createClientRequest(Long transactionId, Long userId, String amount, TransactionType type) {
        return ClientRequest.builder()
                .transactionId(transactionId)
                .userId(userId)
                .amount(amount == null ? null : new BigDecimal(amount))
                .type(type)
                .build();
    }

    private Transaction createTransaction(Long id, User user, String amount, TransactionType type, TransactionStatus status) {
        return Transaction.builder()
                .id(id)
                .user(user)
                .amount(new BigDecimal(amount))
                .type(type)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
