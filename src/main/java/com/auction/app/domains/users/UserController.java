package com.auction.app.domains.users;

import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/information/")
    public UserResponse getCurrentUserInformation() {
        return userService.getCurrentUserInfo();
    }

    @PatchMapping("/updateName/") //update name
    public UserResponse updateProfile(@RequestBody UsernameRequest  userRequest){
        return userService.updateUsername(userRequest);
    }

    @PatchMapping("/updateEmail/")
    public UserResponse updateEmail(@RequestBody EmailRequest  userRequest){
        return userService.updateEmail(userRequest);

    }
    @PatchMapping("/udatePassword")
    public UserResponse updatePassword(@RequestBody PasswordRequest  userRequest){
        return userService.updatePassword(userRequest);
    }

}
