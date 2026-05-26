package com.auction.app.domains.users.users;

import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.domains.users.exceptions.InvalidPasswordException;
import com.auction.app.domains.users.exceptions.InvalidUserStateException;
import com.auction.app.domains.users.exceptions.UserUpdateException;
import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import com.auction.app.domains.users.users.model.Role;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.infrastructure.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    @Override
    public UserResponse getCurrentUserInfo() {
        try {
            User user = securityUtils.getCurrentUser();
            return mapToResponse(user);
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }
    }

    @Override
    @Transactional
    public void updateUsername(UsernameRequest usernameRequest) {
        try {
            userRepository.updateUsername(securityUtils.getCurrentUserId(), usernameRequest.getUsername());
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }
    }

    @Override
    @Transactional
    public void updateEmail(EmailRequest emailRequest) {
        String newEmail = emailRequest.getEmail();
        User user;
        try {
            user = securityUtils.getCurrentUser();
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }

        // Valid check
        if (newEmail.equals(user.getEmail())) {
            throw new UserUpdateException("Update failed: New email must be different from current email.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new UserUpdateException("Update failed.");
        }

        userRepository.updateEmail(user.getId(), newEmail);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordRequest passwordRequest) {
        User user;
        try {
            user = securityUtils.getCurrentUser();
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }

        if (!passwordEncoder.matches(passwordRequest.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Update failed: Current password is incorrect.");
        }
        if (passwordRequest.getCurrentPassword().equals(passwordRequest.getNewPassword())) {
            throw new InvalidPasswordException("Update failed: New password must be different from current password.");
        }

        userRepository.updatePassword(user.getId(), passwordEncoder.encode(passwordRequest.getNewPassword()));
    }

    @Override
    public Page<UserResponse> getAllUsers(int page, int size) {
        // Create pagination request
        PageRequest pageRequest = PageRequest.of(page, size);

        // Fetch users from repo and map Entity to DTO (UserResponse)
        return userRepository.findAll(pageRequest).map(this :: mapToResponse);
    }

    @Override
    @Transactional
    public void disableUser(Long id) {
        try {
            // Prevent admin from disabling their own account
            if (securityUtils.getCurrentUserId().equals(id)) {
                throw new InvalidUserStateException("Action rejected: You cannot disable your own account.");
            }
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }

        User userToDisable = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        userToDisable.setRole(Role.DISABLE);
        userRepository.save(userToDisable);
    }

    //HELPERS
    private UserResponse mapToResponse(User user){
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .balance(user.getBalance())
                .build();
    }
}