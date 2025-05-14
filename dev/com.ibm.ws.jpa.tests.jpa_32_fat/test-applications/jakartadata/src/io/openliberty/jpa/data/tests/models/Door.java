/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import java.io.Serializable;

import jakarta.persistence.*;

@Embeddable
public class Door implements Serializable {

    private static final long serialVersionUID = 1L; // Optional: for version control of the serialized object.

    @Column(name = "GARAGE_DOOR_HEIGHT")
    private double height;

    @Column(name = "GARAGE_DOOR_WIDTH")
    private double width;

    // Getters and Setters
    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }
}