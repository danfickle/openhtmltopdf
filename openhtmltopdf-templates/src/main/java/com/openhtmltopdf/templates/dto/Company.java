package com.openhtmltopdf.templates.dto;

public class Company {
    public final String name;
    public final String logoFile;
    public final Address address;
    public final String email;
    public final String website;
    public final String phone;

    public Company(String name, String logoFile, Address address, String email,
                   String website, String phone) {
        this.name = name;
        this.logoFile = logoFile;
        this.address = address;
        this.email = email;
        this.website = website;
        this.phone = phone;
    }
}
