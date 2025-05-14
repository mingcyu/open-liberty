/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.*;

@Embeddable
public class Kitchen {

    private double length;
    private double width;

// Getters and Setters
    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }
}
