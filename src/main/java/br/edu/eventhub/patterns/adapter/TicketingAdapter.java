package br.edu.eventhub.patterns.adapter;

import br.edu.eventhub.legacy.QrCodeLegacyApi;

public class TicketingAdapter {

    private final QrCodeLegacyApi legacy = new QrCodeLegacyApi();

    public String issueQr(String ticketId) {
        return legacy.generate(ticketId);
    }

    public QrCodeLegacyApi legacy() {
        return legacy;
    }
}
