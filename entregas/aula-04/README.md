# Aula 04 — decisões de design

## Participantes
- Arthur Lucas P. Santos
- Augusto Alencar
- Artur Akira

---

## P01 — Dependências recebidas no construtor

### Decisão
O `EventHubService` deixa de criar as próprias dependências e passa a recebê-las pelo construtor.
Quem monta os objetos e liga tudo é o `Main`.

### Motivo
A classe dava `new` nas seis coisas que usava. Isso significa que ninguém de fora tinha como trocar
nenhuma delas: pra testar uma inscrição você era obrigado a cobrar de verdade e mandar e-mail de verdade,
porque o serviço sempre fabricava o gateway e a API de e-mail reais. Trocar qualquer integração exigia
editar o serviço.

### Antes
```java
private final PaymentLegacyGateway payment = new PaymentLegacyGateway();
private final QrCodeLegacyApi qr = new QrCodeLegacyApi();
private final EmailLegacyApi email = new EmailLegacyApi();
private final SupplierLegacyApi suppliers = new SupplierLegacyApi();
private final PricingService pricing = new PricingService();
private final EventPublisher publisher = new EventPublisher();

public EventHubService() {
    publisher.subscribe(new AttendeeObserver());
    publisher.subscribe(new OrganizerObserver()); // replaces attendee
}
```

### Depois
```java
private final PaymentLegacyGateway payment;
private final QrCodeLegacyApi qr;
private final EmailLegacyApi email;
private final SupplierLegacyApi suppliers;
private final PricingService pricing;
private final EventPublisher publisher;

public EventHubService(PaymentLegacyGateway payment, QrCodeLegacyApi qr, EmailLegacyApi email,
                       SupplierLegacyApi suppliers, PricingService pricing, EventPublisher publisher) {
    this.payment = payment;
    this.qr = qr;
    this.email = email;
    this.suppliers = suppliers;
    this.pricing = pricing;
    this.publisher = publisher;

    publisher.subscribe(new AttendeeObserver());
    publisher.subscribe(new OrganizerObserver());
}
```

E no `Main`, que virou o lugar onde tudo é montado:

```java
EventHubService s = new EventHubService(new PaymentLegacyGateway(), new QrCodeLegacyApi(),
    new EmailLegacyApi(), new SupplierLegacyApi(), new PricingService(), new EventPublisher());
```docs/adr/ADR-0001-arquitetura.md

### Alternativas
- **Criar interfaces para cada dependência** — melhor no longo prazo, mas exigiria mexer nas classes
  `legacy/`, que representam sistemas externos que não controlamos.

### Consequências
- O `Main` ficou com a responsabilidade de montar os objetos, e a chamada do construtor ficou longa.

---

## P02 — `register()` dividido em métodos menores

### Decisão
Cada passo do `register()` virou um método privado curto, e o `register()` passou a ser a lista desses
passos. O `checkIn()` e o `cancelEvent()` também usam os métodos novos.

### Motivo
O `register()` fazia sete coisas em sequência num bloco só: inscrever a pessoa, calcular o preço, cobrar,
montar o id, criar e salvar o ingresso, gerar o QR, mandar o e-mail e notificar. Num método assim é difícil
enxergar onde uma etapa termina e a outra começa.

### Antes
```java
public Ticket register(String eventId, String attendeeId, String ticketType, double basePrice) {
    Event e = events.find(eventId);
    Attendee a = attendees.find(attendeeId);
    if (e == null || a == null) {
        return null;
    }

    e.attendeeIds.add(attendeeId);

    double finalPrice = pricing.price(ticketType, basePrice);
    String paymentResult = payment.charge(a.id, finalPrice);

    String ticketId = "T-" + eventId + "-" + attendeeId;
    Ticket t = TicketFactory.create(ticketType, ticketId, eventId, attendeeId, finalPrice);
    tickets.save(ticketId, t);

    String qrCode = qr.generate(ticketId);
    email.send(a.email, "Ingresso " + ticketId + " QR=" + qrCode + " pagamento=" + paymentResult);
    publisher.publish(eventId, "REGISTRATION_CREATED");
    return t;
}
```

### Depois
```java
public Ticket register(String eventId, String attendeeId, String ticketType, double basePrice) {
    Event e = events.find(eventId);
    Attendee a = attendees.find(attendeeId);
    if (e == null || a == null) {
        return null;
    }

    addAttendee(e, attendeeId);

    double finalPrice = calculatePrice(ticketType, basePrice);
    String paymentResult = charge(a, finalPrice);

    Ticket ticket = createTicket(eventId, attendeeId, ticketType, finalPrice);
    sendTicketEmail(a, ticket, paymentResult);

    notify(eventId, "REGISTRATION_CREATED");
    return ticket;
}
```

Os métodos auxiliares, todos `private` e curtos:

```java
private void addAttendee(Event e, String attendeeId) { ... }
private double calculatePrice(String ticketType, double basePrice) { ... }
private String charge(Attendee a, double price) { ... }
private String buildTicketId(String eventId, String attendeeId) { ... }
private Ticket createTicket(String eventId, String attendeeId, String ticketType, double price) { ... }
private void sendTicketEmail(Attendee a, Ticket ticket, String paymentResult) { ... }
private void markAsUsed(Ticket ticket) { ... }
private void notify(String eventId, String event) { ... }
```

### Alternativas
- **Quebrar o `EventHubService` em vários serviços** (inscrição, pagamento, check-in), é o caminho certo,
  mas mexe em quase todo o projeto. 

### Consequências
- O `register()` agora se lê de cima a baixo como a sequência do negócio.
- A classe ganhou oito métodos, ficando mais comprida.

---

## Evidências

- **Repositório:** https://github.com/arthurllucass/aula_design_software
- **Branch:** `aula-04`
- **Commits:**
  - `aula-04: P01 dependencias do EventHubService recebidas no construtor e injetadas no Main`
  - `aula-04: P02 register dividido em metodos menores por responsabilidade`
  - `aula-04: ignora a pasta .idea e os arquivos de compilacao`
- **Arquivos:** `src/main/java/br/edu/eventhub/service/EventHubService.java` e `Main.java`

### Compilação

```bash
$ javac -d out $(find src/main/java -name "*.java")
$ echo $?
0
```

### Saída do `Main`

```bash
$ java -cp out br.edu.eventhub.Main
EMAIL a1@exemplo.com => Ingresso T-E1-A1 QR=QR::T-E1-A1 pagamento=0;PAID
ORGANIZER E1 REGISTRATION_CREATED
EMAIL a2@exemplo.com => Ingresso T-E1-A2 QR=QR::T-E1-A2 pagamento=0;PAID
ORGANIZER E1 REGISTRATION_CREATED
EMAIL a1@exemplo.com => Ingresso T-E1-A1 QR=QR::T-E1-A1 pagamento=0;PAID
ORGANIZER E1 REGISTRATION_CREATED
ORGANIZER E1 CHECKIN
ORGANIZER E1 CHECKIN
ORGANIZER E1 EVENT_CANCELLED
INSCRITOS=3
STATUS=CANCELLED
```
