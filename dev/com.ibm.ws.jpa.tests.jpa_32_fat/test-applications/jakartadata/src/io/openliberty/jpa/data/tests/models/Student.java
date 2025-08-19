package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 *
 */
@Entity
public class Student {

    @Id
    public long rollNo;

    public String name;

    public int[] marks;

    public Student() {
    }

    public Student(long rollNo, String name, int[] marks) {
        this.rollNo = rollNo;
        this.name = name;
        this.marks = marks;
    }
}
