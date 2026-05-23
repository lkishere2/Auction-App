package com.auction.app.domains.users.connection;

import com.auction.app.domains.auth.exceptions.UserNotFoundException;
import com.auction.app.domains.feedback.exceptions.UnauthorizedException;
import com.auction.app.domains.notifications.NotificationService;
import com.auction.app.domains.notifications.NotificationType;
import com.auction.app.domains.users.connection.exceptions.SelfFollowException;
import com.auction.app.domains.users.users.User;
import com.auction.app.domains.users.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConnectionServiceImpl implements ConnectionService {

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public String toggleFollow(Long followingId) {
        Long followerId = currentUser().getId();

        if (followerId.equals(followingId)) {
            throw new SelfFollowException("You cannot follow yourself.");
        }

        Optional<Connection> optionalConnection = connectionRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (optionalConnection.isPresent()) {
            // Unfollow
            connectionRepository.delete(optionalConnection.get());
            return "Unfollowed successfully!";
        }
        else {
            // Follow
            User follower = findUserById(followerId);
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
            throw new UserNotFoundException("User not found");
        }

        long followersCount = connectionRepository.countByFollowing_Id(userId);
        long followingCount = connectionRepository.countByFollower_Id(userId);

        return new UserStats(followersCount, followingCount);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized access");
        }
        return (User) authentication.getPrincipal();
    }

}
