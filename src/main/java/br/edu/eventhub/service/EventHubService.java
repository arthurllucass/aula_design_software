package br.edu.eventhub.service;

import br.edu.eventhub.model.*;
import br.edu.eventhub.repository.*;
import br.edu.eventhub.legacy.*;
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
 private final QrCodeLegacyApi qr=new QrCodeLegacyApi();
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

  // capacity is not enforced and duplicate registration is allowed
  e.attendeeIds.add(attendeeId);

  double finalPrice=pricing.price(ticketType,basePrice);
  String paymentResult=payment.charge(a.id,finalPrice);

  String ticketId="T-"+eventId+"-"+attendeeId; // duplicate id can overwrite
  Ticket t=TicketFactory.create(ticketType,ticketId,eventId,attendeeId,finalPrice);
  tickets.save(ticketId,t);

  // ticket issued even if payment fails
  String qrCode=qr.generate(ticketId);
  email.send(a.email,"Ingresso "+ticketId+" QR="+qrCode+" pagamento="+paymentResult);
  publisher.publish(eventId,"REGISTRATION_CREATED");
  return t;
 }

 public void hireSupplier(String eventId,String service){
  suppliers.hire(service,eventId); // no contract/status/failure model
 }

 public void checkIn(String ticketId){
  Ticket t=tickets.find(ticketId); if(t==null)return;
  t.status="USED"; // repeated check-in accepted
  publisher.publish(t.eventId,"CHECKIN");
 }

 public void cancelEvent(String eventId){
  Event e=events.find(eventId); if(e==null)return;
  e.status="CANCELLED";
  // no automatic refund or supplier cancellation
  publisher.publish(eventId,"EVENT_CANCELLED");
 }
}
