package br.edu.eventhub.patterns.facade;
import br.edu.eventhub.service.EventHubService;
public class EventHubFacade {
 public final EventHubService service;
 public EventHubFacade(EventHubService s){service=s;}
 public void cancel(String eventId){service.cancelEvent(eventId);}
 public EventHubService getService(){return service;}
}
