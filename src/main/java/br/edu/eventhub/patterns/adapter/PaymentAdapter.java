package br.edu.eventhub.patterns.adapter;
import br.edu.eventhub.legacy.PaymentLegacyGateway;
public class PaymentAdapter extends PaymentLegacyGateway {
 public boolean pay(String customer,double amount){return charge(customer,amount).startsWith("0;");}
 public String raw(String customer,double amount){return charge(customer,amount);}
}
