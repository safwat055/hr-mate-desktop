package com.safwat.hr.controller.message.controller;

import com.safwat.hr.controller.message.dto.MessageConversationView;
import com.safwat.hr.controller.message.dto.UserInfo;
import com.safwat.hr.controller.message.service.MessageClientService;
import com.safwat.hr.controller.message.service.MessageComposer;
import com.safwat.hr.controller.message.service.MessageThread;
import com.safwat.hr.controller.message.ui.MessageThreadListCell;
import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.shared.ui.MultiSelectSearchDialog;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * =====================================================================
 * MessageInboxController
 * =====================================================================
 * المتحكم الرئيسي لواجهة صندوق الرسائل.
 * يدير ثلاثة أقسام رئيسية:
 * 1. قائمة المحادثات (اليسار): عرض وتصفية المحادثات
 * 2. منطقة المحادثة (الوسط): عرض الرسائل والردود
 * 3. محرر الرسائل (الأسفل): كتابة رد أو رسالة جديدة
 * <p>
 * يتعامل مع أحداث المستخدم مثل اختيار محادثة، إرسال رد، البحث، وإرفاق ملفات.
 */
public class MessageInboxController implements Initializable {
    private static final DateTimeFormatter SERVER_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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

    /**
     * تهيئة الواجهة عند تحميل الـ FXML.
     * تُنشئ القوائم، وتُعد منطقة المحادثة، وتُفعّل البحث، وتُحمّل المحادثات.
     */
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

    /**
     * إعداد قائمة المحادثات مع الفلترة وخلية العرض المخصصة.
     */
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

    /**
     * إعداد منطقة المحادثة والمحرر وإضافتهما للحاوية.
     */
    private void setupConversationArea() {
        conversationView = new MessageConversationView();
        composer = new MessageComposer();

        conversationContainer.getChildren().addAll(conversationView, composer);
        VBox.setVgrow(conversationView, Priority.ALWAYS);
    }

    /**
     * إعداد مستمع البحث لتصفية المحادثات حسب الموضوع أو المرسل أو المعاينة.
     */
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

