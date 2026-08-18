package com.jnetai.thermal.model;

import java.util.ArrayList;
import java.util.List;

public class ReceiptData {
    public String storeName = "";
    public String header = "";
    public String footer = "";
    public String number = "";
    public String cashier = "";
    public double subtotal = 0;
    public double tax = 0;
    public double total = 0;
    public double tendered = 0;
    public double change = 0;
    public List<ReceiptItem> items = new ArrayList<>();

    public static class ReceiptItem {
        public String name = "";
        public String qty = "1";
        public double price = 0;

        public ReceiptItem() {}
        public ReceiptItem(String name, String qty, double price) {
            this.name = name;
            this.qty = qty;
            this.price = price;
        }
    }

    /** Recompute subtotal/total from items and tax. */
    public void recomputeTotals() {
        subtotal = 0;
        for (ReceiptItem it : items) {
            try {
                subtotal += it.price * Double.parseDouble(it.qty);
            } catch (NumberFormatException ignored) {
                subtotal += it.price;
            }
        }
        total = subtotal + tax;
        change = tendered - total;
        if (change < 0) change = 0;
    }
}