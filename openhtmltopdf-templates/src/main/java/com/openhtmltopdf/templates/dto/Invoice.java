package com.openhtmltopdf.templates.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Invoice {
    public static class InvoiceItem {
        public final String description;
        public final int quantity;
        public final BigDecimal unitPrice;
        public final BigDecimal totalPrice;

        public InvoiceItem(String description, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }
    }

    public final Company from;
    public final Company to;
    public final LocalDate date;
    public final LocalDate due;
    public final String attention;
    public final String id;
    public final List<InvoiceItem> items;
    public final BigDecimal subTotal;
    public final BigDecimal taxRate;
    public final BigDecimal taxAmount;
    public final BigDecimal total;
    public final String notice;

    public Invoice(String id,
                   Company from, Company to,
                   String attention,
                   LocalDate date, LocalDate due,
                   List<InvoiceItem> items,
                   BigDecimal subTotal, BigDecimal taxRate, BigDecimal taxAmount, BigDecimal total,
                   String notice) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.attention = attention;
        this.date = date;
        this.due = due;
        this.items = items;
        this.subTotal = subTotal;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.total = total;
        this.notice = notice;
    }
}
