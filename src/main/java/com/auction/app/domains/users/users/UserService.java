package com.auction.app.domains.users.users;

import org.springframework.data.domain.Page;

public interface UserService {
    UserResponse getCurrentUserInfo();
    void updateUsername(UsernameRequest usernameRequest);
    void updateEmail(EmailRequest emailRequest);
    void updatePassword(PasswordRequest passwordRequest);
    Page<UserResponse> getAllUsers(int page, int size);
    void disableUser(Long id);
}
