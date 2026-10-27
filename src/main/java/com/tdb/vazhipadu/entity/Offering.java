package com.tdb.vazhipadu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "offering")
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temple_id", nullable = false)
    private Temple temple;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    protected Offering() {
    }

    public Offering(Temple temple, String name, BigDecimal price, boolean active) {
        this(temple, name, price, active, null);
    }

    public Offering(Temple temple, String name, BigDecimal price, boolean active, Integer dailyLimit) {
        this.temple = temple;
        update(name, price, active, dailyLimit);
    }

    public void update(String name, BigDecimal price, boolean active, Integer dailyLimit) {
        this.name = name;
        this.price = price;
        this.active = active;
        this.dailyLimit = dailyLimit;
    }

    public boolean belongsTo(Long templeId) {
        return temple.getId().equals(templeId);
    }

    public boolean hasDailyLimit() {
        return dailyLimit != null;
    }

    public Long getId() {
        return id;
    }

    public Temple getTemple() {
        return temple;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }
}
