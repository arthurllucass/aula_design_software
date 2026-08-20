package br.edu.eventhub.patterns.observer;
import java.util.*;
public class EventPublisher {
 private final List<EventObserver> observers=new ArrayList<>();
 public void subscribe(EventObserver o){observers.add(o);}
 public void publish(String id,String event){
  for(EventObserver o:observers) o.update(id,event);
 }
}
