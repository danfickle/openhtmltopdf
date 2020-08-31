package com.openhtmltopdf.templates;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.openhtmltopdf.templates.dto.Address;
import com.openhtmltopdf.templates.dto.Company;
import com.openhtmltopdf.templates.dto.Invoice;

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
}
