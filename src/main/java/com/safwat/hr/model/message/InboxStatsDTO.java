package com.safwat.hr.model.message;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InboxStatsDTO {
    private long totalMessages;
    private long unreadCount;

}
