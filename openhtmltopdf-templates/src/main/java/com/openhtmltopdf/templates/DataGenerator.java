package com.openhtmltopdf.templates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.openhtmltopdf.templates.dto.Address;
import com.openhtmltopdf.templates.dto.Company;
import com.openhtmltopdf.templates.dto.Invoice;
import com.openhtmltopdf.templates.dto.Menu;

public class DataGenerator {
    public interface DataProvider {
        Map<String, Object> provide();
    }

    public static Address defaultAddress() {
        return new Address("Unit 1", "123 Main St", "Springfield", "Ohio", "USA", "54325");
    }

    public static Address address2() {
        return new Address("125 Second Ave", null, "Waterside", "California", "America", null);
    }

    public static Company defaultCompany() {
        return new Company("ACME Corp", "images/flyingsaucer.png", defaultAddress(), "acme@example.com", "http://example.com", "555-5432");
    }

    public static Company company2() {
        return new Company("ABC", null, address2(), "abc@example.com", "http://abc.example.com", "555-1234");
    }

    public static final DataProvider INVOICE = () -> 
        Collections.singletonMap("invoice", new Invoice(
                "1-2-3",
                defaultCompany(),
                company2(),
                "John Citizen",
                LocalDate.of(2025, 03, 25),
                LocalDate.of(2025, 05, 25),
                Arrays.asList(
                    new Invoice.InvoiceItem("Create simple website", 1, BigDecimal.valueOf(150000, 2), BigDecimal.valueOf(150000, 2)),
                    new Invoice.InvoiceItem("Social media posts", 10, BigDecimal.valueOf(10000, 2), BigDecimal.valueOf(100000, 2)),
                    new Invoice.InvoiceItem("Email templates", 2, BigDecimal.valueOf(15073, 2), BigDecimal.valueOf(30146, 2))
                ),
                BigDecimal.valueOf(280146, 2),
                BigDecimal.valueOf(2500, 2),
                BigDecimal.valueOf(70037, 2),
                BigDecimal.valueOf(350183, 2),
                "A finance charge of 1.5% will be made on unpaid balances after 30 days."
       ));

     public static Company company3() {
         return new Company("My Place", null, address2(), "myplace@example.com", "myplace.example.com", "555-3999");
     }

     public static final DataProvider MENU = () ->
         Collections.singletonMap("menu", new Menu(
            company3(),
            new Menu.MenuSection("Breakfast", 
                  new Menu.MenuItem("Toast with jam", "Ask about our delicious array of fruit jams", "$10.00"),
                  new Menu.MenuItem("Vegemite on sourdough", "Who can resist our vegemite laden bread", "$9.20"),
                  new Menu.MenuItem("Eggs your way", "Choose from fried, poached or scrambled", "$15.00"),
                  new Menu.MenuItem("Porridge", "Simply irresistible with berries", "$5.80"),
                  new Menu.MenuItem("Big breakfast", "Sausages, eggs, toast and mushroom goodness", "$18.00"),
                  new Menu.MenuItem("Pancakes", "Served with ice-cream on top", "$14.00"),
                  new Menu.MenuItem("French toast", "With the very best maple syrup", "$12.00")),
            new Menu.MenuSection("Lunch",
                  new Menu.MenuItem("Chicken burger", "Yummy chicken piece on a bun", "$19.00"),
                  new Menu.MenuItem("Sandwich", "Choice of fillings including cucumber", "$3.99"),
                  new Menu.MenuItem("Vegetarian curry", "Not too hot, not too mild", "$15.00"),
                  new Menu.MenuItem("Chips", "Get healthy with our sweet-potato fries", "$9.00"))));
}
