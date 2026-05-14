package com.auction.app.domains.users;

public interface UserService {
    UserResponse updateUser(UserRequest  userRequest);
    UserResponse getUserInfo(Long id);

}
