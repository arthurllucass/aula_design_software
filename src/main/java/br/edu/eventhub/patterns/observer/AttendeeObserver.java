package br.edu.eventhub.patterns.observer;
public class AttendeeObserver implements EventObserver {
 public void update(String id,String event){System.out.println("ATTENDEE "+id+" "+event);}
}
