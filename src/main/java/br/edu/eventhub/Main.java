package br.edu.eventhub;
import br.edu.eventhub.model.*;
import br.edu.eventhub.service.*;
import br.edu.eventhub.patterns.facade.*;
import br.edu.eventhub.patterns.adapter.*;

public class Main {
 public static void main(String[] args){
  EventHubService s=new EventHubService();
  s.venues.save("V1",new Venue("V1","Centro de Convenções"));
  s.events.save("E1",new Event("E1","Tech Summit","V1",1));
  s.attendees.save("A1",new Attendee("A1","Pessoa 1","a1@exemplo.com"));
  s.attendees.save("A2",new Attendee("A2","Pessoa 2","a2@exemplo.com"));

  s.register("E1","A1","STANDARD",100.0);
  s.register("E1","A2","STANDARD",100.0); // over capacity
  s.register("E1","A1","STANDARD",100.0); // duplicate registration

  s.checkIn("T-E1-A1");
  s.checkIn("T-E1-A1"); // repeated check-in
  s.hireSupplier("E1","SOM");

  EventHubFacade facade=new EventHubFacade(s,new PaymentAdapter(),new TicketingAdapter());
  facade.cancel("E1");

  System.out.println("INSCRITOS="+facade.getService().events.find("E1").attendeeIds.size());
  System.out.println("STATUS="+facade.getService().events.find("E1").status);
 }
}
