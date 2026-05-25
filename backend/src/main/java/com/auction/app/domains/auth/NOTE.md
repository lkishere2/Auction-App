# The registration
This is explanation of how new user register to our app and how to handle existing user from registering again.
The controller will receive the RegisterRequest containing username, email, and password, and then passes it to the service.
The service will first look up the DB to check if the user has already existed or not, it doesn't matter anyway cause we need to check if they are enabled or not.
[Case 1]: If the user is existed
We first check if they are enabled or not, if they have already enabled then silently return nothing to avoid leaking information.
Why? Because hackers can utilize the information of that email existed or not to brute force the verification code.
If they are not enabled, then we simply generate a new code for them to verify.
We first save some new info like username, password, and then use cache to store verification code with 15 minutes time-to-live.
After that, we will just send the email to that user.
[Case 2]: If the user is new
We'll build some info for them and save it to the DB, but set enabled to false.
And then we will simply cache and send the verification code.
[Verify the user]: verifyUser() method
After the verification code is sent to the user, they need to send to the BE VerifyRequest containing email, and the code.
We first check if the email existed or not, if not then fakely return a false exception InvalidVerificationCodeException.
If the user existed, we then get the verification code in cache to compare with the request.
If no code was found, or they are not equal then return exception message.
Alright, so after all of that, if the user does everything correctly, we should set enabled to true, and save it to DB, and delete the code from cache.

# The login
Ok so, after all the registration, user can then log in.
Alright, this thing is quite simple. We just need to verify the LoginRequest.
Check the email and password, enablity, if something goes wrong then throw BadCredentialsException.
If they do everything correctly, they should receive an access token and a refresh token which I'll talk about these later.
For now, let's just simply think that access token is some kind of tickets allowing you to do thing in our web.
Refresh token is used to refresh both the access token and refresh token.

# The logout
When a user log out, we should delete the access token and the refresh token.
We first find the refresh token, and delete it from the cache.
About the access token, since they are stateless, we must blacklist this to avoid someone gets it.
We extract the token, set a key in Redis with its remaining time to live.
From now on, even hackers somehow get this token, they can't use it to access because we have already banned it.

# What if user forget their password?
They first need to send their email for us to verify, if they are existed then we send a new verification code.
And save it to cache with new key and 15 minutes ttl.
Users then verify the code, this is pretty much similar to the registration.
If everything succeed, cache will save a new key with 5 minutes ttl. This is the amount of time users are able to reset their password.
If 5 minutes passes, they cannot reset and have to send new request.


> Sorry guys, I'm so lazy to write about JWT and Refresh Token.