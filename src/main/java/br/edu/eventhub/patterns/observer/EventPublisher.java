package br.edu.eventhub.patterns.observer;

public class EventPublisher {

    private EventObserver observer;

    public void subscribe(EventObserver o) {
        observer = o;
    }

    public void publish(String id, String event) {
        if (observer != null) {
            observer.update(id, event);
    
        }}
}
