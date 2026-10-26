package com.soilsync.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private double price;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private int stock;

    @Column(length = 800)
    private String recommendedUse;

    public Product() {
    }

    public Product(String name, String category, double price, String description, String unit, int stock, String recommendedUse) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.unit = unit;
        this.stock = stock;
        this.recommendedUse = recommendedUse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getRecommendedUse() {
        return recommendedUse;
    }

    public void setRecommendedUse(String recommendedUse) {
        this.recommendedUse = recommendedUse;
    }
}
