package com.tdb.vazhipadu.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OfferingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "temple_id", nullable = false)
    private Temple temple;

    @Column(name = "devotee_name", nullable = false, length = 100)
    private String devoteeName;

    @Column(nullable = false, length = 10)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nakshatra nakshatra;

    @Column(name = "offering_date", nullable = false)
    private LocalDate offeringDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    protected OfferingOrder() {
    }

    public OfferingOrder(Temple temple, String devoteeName, String phone, Nakshatra nakshatra,
                         LocalDate offeringDate, Instant createdAt) {
        this.temple = temple;
        this.devoteeName = devoteeName;
        this.phone = phone;
        this.nakshatra = nakshatra;
        this.offeringDate = offeringDate;
        this.createdAt = createdAt;
        this.status = OrderStatus.CONFIRMED;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(Offering offering, int quantity) {
        var item = new OrderItem(this, offering, quantity);
        items.add(item);
        totalAmount = totalAmount.add(item.getLineTotal());
    }

    public void cancel(Instant at) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = at;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Temple getTemple() {
        return temple;
    }

    public String getDevoteeName() {
        return devoteeName;
    }

    public String getPhone() {
        return phone;
    }

    public Nakshatra getNakshatra() {
        return nakshatra;
    }

    public LocalDate getOfferingDate() {
        return offeringDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
