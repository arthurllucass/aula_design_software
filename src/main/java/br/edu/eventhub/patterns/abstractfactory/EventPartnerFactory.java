package br.edu.eventhub.patterns.abstractfactory;
import br.edu.eventhub.legacy.*;
public class EventPartnerFactory {
 public Object payment(String family){return new PaymentLegacyGateway();}
 public Object ticketing(String family){return new QrCodeLegacyApi();}
 public Object supplier(String family){return new SupplierLegacyApi();}
}
