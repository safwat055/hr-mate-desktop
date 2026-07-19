package com.safwat.hr.notification.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * نموذج بيانات الإشعار في نظام الموارد البشرية.
 * يمثل إشعاراً واحداً بكل تفاصيله.
 */
public class HRNotification {

    // ===================== الحقول =====================
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();
    private final ObjectProperty<NotificationType> type =
            new SimpleObjectProperty<>();
    private final ObjectProperty<Priority> priority =
            new SimpleObjectProperty<>(Priority.NORMAL);
    private final BooleanProperty read = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> timestamp =
            new SimpleObjectProperty<>(LocalDateTime.now());
    private final StringProperty actionLabel = new SimpleStringProperty();
    private final StringProperty actionTarget = new SimpleStringProperty();
    private final BooleanProperty hasFile = new SimpleBooleanProperty(false);
    private final StringProperty filePath = new SimpleStringProperty();
    private final StringProperty senderName = new SimpleStringProperty();
    // ===================== بناء الكائن =====================
    private HRNotification() {
        id.set(UUID.randomUUID().toString());
    }

    // ===================== Builder =====================
    public static Builder builder() {
        return new Builder();
    }

    // ===================== Getters / Properties =====================
    public String getId() {
        return id.get();
    }

    public String getTitle() {
        return title.get();
    }

    public String getMessage() {
        return message.get();
    }

    public NotificationType getType() {
        return type.get();
    }

    public Priority getPriority() {
        return priority.get();
    }

    public boolean isRead() {
        return read.get();
    }

    public LocalDateTime getTimestamp() {
        return timestamp.get();
    }

    public String getActionLabel() {
        return actionLabel.get();
    }

    public String getActionTarget() {
        return actionTarget.get();
    }

    public boolean hasFile() {
        return hasFile.get();
    }

    public String getFilePath() {
        return filePath.get();
    }

    public String getSenderName() {
        return senderName.get();
    }

    public BooleanProperty readProperty() {
        return read;
    }

    public ObjectProperty<NotificationType> typeProperty() {
        return type;
    }

    public void markAsRead() {
        read.set(true);
    }

    public String getFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(timestamp.get(), now).toMinutes();
        if (minutes < 1) return "الآن";
        if (minutes < 60) return "منذ " + minutes + " دقيقة";
        long hours = minutes / 60;
        if (hours < 24) return "منذ " + hours + " ساعة";
        return timestamp.get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ===================== أنواع الإشعارات =====================
    public enum NotificationType {
        EMPLOYEE("موظفون", "#185FA5", "#E6F1FB"),
        SALARY("رواتب", "#3B6D11", "#EAF3DE"),
        LEAVE("إجازات", "#854F0B", "#FAEEDA"),
        TRAINING("تدريب", "#534AB7", "#EEEDFE"),
        TASK("مهام", "#A32D2D", "#FCEBEB"),
        SYSTEM("النظام", "#5F5E5A", "#F1EFE8");

        public final String label;
        public final String color;
        public final String bgColor;

        NotificationType(String label, String color, String bgColor) {
            this.label = label;
            this.color = color;
            this.bgColor = bgColor;
        }
    }

    // ===================== مستوى الأهمية =====================
    public enum Priority {LOW, NORMAL, HIGH, URGENT}

    public static class Builder {
        private final HRNotification n = new HRNotification();

        public Builder title(String v) {
            n.title.set(v);
            return this;
        }

        public Builder message(String v) {
            n.message.set(v);
            return this;
        }

        public Builder type(NotificationType v) {
            n.type.set(v);
            return this;
        }

        public Builder priority(Priority v) {
            n.priority.set(v);
            return this;
        }

        public Builder action(String label, String target) {
            n.actionLabel.set(label);
            n.actionTarget.set(target);
            return this;
        }

        public Builder file(String path) {
            n.hasFile.set(true);
            n.filePath.set(path);
            return this;
        }

        public Builder sender(String name) {
            n.senderName.set(name);
            return this;
        }

        public Builder timestamp(LocalDateTime t) {
            n.timestamp.set(t);
            return this;
        }

        public HRNotification build() {
            return n;
        }
    }
}
