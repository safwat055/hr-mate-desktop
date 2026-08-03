package com.safwat.hr.message.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InboxStatsDTO {
    private long totalMessages;
    private long unreadCount;

}
