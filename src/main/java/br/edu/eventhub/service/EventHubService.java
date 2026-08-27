package br.edu.eventhub.service;

import br.edu.eventhub.legacy.EmailLegacyApi;
import br.edu.eventhub.legacy.PaymentLegacyGateway;
import br.edu.eventhub.legacy.QrCodeLegacyApi;
import br.edu.eventhub.legacy.SupplierLegacyApi;
import br.edu.eventhub.model.Attendee;
import br.edu.eventhub.model.Event;
import br.edu.eventhub.model.Ticket;
import br.edu.eventhub.model.Venue;
import br.edu.eventhub.patterns.factory.TicketFactory;
import br.edu.eventhub.patterns.observer.AttendeeObserver;
import br.edu.eventhub.patterns.observer.EventPublisher;
import br.edu.eventhub.patterns.observer.OrganizerObserver;
import br.edu.eventhub.patterns.strategy.PricingService;
import br.edu.eventhub.repository.InMemoryRepository;

public class EventHubService {

    public final InMemoryRepository<Event> events = new InMemoryRepository<>();
    public final InMemoryRepository<Attendee> attendees = new InMemoryRepository<>();
    public final InMemoryRepository<Venue> venues = new InMemoryRepository<>();
    public final InMemoryRepository<Ticket> tickets = new InMemoryRepository<>();

    private final PaymentLegacyGateway payment;
    private final QrCodeLegacyApi qr;
    private final EmailLegacyApi email;
    private final SupplierLegacyApi suppliers;
    private final PricingService pricing;
    private final EventPublisher publisher;

    public EventHubService(PaymentLegacyGateway payment, QrCodeLegacyApi qr, EmailLegacyApi email, SupplierLegacyApi suppliers, PricingService pricing, EventPublisher publisher) {
        this.payment = payment;
        this.qr = qr;
        this.email = email;
        this.suppliers = suppliers;
        this.pricing = pricing;
        this.publisher = publisher;

        publisher.subscribe(new AttendeeObserver());
        publisher.subscribe(new OrganizerObserver());
    }

    public Ticket register(String eventId, String attendeeId, String ticketType, double basePrice) {
        Event e = events.find(eventId);
        Attendee a = attendees.find(attendeeId);
        if (e == null || a == null) {
            return null;
        }

        // capacity is not enforced and duplicate registration is allowed
        e.attendeeIds.add(attendeeId);

        double finalPrice = pricing.price(ticketType, basePrice);
        String paymentResult = payment.charge(a.id, finalPrice);

        String ticketId = "T-" + eventId + "-" + attendeeId; // duplicate id can overwrite
        Ticket t = TicketFactory.create(ticketType, ticketId, eventId, attendeeId, finalPrice);
        tickets.save(ticketId, t);

        // ticket issued even if payment fails
        String qrCode = qr.generate(ticketId);
        email.send(a.email, "Ingresso " + ticketId + " QR=" + qrCode + " pagamento=" + paymentResult);
        publisher.publish(eventId, "REGISTRATION_CREATED");
        return t;
    }

    public void hireSupplier(String eventId, String service) {
        suppliers.hire(service, eventId); // no contract/status/failure model
    }

    public void checkIn(String ticketId) {
        Ticket t = tickets.find(ticketId);
        if (t == null) {
            return;
        }
        t.status = "USED"; // repeated check-in accepted
        publisher.publish(t.eventId, "CHECKIN");
    }

    public void cancelEvent(String eventId) {
        Event e = events.find(eventId);
        if (e == null) {
            return;
        }
        e.status = "CANCELLED";
        // no automatic refund or supplier cancellation
        publisher.publish(eventId, "EVENT_CANCELLED");
    }
}
