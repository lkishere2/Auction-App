package com.auction.app.transaction;

import com.auction.app.domains.transaction.model.Transaction;
import com.auction.app.domains.transaction.TransactionRepository;
import com.auction.app.domains.transaction.model.TransactionStatus;
import com.auction.app.domains.transaction.model.TransactionType;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User client;
    private User otherClient;
    private Transaction deposit;
    private Transaction withdrawal;

    @BeforeEach
    void setUp() {
        client = userRepository.saveAndFlush(createUser("client", "client@example.com"));
        otherClient = userRepository.saveAndFlush(createUser("other", "other@example.com"));

        deposit = transactionRepository.saveAndFlush(createTransaction(
                client,
                "100.00",
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING
        ));
        withdrawal = transactionRepository.saveAndFlush(createTransaction(
                client,
                "25.00",
                TransactionType.WITHDRAWAL,
                TransactionStatus.SUCCESS
        ));
        transactionRepository.saveAndFlush(createTransaction(
                otherClient,
                "75.00",
                TransactionType.DEPOSIT,
                TransactionStatus.PENDING
        ));
    }

    // =========================================================================
    // METHOD 1: getTransactionByUserId (6 Tests)
    // =========================================================================

    // --- Happy Paths (3 Tests) ---

    @Test
    void getTransactionByUserId_WhenUserHasTransactions_ShouldReturnOnlyThatUsersTransactions() {
        Page<Transaction> result = transactionRepository.getTransactionByUserId(
                PageRequest.of(0, 10),
                client.getId()
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(transaction -> transaction.getUser().getEmail())
                .containsOnly("client@example.com");
    }

    @Test
    void getTransactionByUserId_WhenPageSizeIsOne_ShouldReturnOneTransactionAndTotalCount() {
        Page<Transaction> result = transactionRepository.getTransactionByUserId(
                PageRequest.of(0, 1),
                client.getId()
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getTransactionByUserId_WhenSecondPageRequested_ShouldReturnRemainingTransaction() {
        Page<Transaction> result = transactionRepository.getTransactionByUserId(
                PageRequest.of(1, 1),
                client.getId()
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void getTransactionByUserId_WhenUserDoesNotExist_ShouldReturnEmptyPage() {
        Page<Transaction> result = transactionRepository.getTransactionByUserId(
                PageRequest.of(0, 10),
                999L
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getTransactionByUserId_WhenUserIdIsNull_ShouldReturnEmptyPage() {
        Page<Transaction> result = transactionRepository.getTransactionByUserId(
                PageRequest.of(0, 10),
                null
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getTransactionByUserId_WhenPageIsPastLastPage_ShouldReturnEmptyContentWithTotalCount() {
        Page<Transaction> result = transactionRepository.getTransactionByUserId(
                PageRequest.of(5, 10),
                client.getId()
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // =========================================================================
    // METHOD 2: inherited save/findById behavior for Transaction entity (6 Tests)
    // =========================================================================

    // --- Happy Paths (3 Tests) ---

    @Test
    void save_WhenStatusIsNotProvided_ShouldDefaultToPending() {
        Transaction transaction = Transaction.builder()
                .user(client)
                .amount(new BigDecimal("10.00"))
                .type(TransactionType.DEPOSIT)
                .build();

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void save_WhenTransactionIsPersisted_ShouldSetCreatedAt() {
        assertThat(deposit.getCreatedAt()).isNotNull();
    }

    @Test
    void findById_WhenTransactionExists_ShouldReturnTransaction() {
        Transaction result = transactionRepository.findById(deposit.getId()).orElseThrow();

        assertThat(result.getAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    // --- Edge Cases (3 Tests) ---

    @Test
    void findById_WhenTransactionDoesNotExist_ShouldReturnEmptyOptional() {
        assertThat(transactionRepository.findById(999L)).isEmpty();
    }

    @Test
    void save_WhenAmountIsNull_ShouldViolateNotNullConstraint() {
        Transaction transaction = createTransaction(client, "10.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);
        transaction.setAmount(null);

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(transaction))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_WhenUserIsNull_ShouldViolateNotNullConstraint() {
        Transaction transaction = createTransaction(null, "10.00", TransactionType.DEPOSIT, TransactionStatus.PENDING);

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(transaction))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .password("encodedPassword")
                .enabled(true)
                .build();
    }

    private Transaction createTransaction(User user, String amount, TransactionType type, TransactionStatus status) {
        return Transaction.builder()
                .user(user)
                .amount(new BigDecimal(amount))
                .type(type)
                .status(status)
                .build();
    }
}
