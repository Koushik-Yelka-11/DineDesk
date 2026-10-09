
package com.dinedesk;

import com.dinedesk.dao.CategoryDAO;
import com.dinedesk.model.Category;

public class CategoryTest {

    public static void main(String[] args) {
        CategoryDAO categoryDAO = new CategoryDAO();

        try {
            for (Category category : categoryDAO.getAllCategories()) {
                System.out.println(
                        category.getId() + " - " + category.getName()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
