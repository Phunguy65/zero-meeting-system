package io.github.phunguy65.zms.usermanagement.application.command;

/**
 * Command to reset a password using an OTP.
 *
 * @param email       the email address
 * @param otp         the 6-digit OTP received via email
 * @param newPassword the new password to set
 */
public record ResetPasswordCommand(String email, String otp, String newPassword) {}
