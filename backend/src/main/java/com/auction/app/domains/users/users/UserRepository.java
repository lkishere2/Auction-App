package com.auction.app.domains.users.users;

import com.auction.app.domains.users.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.username = :username WHERE u.id = :id")
    void updateUsername(@Param("id") Long id, @Param("username") String username);

    @Modifying
    @Query("UPDATE User u SET u.email = :email WHERE u.id = :id")
    void updateEmail(@Param("id") Long id, @Param("email") String email);

    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    void updatePassword(@Param("id") Long id, @Param("password") String password);

    @Modifying
    @Query("UPDATE User u SET u.profileImageUrl = :profileImageUrl WHERE u.id = :id")
    void updateProfileImageUrl(@Param("id") Long id, @Param("profileImageUrl") String profileImageUrl);

}