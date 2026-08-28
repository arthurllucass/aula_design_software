package br.edu.eventhub.patterns.observer;

public interface EventObserver {

    void update(String eventId, String event);
}
