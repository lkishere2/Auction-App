package com.auction.app.domains.users;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserResponse getCurrentUserInfo() {
        // Get the current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Return the response
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(currentUser.getUsername());
        userResponse.setEmail(currentUser.getEmail());
        userResponse.setBalance(currentUser.getBalance());
        return userResponse;
    }

    @Override
    @Transactional
    public UserResponse updateUsername(UsernameRequest usernameRequest) {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Set username for them
        currentUser.setUsername(usernameRequest.getUsername());
        userRepository.save(currentUser);

        // Return the response
        UserResponse userResponse = new UserResponse();
        userResponse.setUsername(currentUser.getUsername());
        userResponse.setEmail(currentUser.getEmail());
        userResponse.setBalance(currentUser.getBalance());
        return userResponse;
    }
    @Override
    @Transactional
    public UserResponse updateEmail(EmailRequest emailRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        UserResponse userResponse = new UserResponse();
        currentUser.setEmail(emailRequest.getEmail());
        userRepository.save(currentUser);
        userResponse.setUsername(currentUser.getUsername());
        userResponse.setBalance(currentUser.getBalance());
        userResponse.setEmail(currentUser.getEmail());
        return userResponse;
    }
    @Override
    @Transactional
    public UserResponse updatePassword(PasswordRequest passwordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        UserResponse userResponse = new UserResponse();
        currentUser.setPassword(passwordRequest.getPassword());
        userRepository.save(currentUser);
        userResponse.setUsername(currentUser.getUsername());
        userResponse.setBalance(currentUser.getBalance());
        userResponse.setEmail(currentUser.getEmail());
        return userResponse;
    }







}
