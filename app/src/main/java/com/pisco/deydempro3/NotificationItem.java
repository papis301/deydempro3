package com.pisco.deydempro3;

public class NotificationItem {
    private int id;
    private String title;
    private String message;
    private String type;
    private String actionUrl;
    private String createdAt;

    public NotificationItem(int id, String title, String message, String type, String actionUrl, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.actionUrl = actionUrl;
        this.createdAt = createdAt;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getActionUrl() { return actionUrl; }
}
