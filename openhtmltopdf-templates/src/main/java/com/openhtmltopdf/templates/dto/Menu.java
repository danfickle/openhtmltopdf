package com.openhtmltopdf.templates.dto;

import java.util.Arrays;
import java.util.List;

public class Menu {
    public static class MenuSection {
        public String title;
        public List<MenuItem> items;

        public MenuSection(String title, MenuItem... items) {
            this.title = title;
            this.items = Arrays.asList(items);
        }
    }

    public static class MenuItem {
        public String title;
        public String description;
        public String price;

        public MenuItem(String title, String description, String price) {
            this.title = title;
            this.description = description;
            this.price = price;
        }
    }

    public final Company company;
    public List<MenuSection> sections;

    public Menu(Company company, MenuSection... sections) {
        this.company = company;
        this.sections = Arrays.asList(sections);
    }
}
