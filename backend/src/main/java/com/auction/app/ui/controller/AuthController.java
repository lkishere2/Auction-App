package com.auction.app.ui.controller;

import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import com.auction.app.infrastructure.config.SpringContext;
import com.auction.app.security.LoggedUser;
import com.auction.app.ui.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

public class AuthController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private void onLogin() {
        String email = usernameField.getText();
        String password = passwordField.getText();

        try {
            AuthenticationManager authenticationManager = SpringContext.getBean(AuthenticationManager.class);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            UserRepository repo = SpringContext.getBean(UserRepository.class);
            java.util.Optional<User> found = repo.findByEmail(email);
            if (found.isPresent()) {
                User user = found.get();
                if (!user.isEnabled()) {
                    // still show auth page in real app would show verification prompt; here return
                    return;
                }
                LoggedUser.set(user);
                SceneManager.showDashboard();
                return;
            }
        } catch (AuthenticationException ae) {
            // authentication failed; in real UI show message
            ae.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback: show auth again
        try { SceneManager.showAuth(); } catch (Exception ignored) {}
    }

    @FXML
    private void onBack() {
        try {
            SceneManager.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
