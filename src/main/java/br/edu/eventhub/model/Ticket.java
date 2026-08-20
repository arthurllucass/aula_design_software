package br.edu.eventhub.model;
public class Ticket {
 public String id; public String eventId; public String attendeeId; public String type; public double price; public String status="ISSUED";
 public Ticket(String id,String eventId,String attendeeId,String type,double price){
  this.id=id;this.eventId=eventId;this.attendeeId=attendeeId;this.type=type;this.price=price;
 }
 public static String buildId(String eventId,String attendeeId){return "T-"+eventId+"-"+attendeeId;}
 public boolean isUsed(){return "USED".equals(status);}
 public void markUsed(){status="USED";}
}
