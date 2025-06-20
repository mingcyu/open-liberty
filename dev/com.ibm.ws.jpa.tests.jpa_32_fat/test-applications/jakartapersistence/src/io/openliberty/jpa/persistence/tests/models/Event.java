package io.openliberty.jpa.persistence.tests.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Event {
    @Id
    public Long id;

    @Column(secondPrecision = 5) 
    public LocalDateTime timestamp;

    public Event() {}
    public Event(Long id, LocalDateTime timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }
}
