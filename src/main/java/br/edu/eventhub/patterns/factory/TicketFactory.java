package br.edu.eventhub.patterns.factory;
import br.edu.eventhub.model.Ticket;
public class TicketFactory {
 public static Ticket create(String type,String id,String eventId,String attendeeId,double price){
  return new Ticket(id,eventId,attendeeId,type,price);
 }
}
