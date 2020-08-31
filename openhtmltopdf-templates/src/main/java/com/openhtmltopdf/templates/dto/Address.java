package com.openhtmltopdf.templates.dto;

public class Address {
    public final String line1;
    public final String line2;
    public final String city;
    public final String state;
    public final String country;
    public final String zipCode;

    public Address(String line1, String line2, String city, String state, String country, String zipCode) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipCode = zipCode;
    }
}
