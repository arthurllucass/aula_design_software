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

        if(e.hasAttendee(attendeeId)){
            return null;
        }

        addAttendee(e, attendeeId);

        double finalPrice = calculatePrice(ticketType, basePrice);
        String paymentResult = charge(a, finalPrice);

        Ticket ticket = createTicket(eventId, attendeeId, ticketType, finalPrice);
        sendTicketEmail(a, ticket, paymentResult);

        notify(eventId, "REGISTRATION_CREATED");
        return ticket;
    }

    public void hireSupplier(String eventId, String service) {
        suppliers.hire(service, eventId); // no contract/status/failure model
    }

    public void checkIn(String ticketId) {
        Ticket t = tickets.find(ticketId);
        if (t == null) {
            return;
        }
        if(t.inUsed()){
            return;
        }
        markAsUsed(t);
        notify(t.eventId, "CHECKIN");
    }

    public void cancelEvent(String eventId) {
        Event e = events.find(eventId);
        if (e == null) {
            return;
        }
        e.status = "CANCELLED";
        // no automatic refund or supplier cancellation
        notify(eventId, "EVENT_CANCELLED");
    }

    // capacity is not enforced and duplicate registration is allowed
    private void addAttendee(Event e, String attendeeId) {
        e.attendeeIds.add(attendeeId);
    }

    private double calculatePrice(String ticketType, double basePrice) {
        return pricing.price(ticketType, basePrice);
    }

    private String charge(Attendee a, double price) {
        return payment.charge(a.id, price);
    }

    // duplicate id can overwrite
    private String buildTicketId(String eventId, String attendeeId) {
        return "T-" + eventId + "-" + attendeeId;
    }

    // ticket issued even if payment fails
    private Ticket createTicket(String eventId, String attendeeId, String ticketType, double price) {
        String ticketId = buildTicketId(eventId, attendeeId);
        Ticket ticket = TicketFactory.create(ticketType, ticketId, eventId, attendeeId, price);
        tickets.save(ticketId, ticket);
        return ticket;
    }

    private void sendTicketEmail(Attendee a, Ticket ticket, String paymentResult) {
        String qrCode = qr.generate(ticket.id);
        email.send(a.email, "Ingresso " + ticket.id + " QR=" + qrCode + " pagamento=" + paymentResult);
    }

    // repeated check-in accepted
    private void markAsUsed(Ticket ticket) {
        ticket.status = "USED";
    }

    private void notify(String eventId, String event) {
        publisher.publish(eventId, event);
    }
}
