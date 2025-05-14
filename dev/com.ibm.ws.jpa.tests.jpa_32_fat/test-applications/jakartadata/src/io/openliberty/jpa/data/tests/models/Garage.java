/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.*;

@Embeddable
public class Garage {

    public static enum Type {
        Attached, Detached, TuckUnder
    };

    @Column(name = "GARAGE_AREA")
    private double area;

    @Column(name = "GARAGE_TYPE")
    private Type type;

    //@Embedded
    private Door door;

    // Getters and Setters
    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Door getDoor() {
        return door;
    }

    public void setDoor(Door door) {
        this.door = door;
    }
}
