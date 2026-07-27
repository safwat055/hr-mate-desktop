// PageDTO.java - في العميل (JavaFX)
package com.safwat.hr.notification.util;

import lombok.Setter;

import java.util.List;

@Setter
public class PageDTO<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;

    public List<T> getContent() {
        return content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getSize() {
        return size;
    }

    public int getNumber() {
        return number;
    }

}