package br.edu.eventhub;
import br.edu.eventhub.model.*;
import br.edu.eventhub.service.*;
import br.edu.eventhub.legacy.*;
import br.edu.eventhub.patterns.facade.*;
import br.edu.eventhub.patterns.adapter.*;
import br.edu.eventhub.patterns.observer.*;
import br.edu.eventhub.patterns.strategy.*;

public class Main {
 public static void main(String[] args){
  EventPublisher publisher=new EventPublisher();
  publisher.subscribe(new AttendeeObserver());
  publisher.subscribe(new OrganizerObserver());

  EventHubService s=new EventHubService(new PaymentAdapter(),new TicketingAdapter(),
    new EmailLegacyApi(),new SupplierLegacyApi(),new PricingService(),publisher);

  s.venues.save("V1",new Venue("V1","Centro de Convenções"));
  s.events.save("E1",new Event("E1","Tech Summit","V1",1));
  s.attendees.save("A1",new Attendee("A1","Pessoa 1","a1@exemplo.com"));
  s.attendees.save("A2",new Attendee("A2","Pessoa 2","a2@exemplo.com"));

  Ticket t1=s.register("E1","A1","STANDARD",100.0);
  s.register("E1","A2","STANDARD",100.0); // deve ser recusado: capacidade cheia
  s.register("E1","A1","STANDARD",100.0); // deve ser recusado: ja inscrito

  s.checkIn(t1.id);
  s.checkIn(t1.id); // deve ser recusado: ingresso ja usado
  s.hireSupplier("E1","SOM");

  EventHubFacade facade=new EventHubFacade(s);
  facade.cancel("E1");

  System.out.println("INSCRITOS="+facade.getService().events.find("E1").attendeeIds.size());
  System.out.println("STATUS="+facade.getService().events.find("E1").status);
 }
}
