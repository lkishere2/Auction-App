package com.auction.app.domains.users.connection;

import com.auction.app.domains.users.connection.dtos.UserStats;
import com.auction.app.domains.users.connection.model.Connection;
import com.auction.app.domains.users.exceptions.UserNotFoundException;
import com.auction.app.domains.users.exceptions.SelfFollowException;
import com.auction.app.domains.notifications.NotificationService;
import com.auction.app.domains.notifications.model.NotificationType;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import com.auction.app.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public String toggleFollow(Long followingId) {
        Long followerId;
        try {
            followerId = securityUtils.getCurrentUserId();
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }

        if (followerId.equals(followingId)) {
            throw new SelfFollowException("Action rejected: You cannot follow your own account.");
        }

        Optional<Connection> optionalConnection = connectionRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (optionalConnection.isPresent()) {
            // Unfollow
            connectionRepository.delete(optionalConnection.get());
            return "Unfollowed successfully!";
        }
        else {
            // Follow
            User follower;
            try {
                follower = securityUtils.getCurrentUser();
            } catch (IllegalStateException e) {
                throw new BadCredentialsException("User session is invalid or expired.", e);
            }
            User following = findUserById(followingId);
            Connection connection = Connection.builder()
                    .follower(follower)
                    .following(following)
                    .build();
            connectionRepository.save(connection);

            // New step's here! call the notification service
            notificationService.createAndSend(following, follower, NotificationType.FOLLOWING);

            return "Followed successfully";
        }
    }

    @Override
    public UserStats getUserStats(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }

        long followersCount = connectionRepository.countByFollowing_Id(userId);
        long followingCount = connectionRepository.countByFollower_Id(userId);

        return new UserStats(followersCount, followingCount);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }
}