# TODO: Integrate OTP into Registration Flow

## Backend Changes
- [x] Modify `/auth/register` endpoint to accept `name` field and store it in users table.
- [x] After creating user in register, automatically trigger OTP send logic (reuse existing send-otp code).
- [x] Ensure OTP is sent immediately after account creation.

## Frontend Changes
- [x] Update `fragment_register.xml` to include name field (et_fullname is already there).
- [x] Update `RegisterFragment.java` to collect name from et_fullname.
- [x] Modify register call to include name in body.
- [x] After successful register, automatically call sendOtp and show OTP input fields.
- [x] Update verifyOtp logic: on failure, if attempts < 5, automatically resend OTP; if >=5, wait 1 minute before allowing resend.
- [x] Handle navigation: on successful verify, go to login screen.

## Testing
- [ ] Test full flow: register -> auto OTP send -> verify correct -> login.
- [ ] Test resend on failure: enter wrong OTP, check auto resend if <5 attempts.
- [ ] Test rate limit: after 5 attempts, wait 1 minute.
