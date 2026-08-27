package br.edu.eventhub.model.enums;

public enum StatusEvent {

    PLANNED ("planned"),
    CANCELLED ("cancelled");

    private final String description;

    StatusEvent(String description) {
        this.description = description;
    }
}
