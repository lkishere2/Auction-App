package com.auction.app.domains.users;

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

    @GetMapping("/{id}")// get the information where id is , is only C
    // get user information(name , email , balance)
    public UserResponse getProfile(@PathVariable long id){
        return userService.getUserInfo(id);
    }
    @PostMapping("/update/") //add path , can change the data
    public UserResponse updateProfile(@RequestBody UserRequest  userRequest){
        return userService.updateUser(userRequest);
    }
}
