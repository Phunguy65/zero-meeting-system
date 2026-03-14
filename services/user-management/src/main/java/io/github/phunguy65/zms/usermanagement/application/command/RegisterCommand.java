package io.github.phunguy65.zms.usermanagement.application.command;

public record RegisterCommand(String email, String password, String fullName, String username) {}
