package io.github.phunguy65.zms.meetingmanagement.application.command;

/**
 * Command for activating a recording after LiveKit confirms egress start.
 */
public record ActivateRecordingCommand(String livekitEgressId) {}
