package br.edu.eventhub.patterns.observer;

public class OrganizerObserver implements EventObserver {
    public void update(String id, String event) {
        System.out.println("ORGANIZER " + id + " " + event);
    }
}
