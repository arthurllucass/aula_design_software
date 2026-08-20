package br.edu.eventhub.service;

import br.edu.eventhub.model.*;
import br.edu.eventhub.repository.*;
import br.edu.eventhub.legacy.*;
import br.edu.eventhub.patterns.adapter.*;
import br.edu.eventhub.patterns.strategy.*;
import br.edu.eventhub.patterns.observer.*;
import br.edu.eventhub.patterns.factory.*;

public class EventHubService {
 public final InMemoryRepository<Event> events=new InMemoryRepository<>();
 public final InMemoryRepository<Attendee> attendees=new InMemoryRepository<>();
 public final InMemoryRepository<Venue> venues=new InMemoryRepository<>();
 public final InMemoryRepository<Ticket> tickets=new InMemoryRepository<>();

 private final PaymentAdapter payment;
 private final TicketingAdapter ticketing;
 private final EmailLegacyApi email;
 private final SupplierLegacyApi suppliers;
 private final PricingService pricing;
 private final EventPublisher publisher;

 public EventHubService(PaymentAdapter payment,TicketingAdapter ticketing,EmailLegacyApi email,
                        SupplierLegacyApi suppliers,PricingService pricing,EventPublisher publisher){
  this.payment=payment; this.ticketing=ticketing; this.email=email;
  this.suppliers=suppliers; this.pricing=pricing; this.publisher=publisher;
 }

 public Ticket register(String eventId,String attendeeId,String ticketType,double basePrice){
  Event e=events.find(eventId);
  Attendee a=attendees.find(attendeeId);
  if(e==null||a==null)return null;

  if(e.isRegistered(attendeeId)) return null;
  if(!e.hasSpace()) return null;

  double finalPrice=pricing.price(ticketType,basePrice);
  boolean paid=payment.pay(a.id,finalPrice);

  e.addAttendee(attendeeId);

  String ticketId=Ticket.buildId(eventId,attendeeId);
  Ticket t=TicketFactory.create(ticketType,ticketId,eventId,attendeeId,finalPrice);
  tickets.save(ticketId,t);

  // o ingresso ainda sai mesmo se o pagamento for recusado (P10, fica pra proxima aula)
  String qrCode=ticketing.issueQr(ticketId);
  email.send(a.email,"Ingresso "+ticketId+" QR="+qrCode+" pagamento="+paid);
  publisher.publish(eventId,"REGISTRATION_CREATED");
  return t;
 }

 public void hireSupplier(String eventId,String service){
  suppliers.hire(service,eventId); // no contract/status/failure model
 }

 public void checkIn(String ticketId){
  Ticket t=tickets.find(ticketId); if(t==null)return;
  if(t.isUsed()) return;
  t.markUsed();
  publisher.publish(t.eventId,"CHECKIN");
 }

 public void cancelEvent(String eventId){
  Event e=events.find(eventId); if(e==null)return;
  e.status="CANCELLED";

  for(Ticket t:tickets.all()){
   if(t.eventId.equals(eventId)&&!"REFUNDED".equals(t.status)){
    payment.refund(t.attendeeId,t.price);
    t.status="REFUNDED";
   }
  }
  publisher.publish(eventId,"EVENT_CANCELLED");
 }
}
