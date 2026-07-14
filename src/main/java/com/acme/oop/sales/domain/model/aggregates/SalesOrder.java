package com.acme.oop.sales.domain.model.aggregates;

import com.acme.oop.sales.domain.model.valueobjects.ProductId;
import com.acme.oop.shared.domain.model.valueobjects.CustomerId;
import com.acme.oop.shared.domain.model.valueobjects.Money;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class SalesOrder {
    private final UUID id;
    private final CustomerId customerId;
    private LocalDateTime orderDate;
    private final List<SalesOrderItem> items;
    private Money totalAmount;

    public SalesOrder(@NonNull CustomerId customerId) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.orderDate = LocalDateTime.now();
        this.items = new ArrayList<>();
        this.totalAmount = Money.zero();
    }

    public void addItem(@NonNull ProductId productId, int quantity, @NonNull Money unitPrice) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        if (unitPrice.amount().compareTo(Money.zero().amount()) <= 0)
            throw new IllegalArgumentException("Unit price must be greater than zero");
        SalesOrderItem newItem = new SalesOrderItem(productId, quantity, unitPrice);
        this.items.add(newItem);
        this.totalAmount = calculateTotalAmount();
    }

    public Money calculateTotalAmount() {
        return this.items.stream().map(SalesOrderItem::calculateItemAmount).reduce(Money.zero(), Money::add);
    }

    public SalesOrder withOrderDate(@NonNull LocalDateTime orderDate) {
        this.orderDate = orderDate;
        return this;
    }

    public String getTotalAmountAsString() {
        return this.totalAmount.amount() + " " + this.totalAmount.currency().getCurrencyCode();
    }
}