    /**
     * إعداد محرر الرسائل وتعيين callbacks للرد والرسائل الجديدة والإرفاق.
     */
    private void setupComposer() {
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

        composer.setOnNewMessage(content -> {
            List<String> recipients = composer.getRecipientUsernames();
            String subject = composer.getSubject();
            List<Path> attachments = composer.getAttachments();

            if (recipients.isEmpty()) {
                System.err.println("[Inbox] No recipients selected");
                return;
            }

            msgService.sendMessageToMultiple(
                    recipients,
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

        composer.setOnAttach(() -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("اختر ملف للإرفاق");
            List<java.io.File> files = chooser.showOpenMultipleDialog(threadList.getScene().getWindow());
            if (files != null) {
                files.forEach(f -> composer.addAttachment(f.toPath()));
            }
        });

        composer.setOnSearchRecipients(() -> {
            javafx.scene.control.Alert loadingAlert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            loadingAlert.setTitle("جاري التحميل");
            loadingAlert.setHeaderText(null);
            loadingAlert.setContentText("جاري جلب قائمة المستخدمين...");
            loadingAlert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK).setVisible(false);
            loadingAlert.show();

            msgService.getAllUsers().thenAccept(users -> {
                Platform.runLater(() -> {
                    loadingAlert.close();

                    if (users == null || users.isEmpty()) {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.WARNING);
                        alert.setTitle("تنبيه");
                        alert.setHeaderText(null);
                        alert.setContentText("لم يتم العثور على مستخدمين أو فشل الاتصال بالسيرفر.");
                        alert.showAndWait();
                        return;
                    }

                    MultiSelectSearchDialog<UserInfo> dialog = MultiSelectSearchDialog.<UserInfo>builder(UserInfo.class)
                            .title("اختر المستلمين")
                            .column("اسم الموظف", UserInfo::getDisplayName)
                            .column("اسم المستخدم", UserInfo::getUsername)
                            .data(users)
                            .searchPlaceholder("ابحث باسم الموظف أو اسم المستخدم...")
                            .owner((javafx.stage.Stage) threadList.getScene().getWindow());

                    List<UserInfo> selected = dialog.showAndWait();
                    if (selected != null && !selected.isEmpty()) {
                        composer.setRecipients(selected);
                    }
                });
            }).exceptionally(e -> {
                Platform.runLater(() -> {
                    loadingAlert.close();
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("خطأ");
                    alert.setHeaderText(null);
                    alert.setContentText("فشل جلب المستخدمين: " + e.getMessage());
                    alert.showAndWait();
                });
                return null;
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════
    //  Load / Update Threads
    // ═════════════════════════════════════════════════════════════════

    /**
     * تحميل المحادثات من خدمة الإشعارات إلى القائمة.
     */
    private void loadThreads() {
        threads.clear();
        for (HRNotification n : notifService.getAll()) {
            if (n.isMessage()) {
                threads.add(new MessageThread(n));
            }
        }
    }

    /**
     * إضافة مستمع لتغيرات قائمة الإشعارات لإضافة الرسائل الجديدة تلقائياً.
     */
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

    /**
     * إضافة محادثة جديدة أو تحديث موجودة عند وصول إشعار.
     *
     * @param notification الإشعار الجديد
     */
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

    /**
     * فتح محادثة محددة وتحميل تفاصيلها من الخادم.
     * يتم تعليمها كمقروءة وعرضها في منطقة المحادثة.
     *
     * @param thread المحادثة المختارة
     */
    private void openThread(MessageThread thread) {
        selectedThread = thread;

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
     * تحديث محتوى المحادثة من بيانات الخادم (parent + replies).
     *
     * @param thread    كائن المحادثة المحلي
     * @param threadDTO بيانات المحادثة من الخادم
     */
    @SuppressWarnings("unchecked")
    private void updateThreadFromDTO(MessageThread thread, Map<String, Object> threadDTO) {
        Object parentObj = threadDTO.get("parent");
        if (parentObj instanceof Map) {
            Map<String, Object> parent = (Map<String, Object>) parentObj;
            HRNotification updatedRoot = mapToNotification(parent);
            thread.setRootMessage(updatedRoot);
        }

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
     * تحويل Map قادم من الخادم إلى كائن HRNotification.
     * يتعامل مع تحليل التاريخ بصيغة السيرفر.
     *
     * @param data Map بالبيانات
     * @return كائن HRNotification
     */
    @SuppressWarnings("unchecked")
    private HRNotification mapToNotification(Map<String, Object> data) {
        String subject = (String) data.get("subject");
        String body = (String) data.get("body");
        String senderUsername = (String) data.get("senderUsername");
        String senderDisplayName = (String) data.get("senderDisplayName");
        Object id = data.get("id");

        LocalDateTime createdAt = parseDateTime(data.get("createdAt"));

        HRNotification.Builder builder = HRNotification.builder()
                .category(HRNotification.NotificationCategory.MESSAGE)
                .type(HRNotification.NotificationType.MESSAGE)
                .title(subject != null ? subject : "رد")
                .message(body != null ? body : "")
                .sender(senderDisplayName != null ? senderDisplayName : senderUsername)
                .senderUsername(senderUsername)
                .senderAvatar(buildAvatar(senderDisplayName))
                .timestamp(createdAt != null ? createdAt : LocalDateTime.now())
                .action("فتح الرسالة", "messages/" + id);

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

    /**
     * تحليل قيمة التاريخ من أي صيغة (String بصيغة السيرفر أو ISO أو List).
     *
     * @param value قيمة التاريخ من JSON
     * @return LocalDateTime أو null
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                String s = (String) value;
                if (s.contains("T")) {
                    return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                return LocalDateTime.parse(s, SERVER_FORMAT);
            }
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.size() >= 6) {
                    return LocalDateTime.of(
                            ((Number) list.get(0)).intValue(),
                            ((Number) list.get(1)).intValue(),
                            ((Number) list.get(2)).intValue(),
                            ((Number) list.get(3)).intValue(),
                            ((Number) list.get(4)).intValue(),
                            ((Number) list.get(5)).intValue()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("[TIME] Failed to parse: " + value + " — " + e.getMessage());
        }
        return null;
    }

    private String buildAvatar(String displayName) {
        if (displayName == null || displayName.isBlank()) return "؟";
        String[] parts = displayName.trim().split("\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0));
        return "" + parts[0].charAt(0) + parts[1].charAt(0);
    }

    /**
     * تحديث المحادثة من getMessageDetails (رسالة واحدة) كبديل.
     *
     * @param thread المحادثة المحلية
     * @param data   بيانات الرسالة من الخادم
     */
    @SuppressWarnings("unchecked")
    private void updateThreadFromDetails(MessageThread thread, Map<String, Object> data) {
        HRNotification root = thread.getRootMessage();

        LocalDateTime createdAt = parseDateTime(data.get("createdAt"));
        if (createdAt != null) {
            // Note: timestamp is set during initial creation
        }

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

    /**
     * تحديث محادثة محددة من الخادم بعد إرسال رد.
     *
     * @param messageId معرف الرسالة
     */
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

    /**
     * فتح وضع كتابة رسالة جديدة.
     * يمسح التحديد ويفرغ منطقة المحادثة ويُفعّل وضع الرسالة الجديدة في المحرر.
     */
    @FXML
    private void onComposeNew() {
        selectedThread = null;
        threadList.getSelectionModel().clearSelection();
        conversationView.clear();
        composer.setNewMessageMode();
    }

    /**
     * فتح رسالة محددة من الإشعارات.
     * إذا كانت المحادثة موجودة يتم تحديدها، وإلا تُنشأ محادثة جديدة.
     *
     * @param notification الإشعار المرتبط بالرسالة
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