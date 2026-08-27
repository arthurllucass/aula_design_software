package br.edu.eventhub.legacy;

public class PaymentLegacyGateway {

    public String charge(String customer, double amount) {
        return amount > 0 ? "0;PAID" : "9;ERROR";
    }

    public String refund(String customer, double amount) {
        return "0;REFUNDED";
    }
}
