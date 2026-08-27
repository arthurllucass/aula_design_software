package br.edu.eventhub.patterns.strategy;

public class PricingService {

    private PricingStrategy strategy;

    public void setStrategy(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public double price(String ticketType, double basePrice) {
        if ("VIP".equals(ticketType)) {
            return basePrice * 2.0;
        }
        if ("STUDENT".equals(ticketType)) {
            return basePrice * 0.5;
        }
        return strategy == null ? basePrice : strategy.calculate(basePrice);
    }
}
