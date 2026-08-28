package br.edu.eventhub.patterns.abstractfactory;

import br.edu.eventhub.legacy.PaymentLegacyGateway;
import br.edu.eventhub.legacy.QrCodeLegacyApi;
import br.edu.eventhub.legacy.SupplierLegacyApi;

public class EventPartnerFactory {

    public Object payment(String family) {
        return new PaymentLegacyGateway();
    }

    public Object ticketing(String family) {
        return new QrCodeLegacyApi();
    }

    public Object supplier(String family) {
        return new SupplierLegacyApi();
    }
}
