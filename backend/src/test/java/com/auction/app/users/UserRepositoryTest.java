package com.auction.app.users;


import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;

import org.assertj.core.error.ShouldBeTrue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class UserRepositoryTest {


    @Autowired
    private UserRepository userRepository;

    private User WhiteMouse;

    @BeforeEach
    void setUp(){
        WhiteMouse = User.builder()
                .username("MickeyMouse")
                .email("aaaa@gmail.com")
                .password("123456")
                .enabled(true)
                .build();

        WhiteMouse = userRepository.save(WhiteMouse);
    }

    //findByEmail
    @Test
    public void findUser_WhenEmailExist_ShouldReturnUser() {
        // Arrange
        String email = "aaaa@gmail.com";

        // Act
        User user = userRepository.findByEmail(email).get();

        // Assert
        Assertions.assertNotNull(user);
        Assertions.assertEquals("MickeyMouse", user.getDisplayName());
    }

    @Test
    public void findUser_WhenEmailNotExist_ShouldReturnNull() {
        // Arrange
        String email = "aaa@gmail.com";

        // Act
        Optional<User> user = userRepository.findByEmail(email);

        // Assert
        Assertions.assertTrue(user.isEmpty());
    }

    @Test
    public void findUser_WhenEmailIsNull_ShouldReturnNull() {
        // Arrange
        String email = null;

        // Act
        Optional<User> user = userRepository.findByEmail(email);

        // Assert
        Assertions.assertTrue(user.isEmpty());
    }

    //existsByEmail
    @Test
    public void checkUser_whenEmailIsTrue_ShouldReturnTrue(){
        String email = "aaaa@gmail.com";
        boolean check = userRepository.existsByEmail(email);
        Assertions.assertTrue(check);
    }
    @Test
    public void checkUser_whenEmailIsFalse_ShouldReturnFalse(){
        String email = "aaa@gmail.com";
        boolean check = userRepository.existsByEmail(email);
        Assertions.assertFalse(check);
    }
    @Test
    public void checkUser_whenEmailIsNull_ShouldReturnFalse(){
        String email = "";
        boolean check = userRepository.existsByEmail(email);
        Assertions.assertFalse(check);
    }


    //updateUsername
    @Autowired
    private TestEntityManager entityManager;

    @Test
    public void updateUserName_WhenIdIsTrue_ShouldUpdateSucessfully(){
        Long id = WhiteMouse.getId();
        String newName = "Whut??";

        userRepository.updateUsername(id, newName);

        entityManager.clear();
        Optional<User> updatedMouse = userRepository.findById(id);

        Assertions.assertTrue(updatedMouse.isPresent());
        Assertions.assertEquals(newName, updatedMouse.get().getDisplayName());
    }

    @Test
    public void updateUsername_WhenIdDoesNotExist_ShouldNotAffectAnyUser(){
        Long fakeId = 999L;
        String newName = "Whut??";

        userRepository.updateUsername(fakeId, newName);

        Assertions.assertDoesNotThrow(() -> {
            userRepository.updateUsername(fakeId, newName);
        });
    }

    //Update Email
    @Test
    public void updateEmail_WhenIdIsTrue_ShouldUpdateSucessfully(){
        Long id = WhiteMouse.getId();
        String newEmail = "newemail@gmail.com";

        userRepository.updateEmail(id, newEmail);

        entityManager.clear();
        Optional<User> updatedMouse = userRepository.findById(id);

        Assertions.assertTrue(updatedMouse.isPresent());
        Assertions.assertEquals(newEmail, updatedMouse.get().getEmail());
    }

    @Test
    public void updateEmail_WhenIdDoesNotExist_ShouldNotAffectAnyUser(){
        Long fakeId = 999L;
        String newEmail = "newemail@gmail.com";

        userRepository.updateEmail(fakeId, newEmail);

        Assertions.assertDoesNotThrow(() -> {
            userRepository.updateUsername(fakeId, newEmail);
        });
    }

    //update password
    @Test
    public void updatePassword_WhenIdIsTrue_ShouldUpdateSucessfully(){
        Long id = WhiteMouse.getId();
        String newPassword = "dhdshajdas";

        userRepository.updatePassword(id, newPassword);

        entityManager.clear();
        Optional<User> updatedMouse = userRepository.findById(id);

        Assertions.assertTrue(updatedMouse.isPresent());
        Assertions.assertEquals(newPassword, updatedMouse.get().getPassword());
    }

    @Test
    public void updatePassword_WhenIdDoesNotExist_ShouldNotAffectAnyUser(){
        Long fakeId = 999L;
        String newPassword = "dhdshajdas";

        userRepository.updatePassword(fakeId, newPassword);


        Assertions.assertDoesNotThrow(() -> {
            userRepository.updateUsername(fakeId, newPassword);
        });
    }
}
