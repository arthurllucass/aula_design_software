package br.edu.eventhub.patterns.strategy;
public class VipPricing implements PricingStrategy {
 public double calculate(double basePrice){return basePrice*2.0;}
}
