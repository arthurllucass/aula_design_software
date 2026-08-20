# Visão parcial

```mermaid
classDiagram
 class EventHubService
 class Event
 class Attendee
 class Ticket
 class Venue
 class PaymentLegacyGateway
 class QrCodeLegacyApi
 EventHubService --> Event
 EventHubService --> Ticket
 EventHubService --> PaymentLegacyGateway
 EventHubService --> QrCodeLegacyApi
```
