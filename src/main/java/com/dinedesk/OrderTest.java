
package com.dinedesk;

import com.dinedesk.dao.MenuItemDAO;
import com.dinedesk.model.MenuItem;
import com.dinedesk.model.Order;

import java.util.List;

public class OrderTest {

    public static void main(String[] args) {
        try {
            MenuItemDAO menuItemDAO = new MenuItemDAO();
            List<MenuItem> menuItems = menuItemDAO.getAllMenuItems();

            if (menuItems.size() < 2) {
                System.out.println("Insert at least two menu items first.");
                return;
            }

            MenuItem firstItem = menuItems.get(0);
            MenuItem secondItem = menuItems.get(1);

            Order order = new Order();

            order.addItem(firstItem, 2);
            order.addItem(secondItem, 1);

            // Add the first item again to test quantity merging.
            order.addItem(firstItem, 1);

            System.out.println("Cart items:");

            order.getItems().forEach(item ->
                    System.out.printf(
                            "%s | Qty: %d | Subtotal: ₹%.2f%n",
                            item.getMenuItem().getName(),
                            item.getQuantity(),
                            item.getSubtotal()
                    )
            );

            System.out.printf(
                    "Total bill: ₹%.2f%n",
                    order.getTotalBill()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
