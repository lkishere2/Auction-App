package com.auction.app.domains.users;

import jakarta.transaction.Transactional;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service

public class UserServiceImpl implements  UserService {


@Autowired
private UserRepository  userRepository;
@Override
public UserResponse getUserInfo(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Can't find User with ID: " + id));

    // Đổ dữ liệu ra Response
    UserResponse response = new UserResponse();
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setBalance(user.getBalance());

    return response;

}

@Override
@Transactional // data save safely
public UserResponse updateUser(UserRequest userRequest) {
        // use ID from request to find user
        User user = userRepository.findById(userRequest.getId())
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        // update information , id,and balance can't be here
        // check name , if not null , set name , if null ,name unchanged
        if (userRequest.getUsername() != null && !userRequest.getUsername().isEmpty()) {
        user.setUsername(userRequest.getUsername());
        }

        //check email if not null , set email , if null , name changed
        if (userRequest.getEmail() != null && !userRequest.getEmail().isEmpty()) {
        user.setEmail(userRequest.getEmail());
        }

        // save
        userRepository.save(user);

        // return latest version for client
        UserResponse response = new UserResponse();
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setBalance(user.getBalance());

        return response;
    }
}
