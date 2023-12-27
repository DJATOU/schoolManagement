package com.school.management.persistance;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "price")
public class PricingEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description; // A description of the price (e.g., "Standard Math Class Price")

    @Column(name = "price")
    private BigDecimal price;

    // ... constructors, getters, setters...
}
