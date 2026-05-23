package com.auction.app.domains.users.users;

import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/users")
@Tag(name = "User")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/info")
    public ResponseEntity<UserResponse> getCurrentUserInformation() {
        return ResponseEntity.ok(userService.getCurrentUserInfo());
    }

    @PatchMapping("/update-username")
    public ResponseEntity<Void> updateUsername(@Valid @RequestBody UsernameRequest userRequest) {
        userService.updateUsername(userRequest);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/update-email")
    public ResponseEntity<Void> updateEmail(@Valid @RequestBody EmailRequest userRequest) {
        userService.updateEmail(userRequest);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/update-password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordRequest userRequest) {
        userService.updatePassword(userRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @PatchMapping("/disable/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }
}