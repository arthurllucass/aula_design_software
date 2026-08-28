package br.edu.eventhub.model.enums;

public enum StatusTicket {
    ISSUED ("issued"),
    USED ("used");

    private final String description;

    StatusTicket(String description) {
        this.description = description;
    }
}
