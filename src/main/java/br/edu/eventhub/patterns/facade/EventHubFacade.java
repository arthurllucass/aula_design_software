package br.edu.eventhub.patterns.facade;

import br.edu.eventhub.patterns.adapter.PaymentAdapter;
import br.edu.eventhub.patterns.adapter.TicketingAdapter;
import br.edu.eventhub.service.EventHubService;

public class EventHubFacade {

    public final EventHubService service;
    public final PaymentAdapter payment;
    public final TicketingAdapter ticketing;

    public EventHubFacade(EventHubService s, PaymentAdapter p, TicketingAdapter t) {
        service = s;
        payment = p;
        ticketing = t;
    }

    public void cancel(String eventId) {
        service.cancelEvent(eventId);
    }

    public EventHubService getService() {
        return service;
    }
}
