
package com.dinedesk;

import com.dinedesk.dao.MenuItemDAO;
import com.dinedesk.model.MenuItem;

public class MenuItemTest {

    public static void main(String[] args) {
        MenuItemDAO menuItemDAO = new MenuItemDAO();

        try {
            for (MenuItem item : menuItemDAO.getAllMenuItems()) {
                System.out.printf(
                        "%s | ₹%.2f | Category ID: %d%n",
                        item.getName(),
                        item.getPrice(),
                        item.getCategoryId()
                );
            }

            System.out.println("Menu items loaded successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
