package com.safwat.hr.controller.payroll.payments.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PaymentsResult {
    private final BooleanProperty selected;
    private final StringProperty month;
    private final StringProperty payGroup;
    private final StringProperty total;
    private final StringProperty tax;
    private final StringProperty stampTax;
    private final StringProperty net;
    private final StringProperty description;
    private final StringProperty note;

    public PaymentsResult(String month, String payGroup, String total, String tax,
                          String stampTax, String net, String description, String note) {
        this.selected = new SimpleBooleanProperty(false);
        this.month = new SimpleStringProperty(month);
        this.payGroup = new SimpleStringProperty(payGroup);
        this.total = new SimpleStringProperty(total);
        this.tax = new SimpleStringProperty(tax);
        this.stampTax = new SimpleStringProperty(stampTax);
        this.net = new SimpleStringProperty(net);
        this.description = new SimpleStringProperty(description);
        this.note = new SimpleStringProperty(note);
    }

    // Getters للقيم النصية العادية
    public boolean getSelected() {
        return selected.get();
    }

    // Setters
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public String getMonth() {
        return month.get();
    }

    public void setMonth(String month) {
        this.month.set(month);
    }

    public String getPayGroup() {
        return payGroup.get();
    }

    public void setPayGroup(String payGroup) {
        this.payGroup.set(payGroup);
    }

    public String getTotal() {
        return total.get();
    }

    public void setTotal(String total) {
        this.total.set(total);
    }

    public String getTax() {
        return tax.get();
    }

    public void setTax(String tax) {
        this.tax.set(tax);
    }

    public String getStampTax() {
        return stampTax.get();
    }

    public void setStampTax(String stampTax) {
        this.stampTax.set(stampTax);
    }

    public String getNet() {
        return net.get();
    }

    public void setNet(String net) {
        this.net.set(net);
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public String getNote() {
        return note.get();
    }

    public void setNote(String note) {
        this.note.set(note);
    }

    // Property getters (التي يحتاجها PropertyValueFactory)
    public BooleanProperty selectedProperty() {
        return selected;
    }

    public StringProperty monthProperty() {
        return month;
    }

    public StringProperty payGroupProperty() {
        return payGroup;
    }


    public StringProperty totalProperty() {
        return total;
    }

    public StringProperty taxProperty() {
        return tax;
    }

    public StringProperty stampTaxProperty() {
        return stampTax;
    }

    public StringProperty netProperty() {
        return net;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty noteProperty() {
        return note;
    }

    public void setNotes(String notes) {
        this.note.set(notes);
    }
}