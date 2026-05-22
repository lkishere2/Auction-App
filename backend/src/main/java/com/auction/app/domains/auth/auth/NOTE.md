1) The register workflow
*) Register = register() + sendVerificationCode() + verifyUser() + resendVerificationCode()

- When the user first time register
- They will provide username, password, and email
- The DB will set the user fields first, but also set enable to false because this is unverified account
- After they hit register button (call to register()), a verification code will be sent to their email (call sendVerificationEmail())
- Now, they need to verify the verification code in order to enable user (call to verifyUser())
- If the code get expired, user can request to resend new code as well (call to resendVerificationCode())
- After they successfully verified, the user is enabled now

2) The forget password feature (We'll utilize the verification code to set up this)
*) Forget password = requestPasswordReset() + sendVerificationCode() + verifyPasswordReset() + resetPassword() + resendVerificationCode()

- Notice that, a new field named passwordResetVerified appears in user entity, default false
- When user forget the password, they will hit the forget password button, now an email request would be sent to requestPasswordReset()
- After that, a new verification code will be sent to that user in order to verify
- Now, they need to verify the password reset (call to verifyPasswordReset() - it's almost the same as verifyUser() logic)
- Of course, they could call resendVerificationCode() for new verification code if the old one got expired
- Ok, after they successfully verify, they are now enable to reset password, update the passwordResetVerified to true
- Just need to type new password and send that request to resetPassword()
- Finally, the passwordResetVerified will be set to false again

3) The login workflow
*) Login = login() + authenticationManager + JWT + RefreshToken

- This is simple, I will talk about JWT and RefreshToken role after this
- User simply pass their email, and password, the authenticationManager will handle this in the background to valid information
- After the user successfully login, a new JWT and RefreshToken would be provided for them

(I'll note this for you guys later, I'll push my code now)
4) JWT role

5) RefreshToken role

6) The logout


OUTDATED VERSION (I'll update later, so lazy!)