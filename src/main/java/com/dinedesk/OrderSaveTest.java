
package com.dinedesk;

import com.dinedesk.dao.MenuItemDAO;
import com.dinedesk.dao.OrderDAO;
import com.dinedesk.model.MenuItem;
import com.dinedesk.model.Order;

import java.util.List;

public class OrderSaveTest {

    public static void main(String[] args) {

        try {
            MenuItemDAO menuItemDAO = new MenuItemDAO();
            List<MenuItem> menuItems =
                    menuItemDAO.getAllMenuItems();

            if (menuItems.isEmpty()) {
                System.out.println("No menu items found.");
                return;
            }

            Order order = new Order();
            order.addItem(menuItems.get(0), 2);

            if (menuItems.size() > 1) {
                order.addItem(menuItems.get(1), 1);
            }

            OrderDAO orderDAO = new OrderDAO();
            int orderId = orderDAO.saveOrder(order);

            System.out.println("Order saved successfully!");
            System.out.println("Order ID: " + orderId);
            System.out.printf(
                    "Total bill: ₹%.2f%n",
                    order.getTotalBill()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
