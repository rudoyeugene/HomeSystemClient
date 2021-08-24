package com.rudyii.hsw.client.objects.types;

public enum NotificationType {
    MOTION_DETECTED("motionDetected"),
    VIDEO_RECORDED("videoRecorded"),
    ALL("all"),
    MUTE("mute");

    private final String firebaseName;

    NotificationType(String firebaseName) {
        this.firebaseName = firebaseName;
    }

    public String getFirebaseName() {
        return firebaseName;
    }
}
