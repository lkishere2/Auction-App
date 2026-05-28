package com.auction.app.domains.users.connection;

import com.auction.app.domains.users.connection.model.Connection;
import com.auction.app.domains.users.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    @Query("SELECT c.follower FROM Connection c WHERE c.following.id = :followingId")
    List<User> findAllFollowersByFollowingId(@Param("followingId") Long followingId);

    Optional<Connection> findByFollowerIdAndFollowingId(long followerId, long followingId);

    long countByFollower_Id(long userId);

    long countByFollowing_Id(long userId);
}
