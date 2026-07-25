package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * =====================================================
 * MessageThread — محادثة/Thread واحدة في الـ Inbox
 * =====================================================
 * دلوقتي: كل thread = رسالة واحدة (root)
 * لما الـ Backend يجهز: هنضيف replies
 */
public class MessageThread {
    private final ObservableList<HRNotification> replies = FXCollections.observableArrayList();
    private HRNotification rootMessage;

    public MessageThread(HRNotification rootMessage) {
        this.rootMessage = rootMessage;
    }

    public HRNotification getRootMessage() {
        return rootMessage;
    }

    public void setRootMessage(HRNotification rootMessage) {
        this.rootMessage = rootMessage;
    }

    public ObservableList<HRNotification> getReplies() {
        return replies;
    }

    public Long getId() {
        String target = rootMessage.getActionTarget();
        if (target != null && target.startsWith("messages/")) {
            try {
                return Long.parseLong(target.substring(9));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String getSubject() {
        return rootMessage.getTitle();
    }

    public String getPreview() {
        return rootMessage.getMessage();
    }

    public String getSenderName() {
        return rootMessage.getSenderName();
    }

    public boolean isRead() {
        return rootMessage.isRead();
    }

    public void markAsRead() {
        rootMessage.markAsRead();
    }
}