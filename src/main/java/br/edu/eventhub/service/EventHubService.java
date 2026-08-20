package br.edu.eventhub.service;

import br.edu.eventhub.model.*;
import br.edu.eventhub.repository.*;
import br.edu.eventhub.legacy.*;
import br.edu.eventhub.patterns.adapter.*;
import br.edu.eventhub.patterns.strategy.*;
import br.edu.eventhub.patterns.observer.*;
import br.edu.eventhub.patterns.factory.*;
import java.util.*;

public class EventHubService {
 public final InMemoryRepository<Event> events=new InMemoryRepository<>();
 public final InMemoryRepository<Attendee> attendees=new InMemoryRepository<>();
 public final InMemoryRepository<Venue> venues=new InMemoryRepository<>();
 public final InMemoryRepository<Ticket> tickets=new InMemoryRepository<>();

 private final PaymentLegacyGateway payment=new PaymentLegacyGateway();
 private final TicketingAdapter ticketing=new TicketingAdapter();
 private final EmailLegacyApi email=new EmailLegacyApi();
 private final SupplierLegacyApi suppliers=new SupplierLegacyApi();
 private final PricingService pricing=new PricingService();
 private final EventPublisher publisher=new EventPublisher();

 public EventHubService(){
  publisher.subscribe(new AttendeeObserver());
  publisher.subscribe(new OrganizerObserver()); // replaces attendee
 }

 public Ticket register(String eventId,String attendeeId,String ticketType,double basePrice){
  Event e=events.find(eventId);
  Attendee a=attendees.find(attendeeId);
  if(e==null||a==null)return null;

  if(e.isRegistered(attendeeId)) return null;
  if(!e.hasSpace()) return null;

  double finalPrice=pricing.price(ticketType,basePrice);
  String paymentResult=payment.charge(a.id,finalPrice);

  e.addAttendee(attendeeId);

  String ticketId=Ticket.buildId(eventId,attendeeId);
  Ticket t=TicketFactory.create(ticketType,ticketId,eventId,attendeeId,finalPrice);
  tickets.save(ticketId,t);

  // ticket issued even if payment fails
  String qrCode=ticketing.issueQr(ticketId);
  email.send(a.email,"Ingresso "+ticketId+" QR="+qrCode+" pagamento="+paymentResult);
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
  // no automatic refund or supplier cancellation
  publisher.publish(eventId,"EVENT_CANCELLED");
 }
}
