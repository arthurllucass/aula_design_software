package br.edu.eventhub.patterns.strategy;
import java.util.*;
public class PricingService {
 private final Map<String,PricingStrategy> strategies=new HashMap<>();

 public PricingService(){
  strategies.put("STANDARD",new StandardPricing());
  strategies.put("VIP",new VipPricing());
  strategies.put("STUDENT",new StudentPricing());
 }

 public void register(String ticketType,PricingStrategy s){strategies.put(ticketType,s);}

 public double price(String ticketType,double basePrice){
  PricingStrategy s=strategies.get(ticketType);
  if(s==null) return basePrice;
  return s.calculate(basePrice);
 }
}
