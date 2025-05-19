package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Embedded;

@Inheritance
@DiscriminatorColumn(name = "ADDRESS_TYPE")
@DiscriminatorValue("Standard")
@Entity
public class ShippingAddress {

    @Id
    public Long id;

    public String city;

    public String state;

    @Embedded
    public StreetAddress streetAddress;

    public int zipCode;

    
}