package br.edu.eventhub.model;

import br.edu.eventhub.model.enums.StatusEvent;

import java.util.ArrayList;
import java.util.List;

public class Event {

    public String id;
    public String name;
    public String venueId;
    public StatusEvent status = StatusEvent.PLANNED;
    public int capacity;
    public List<String> attendeeIds = new ArrayList<>();

    public Event(String id, String name, String venueId, int capacity) {
        this.id = id;
        this.name = name;
        this.venueId = venueId;
        this.capacity = capacity;
    }
}
