package com.safwat.hr.notification.ui;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.MessageClientService;
import com.safwat.hr.notification.service.NotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * =====================================================
 * MessageInboxController — Controller الواجهة الرئيسية
 * =====================================================
 */
public class MessageInboxController implements Initializable {

    private final MessageClientService msgService = MessageClientService.getInstance();
    private final NotificationService notifService = NotificationService.getInstance();
    private final ObservableList<MessageThread> threads = FXCollections.observableArrayList();
    @FXML
    private TextField searchField;
    @FXML
    private ListView<MessageThread> threadList;
    @FXML
    private VBox conversationContainer;
    private FilteredList<MessageThread> filteredThreads;

    private MessageConversationView conversationView;
    private MessageComposer composer;
    private MessageThread selectedThread;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupThreadList();
        setupConversationArea();
        setupSearch();
        setupComposer();
        loadThreads();
        listenForNewMessages();
    }

    // ═════════════════════════════════════════════════════════════════
    //  Setup
    // ═════════════════════════════════════════════════════════════════

    private void setupThreadList() {
        filteredThreads = new FilteredList<>(threads, t -> true);
        threadList.setItems(filteredThreads);
        threadList.setCellFactory(lv -> new MessageThreadListCell());

        threadList.getSelectionModel().selectedItemProperty().addListener((obs, old, thread) -> {
            if (thread != null) {
                openThread(thread);
            }
        });
    }

    private void setupConversationArea() {
        conversationView = new MessageConversationView();
        composer = new MessageComposer();

        conversationContainer.getChildren().addAll(conversationView, composer);
        VBox.setVgrow(conversationView, Priority.ALWAYS);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, query) -> {
            filteredThreads.setPredicate(t -> {
                if (query == null || query.isBlank()) return true;
                String q = query.toLowerCase();
                String subject = t.getSubject() != null ? t.getSubject().toLowerCase() : "";
                String sender = t.getSenderName() != null ? t.getSenderName().toLowerCase() : "";
                String preview = t.getPreview() != null ? t.getPreview().toLowerCase() : "";
                return subject.contains(q) || sender.contains(q) || preview.contains(q);
            });
        });
    }

    private void setupComposer() {
        // === Reply mode ===
        composer.setOnReply(content -> {
            if (selectedThread == null) return;

            Long parentId = selectedThread.getId();
            String subject = "رد: " + selectedThread.getSubject();
            List<Path> attachments = composer.getAttachments();

            msgService.replyToMessage(
                    parentId,
                    subject,
                    content,
                    attachments.isEmpty() ? null : attachments,
                    () -> Platform.runLater(() -> {
                        composer.clearAttachments();
                        refreshThread(parentId);
                    }),
                    err -> Platform.runLater(() -> {
                        System.err.println("[Inbox] Reply failed: " + err);
                    })
            );
        });

        // === New message mode ===
        composer.setOnNewMessage((recipient, content) -> {
            String subject = composer.getSubject();
            List<Path> attachments = composer.getAttachments();

            msgService.sendMessage(
                    recipient,
                    subject,
                    content,
                    attachments.isEmpty() ? null : attachments,
                    () -> Platform.runLater(() -> {
                        composer.clearAll();
                        if (selectedThread != null) {
                            composer.setReplyMode(selectedThread.getSubject());
                        }
                    }),
                    err -> Platform.runLater(() -> {
                        System.err.println("[Inbox] Send failed: " + err);
                    })
            );
        });

        // === Attach ===
        composer.setOnAttach(() -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("اختر ملف للإرفاق");
            List<java.io.File> files = chooser.showOpenMultipleDialog(threadList.getScene().getWindow());
            if (files != null) {
                files.forEach(f -> composer.addAttachment(f.toPath()));
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Load / Update Threads
    // ═════════════════════════════════════════════════════════════════

    private void loadThreads() {
        threads.clear();
        for (HRNotification n : notifService.getAll()) {
            if (n.isMessage()) {
                threads.add(new MessageThread(n));
            }
        }
    }

    private void listenForNewMessages() {
        notifService.getAll().addListener((javafx.collections.ListChangeListener<HRNotification>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (HRNotification n : c.getAddedSubList()) {
                        if (n.isMessage()) {
                            Platform.runLater(() -> addOrUpdateThread(n));
                        }
                    }
                }
            }
        });
    }

    private void addOrUpdateThread(HRNotification notification) {
        Long id = extractId(notification.getActionTarget());
        if (id == null) return;

        for (int i = 0; i < threads.size(); i++) {
            MessageThread t = threads.get(i);
            if (id.equals(t.getId())) {
                threads.set(i, new MessageThread(notification));
                if (selectedThread != null && selectedThread.getId().equals(id)) {
                    openThread(threads.get(i));
                }
                return;
            }
        }
        threads.add(0, new MessageThread(notification));
    }

    // ═════════════════════════════════════════════════════════════════
    //  Open Thread
    // ═════════════════════════════════════════════════════════════════

    private void openThread(MessageThread thread) {
        selectedThread = thread;

        // Mark as read
        if (!thread.isRead()) {
            Long id = thread.getId();
            if (id != null) {
                msgService.markMessageAsRead(id).thenRun(() -> {
                    Platform.runLater(() -> {
                        thread.markAsRead();
                        notifService.updateUnreadCount();
                        threadList.refresh();
                    });
                });
            }
        }

        // Load thread (parent + replies)
        Long msgId = thread.getId();
        if (msgId != null) {
            msgService.getThread(msgId).thenAccept(threadDTO -> {
                if (threadDTO != null) {
                    Platform.runLater(() -> updateThreadFromDTO(thread, threadDTO));
                }
            }).exceptionally(e -> {
                System.err.println("[Inbox] Thread load failed, falling back: " + e.getMessage());
                msgService.getMessageDetails(msgId).thenAccept(data -> {
                    if (data != null) {
                        Platform.runLater(() -> updateThreadFromDetails(thread, data));
                    }
                });
                return null;
            });
        }

        conversationView.displayThread(thread);
        composer.setReplyMode(thread.getSubject());
    }

    // ═════════════════════════════════════════════════════════════════
    //  Update Thread from DTO / Details
    // ═════════════════════════════════════════════════════════════════

    /**
     * ✅ يحدّث الـ thread من MessageThreadDTO (parent + replies)
     */
    @SuppressWarnings("unchecked")
    private void updateThreadFromDTO(MessageThread thread, Map<String, Object> threadDTO) {
        // Parent message
        Object parentObj = threadDTO.get("parent");
        if (parentObj instanceof Map) {
            Map<String, Object> parent = (Map<String, Object>) parentObj;
            HRNotification updatedRoot = mapToNotification(parent);
            thread.setRootMessage(updatedRoot);
        }

        // Replies
        Object repliesObj = threadDTO.get("replies");
        if (repliesObj instanceof List) {
            thread.getReplies().clear();
            for (Object o : (List<?>) repliesObj) {
                if (o instanceof Map) {
                    Map<String, Object> replyMap = (Map<String, Object>) o;
                    HRNotification replyNotif = mapToNotification(replyMap);
                    thread.getReplies().add(replyNotif);
                }
            }
        }

        conversationView.displayThread(thread);
    }

    /**
     * ✅ يبني HRNotification من Map (للـ parent أو reply)
     */
    @SuppressWarnings("unchecked")
    private HRNotification mapToNotification(Map<String, Object> data) {
        String subject = (String) data.get("subject");
        String body = (String) data.get("body");
        String senderUsername = (String) data.get("senderUsername");
        String senderDisplayName = (String) data.get("senderDisplayName");
        Object id = data.get("id");
        Object createdAt = data.get("createdAt");

        HRNotification.Builder builder = HRNotification.builder()
                .category(HRNotification.NotificationCategory.MESSAGE)
                .type(HRNotification.NotificationType.MESSAGE)
                .title(subject != null ? subject : "رد")
                .message(body != null ? body : "")
                .sender(senderDisplayName != null ? senderDisplayName : senderUsername)
                .senderUsername(senderUsername)
                .senderAvatar(buildAvatar(senderDisplayName))
                .action("فتح الرسالة", "messages/" + id);

        // Attachments
        Object atts = data.get("attachments");
        if (atts instanceof List) {
            for (Object o : (List<?>) atts) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    String name = (String) m.get("fileName");
                    String token = (String) m.get("downloadToken");
                    String mime = (String) m.get("mimeType");
                    Object size = m.get("fileSize");
                    long sz = size != null ? ((Number) size).longValue() : 0;
                    builder.attachment(name, "", mime, sz, token);
                }
            }
        }

        return builder.build();
    }

    private String buildAvatar(String displayName) {
        if (displayName == null || displayName.isBlank()) return "؟";
        String[] parts = displayName.trim().split("\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    /**
     * Fallback — يحدّث الـ thread من getMessageDetails (single message)
     */
    @SuppressWarnings("unchecked")
    private void updateThreadFromDetails(MessageThread thread, Map<String, Object> data) {
        HRNotification root = thread.getRootMessage();

        // Body
        String body = (String) data.get("body");
        if (body != null && !body.isBlank()) {
            // HRNotification messageBody is read-only
        }

        // Attachments
        Object atts = data.get("attachments");
        if (atts instanceof List) {
            root.getAttachments().clear();
            for (Object o : (List<?>) atts) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    String name = (String) m.get("fileName");
                    String token = (String) m.get("downloadToken");
                    String mime = (String) m.get("mimeType");
                    Object size = m.get("fileSize");
                    long sz = size != null ? ((Number) size).longValue() : 0;

                    root.getAttachments().add(
                            new HRNotification.Attachment(name, "", mime, sz, token)
                    );
                }
            }
        }

        conversationView.displayThread(thread);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Refresh Thread
    // ═════════════════════════════════════════════════════════════════

    private void refreshThread(Long messageId) {
        msgService.getThread(messageId).thenAccept(data -> {
            if (data != null) {
                Platform.runLater(() -> {
                    for (MessageThread t : threads) {
                        if (messageId.equals(t.getId())) {
                            updateThreadFromDTO(t, data);
                            break;
                        }
                    }
                });
            }
        }).exceptionally(e -> {
            // Fallback
            msgService.getMessageDetails(messageId).thenAccept(data2 -> {
                if (data2 != null) {
                    Platform.runLater(() -> {
                        for (MessageThread t : threads) {
                            if (messageId.equals(t.getId())) {
                                updateThreadFromDetails(t, data2);
                                break;
                            }
                        }
                    });
                }
            });
            return null;
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Actions
    // ═════════════════════════════════════════════════════════════════

    @FXML
    private void onComposeNew() {
        selectedThread = null;
        threadList.getSelectionModel().clearSelection();
        conversationView.clear();
        composer.setNewMessageMode();
    }

    /**
     * فتح رسالة معينة من بره (من الـ Notification Panel)
     */
    public void openMessage(HRNotification notification) {
        Long id = extractId(notification.getActionTarget());
        if (id == null) return;

        for (MessageThread t : threads) {
            if (id.equals(t.getId())) {
                threadList.getSelectionModel().select(t);
                threadList.scrollTo(t);
                return;
            }
        }

        MessageThread newThread = new MessageThread(notification);
        threads.add(0, newThread);
        threadList.getSelectionModel().select(newThread);
    }

    // ═════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════

    private Long extractId(String actionTarget) {
        if (actionTarget == null || !actionTarget.startsWith("messages/")) return null;
        try {
            return Long.parseLong(actionTarget.substring(9));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}