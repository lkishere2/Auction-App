package com.auction.app.domains.users.users;

import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getCurrentUserInfo() {
        User currentUser = getCurrentUser();

        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(currentUser.getUsername());
        userResponse.setEmail(currentUser.getEmail());
        userResponse.setBalance(currentUser.getBalance());
        return userResponse;
    }

    @Override
    @Transactional
    public void updateUsername(UsernameRequest usernameRequest) {
        User currentUser = getCurrentUser();
        userRepository.updateUsername(currentUser.getId(), usernameRequest.getUsername());
    }

    @Override
    @Transactional
    public void updateEmail(EmailRequest emailRequest) {
        User currentUser = getCurrentUser();

        String newEmail = emailRequest.getEmail();

        if (newEmail.equals(currentUser.getEmail())) {
            throw new RuntimeException("New email must be different from current email");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email is already in use");
        }

        userRepository.updateEmail(currentUser.getId(), newEmail);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordRequest passwordRequest) {
        User currentUser = getCurrentUser();

        if (!passwordEncoder.matches(passwordRequest.getCurrentPassword(), currentUser.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (passwordRequest.getCurrentPassword().equals(passwordRequest.getNewPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        userRepository.updatePassword(currentUser.getId(), passwordEncoder.encode(passwordRequest.getNewPassword()));
    }

    @Override
    public Page<UserResponse> getAllUsers(int page, int size) {
        // Create pagination request
        PageRequest pageRequest = PageRequest.of(page, size);

        // Fetch users from repo and map Entity to DTO (UserResponse)
        return userRepository.findAll(pageRequest).map(user -> {
            UserResponse response = new UserResponse();
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setBalance(user.getBalance());
            return response;
        });
    }

    @Override
    @Transactional
    public void disableUser(Long id) {
        User currentUser = getCurrentUser();

        // Prevent admin from disabling their own account
        if (currentUser.getId().equals(id)) {
            throw new RuntimeException("You cannot disable your own account");
        }

        User userToDisable = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        userToDisable.setRole(Role.DISABLE);
        userRepository.save(userToDisable);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}