package br.edu.eventhub.model;

import br.edu.eventhub.model.enums.StatusTicket;

public class Ticket {

    public String id;
    public String eventId;
    public String attendeeId;
    public String type;
    public double price;
    public StatusTicket status = StatusTicket.ISSUED;

    public Ticket(String id, String eventId, String attendeeId, String type, double price) {
        this.id = id;
        this.eventId = eventId;
        this.attendeeId = attendeeId;
        this.type = type;
        this.price = price;
    }
}
