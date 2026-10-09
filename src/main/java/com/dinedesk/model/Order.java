
package com.dinedesk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {

    private final List<OrderItem> items = new ArrayList<>();

    public void addItem(MenuItem menuItem, int quantity) {
        for (OrderItem item : items) {
            if (item.getMenuItem().getId() == menuItem.getId()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }

        items.add(new OrderItem(menuItem, quantity));
    }

    public void removeItem(int menuItemId) {
        items.removeIf(
                item -> item.getMenuItem().getId() == menuItemId
        );
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getTotalBill() {
        double total = 0;

        for (OrderItem item : items) {
            total += item.getSubtotal();
        }

        return total;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }
}
