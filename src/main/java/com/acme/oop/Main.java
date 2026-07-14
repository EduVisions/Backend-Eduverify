package com.acme.oop;

import com.acme.oop.crm.domain.model.aggregates.Customer;
import com.acme.oop.sales.domain.model.aggregates.SalesOrder;
import com.acme.oop.sales.domain.model.valueobjects.ProductId;
import com.acme.oop.shared.domain.model.valueobjects.Address;
import com.acme.oop.shared.domain.model.valueobjects.Money;

import java.math.BigDecimal;
import java.util.Currency;

public class Main {
    static void main() {
        // Shared context
        Address address = new Address("123 Main St", "Anytown", "12345", "USA");
        System.out.println("First Address: " + address);
        Address anotherAddress = new Address("456 Main St", "Anytown", "12345", "USA");
        System.out.println("Second Address: " + anotherAddress);
        // CRM context
        System.out.println("Creating a customer...");
        Customer customer = new Customer("John Doe", "john.doe@gmail.com", address);
        System.out.println("Customer Contact Info: " + customer.getContactInfo());
        System.out.println("Updating customer contact info...");
        customer.updateContactInfo(customer.getEmail(), anotherAddress);
        System.out.println("Customer Contact Info: " + customer.getContactInfo());
        // Sales context
        System.out.println("Creating a sales order...");
        SalesOrder order = new SalesOrder(customer.getId());
        Money price = new Money(new BigDecimal("29.99"), Currency.getInstance("USD"));
        ProductId productId = new ProductId();
        order.addItem(productId, 2, price);
        System.out.println("Order ID: " + order.getId());
        System.out.println("Order Date: " + order.getOrderDate());
        System.out.println("Customer ID: " + order.getCustomerId());
        System.out.println("Total Amount: " + order.getTotalAmountAsString());

    }
}