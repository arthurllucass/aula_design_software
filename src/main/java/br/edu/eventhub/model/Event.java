package br.edu.eventhub.model;

import java.util.*;

public class Event {
    public String id;
    public String name;
    public String venueId;
    public String status = "PLANNED";
    public int capacity;
    public List<String> attendeeIds = new ArrayList<>();

    public Event(String id, String name, String venueId, int capacity) {
        this.id = id;
        this.name = name;
        this.venueId = venueId;
        this.capacity = capacity;
    }
}
