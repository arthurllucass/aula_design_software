package br.edu.eventhub;

import br.edu.eventhub.legacy.EmailLegacyApi;
import br.edu.eventhub.legacy.PaymentLegacyGateway;
import br.edu.eventhub.legacy.QrCodeLegacyApi;
import br.edu.eventhub.legacy.SupplierLegacyApi;
import br.edu.eventhub.model.Attendee;
import br.edu.eventhub.model.Event;
import br.edu.eventhub.model.Venue;
import br.edu.eventhub.patterns.adapter.PaymentAdapter;
import br.edu.eventhub.patterns.adapter.TicketingAdapter;
import br.edu.eventhub.patterns.facade.EventHubFacade;
import br.edu.eventhub.patterns.observer.EventPublisher;
import br.edu.eventhub.patterns.strategy.PricingService;
import br.edu.eventhub.service.EventHubService;

public class Main {

    public static void main(String[] args) {
        EventHubService s = new EventHubService(new PaymentLegacyGateway(), new QrCodeLegacyApi(),
                new EmailLegacyApi(), new SupplierLegacyApi(), new PricingService(), new EventPublisher());

        s.venues.save("V1", new Venue("V1", "Centro de Convenções", 1));
        s.events.save("E1", new Event("E1", "Tech Summit", "V1", 1));
        s.attendees.save("A1", new Attendee("A1", "Pessoa 1", "a1@exemplo.com"));
        s.attendees.save("A2", new Attendee("A2", "Pessoa 2", "a2@exemplo.com"));

    System.out.println("CAPACIDADE=" + s.events.find("E1").capacity);

    s.register("E1", "A1", "STANDARD", 100.0);
    s.register("E1", "A2", "STANDARD", 100.0);
    s.register("E1", "A1", "STANDARD", 100.0);

    s.checkIn("T-E1-A1");
    s.checkIn("T-E1-A1");
    s.hireSupplier("E1", "SOM");

    EventHubFacade facade = new EventHubFacade(s, new PaymentAdapter(), new TicketingAdapter());
    facade.cancel("E1");

    System.out.println("INSCRITOS=" + facade.getService().events.find("E1").attendeeIds.size());
    System.out.println("STATUS=" + facade.getService().events.find("E1").status);
  }
}
