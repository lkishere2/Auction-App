package com.auction.app.transaction;

import com.auction.app.domains.transaction.Transaction;
import com.auction.app.domains.transaction.TransactionRepository;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import org.aspectj.apache.bcel.classfile.Code;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class TransactionRepositoryTests {

    @Autowired
    private TransactionRepository transactionRepository;
    private UserRepository userRepository;

    private User WhiteMouse;

    @BeforeEach
    void setUp() {
        WhiteMouse = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("securePassword123")
                .enabled(true)
                .build();

        WhiteMouse = userRepository.saveAndFlush(WhiteMouse);


    }
}

