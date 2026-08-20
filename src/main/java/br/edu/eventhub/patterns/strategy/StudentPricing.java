package br.edu.eventhub.patterns.strategy;
public class StudentPricing implements PricingStrategy {
 public double calculate(double basePrice){return basePrice*0.5;}
}
