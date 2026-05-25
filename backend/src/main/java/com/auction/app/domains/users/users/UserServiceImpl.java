package com.auction.app.domains.users.users;

import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.domains.users.exceptions.InvalidPasswordException;
import com.auction.app.domains.users.exceptions.InvalidUserStateException;
import com.auction.app.domains.users.exceptions.UserUpdateException;
import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
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
        User user = currentUser();
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void updateUsername(UsernameRequest usernameRequest) {
        userRepository.updateUsername(currentUser().getId(), usernameRequest.getUsername());
    }

    @Override
    @Transactional
    public void updateEmail(EmailRequest emailRequest) {
        String newEmail = emailRequest.getEmail();
        // Valid check
        if (newEmail.equals(currentUser().getEmail())) {
            throw new UserUpdateException("Update failed: New email must be different from current email.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new UserUpdateException("Update failed.");
        }

        userRepository.updateEmail(currentUser().getId(), newEmail);
    }

    @Override
    @Transactional
    public void updatePassword(PasswordRequest passwordRequest) {
        if (!passwordEncoder.matches(passwordRequest.getCurrentPassword(), currentUser().getPassword())) {
            throw new InvalidPasswordException("Update failed: Current password is incorrect.");
        }
        if (passwordRequest.getCurrentPassword().equals(passwordRequest.getNewPassword())) {
            throw new InvalidPasswordException("Update failed: New password must be different from current password.");
        }

        userRepository.updatePassword(currentUser().getId(), passwordEncoder.encode(passwordRequest.getNewPassword()));
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
        // Prevent admin from disabling their own account
        if (currentUser().getId().equals(id)) {
            throw new InvalidUserStateException("Action rejected: You cannot disable your own account.");
        }

        User userToDisable = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        userToDisable.setRole(Role.DISABLE);
        userRepository.save(userToDisable);
    }

    //HELPERS
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new BadCredentialsException("User session is invalid or expired.");
        }
        return (User) authentication.getPrincipal();
    }

    private UserResponse mapToResponse(User user){
        return UserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .balance(user.getBalance())
                .build();
    }
}