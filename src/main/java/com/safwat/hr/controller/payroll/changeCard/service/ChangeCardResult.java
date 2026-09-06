package com.safwat.hr.controller.payroll.changeCard.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ChangeCardResult {
    private final BooleanProperty selected;
    private final StringProperty month;
    private final StringProperty value;
    private final StringProperty notes;

    public ChangeCardResult(String month, String value, String notes) {
        this.selected = new SimpleBooleanProperty(false);
        this.month = new SimpleStringProperty(month);
        this.value = new SimpleStringProperty(value);
        this.notes = new SimpleStringProperty(notes);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public String getMonth() {
        return month.get();
    }

    public void setMonth(String month) {
        this.month.set(month);
    }

    public StringProperty monthProperty() {
        return month;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public StringProperty valueProperty() {
        return value;
    }

    public String getNotes() {
        return notes.get();
    }

    public void setNotes(String notes) {
        this.notes.set(notes);
    }

    public StringProperty notesProperty() {
        return notes;
    }
}