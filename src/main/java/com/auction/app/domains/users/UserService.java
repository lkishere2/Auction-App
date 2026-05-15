package com.auction.app.domains.users;

public interface UserService {
    UserResponse getCurrentUserInfo();
    UserResponse updateUsername(UsernameRequest usernameRequest);
    UserResponse updateEmail(EmailRequest emailRequest);
    UserResponse updatePassword(PasswordRequest passwordRequest);
}
