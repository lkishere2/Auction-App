package com.auction.app.domains.users.users;

import com.auction.app.domains.users.users.dtos.EmailRequest;
import com.auction.app.domains.users.users.dtos.PasswordRequest;
import com.auction.app.domains.users.users.dtos.ProfileImageRequest;
import com.auction.app.domains.users.users.dtos.UserResponse;
import com.auction.app.domains.users.users.dtos.UsernameRequest;
import org.springframework.data.domain.Page;

public interface UserService {
    UserResponse getCurrentUserInfo();
    void updateUsername(UsernameRequest usernameRequest);
    void updateEmail(EmailRequest emailRequest);
    void updatePassword(PasswordRequest passwordRequest);
    void updateProfileImage(ProfileImageRequest profileImageRequest);
    Page<UserResponse> getAllUsers(int page, int size);
    void disableUser(Long id);
}