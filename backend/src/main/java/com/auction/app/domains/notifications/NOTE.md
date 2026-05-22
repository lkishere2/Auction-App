# The process of following:

User B -------------> POST /api/v1/users/follow/{userA_id} -------------> BE Controller
                                                                                |
                                                                                ▼ 
User A <--- WS push /user/queue/notifications <--- Trigger notification  <--- Toggle DB

- When app loads, both User A and User B connect to WS and immediately sub to unique queue /user/queue/notifications
- User B click "Follow" on User A profile. The FE sends a REST request POST /api/v1/users/follow/{userA_id}
- The ConnectionService save the entity to DB, then it hands off the control to NotificationService
- The NotificationService will then create a new entity and save to DB
- After that, it will call message template and send the message
  messagingTemplate.convertAndSendToUser(
  User A name,
  "/queue/notifications",
  response
  );
- Because Spring STOMP broker knows User A destination, it pushes the payload to User A browser

# The process when User A created new auction:

User A ------------> POST /api/v1/auctions -------------------------------> BE Controller
                                                                                    |
                                                                                    ▼ 
Loops through to every follower's WS <---- Find all followers (B, C, D) <----- Save auction to DB

- User A completes a form and clicks "Publish Auction". The FE sends a REST request: POST /api/v1/auctions containing the auction details.
- The AuctionService saves the new auction to the database.
- The service looks up to the connection table and check all the records that satisfy following_id = User A id
- And then call the NotificationService and send to all followers