# Evidência da entrega

## Aula
Aula 03 — Problemas de design no legado

## Participantes

| Integrante | O que analisou |
|---|---|
| Arthur Lucas P. Santos | Coordenação, pacote `patterns/` (Observer, Strategy, Facade e as factories) e redação do documento |
| Augusto Alencar | Fluxo de inscrição e pagamento: `EventHubService.register`, `PricingService` e os modelos |
| Artur Akira | Check-in, cancelamento e notificações: `checkIn`, `cancelEvent`, `EventPublisher` e o `Main` |

---

## Problema observado

O EventHub compila e roda, mas o design está bagunçado de um jeito bem visível. Separamos os achados nas
cinco categorias pedidas na atividade: **acoplamento, baixa coesão, duplicação, responsabilidades confusas e
antipadrões**. Todos apontam arquivo e linha, e os que dão pra provar rodando estão marcados com a saída do
`Main` no fim do documento.

### Acoplamento

**P01 — O serviço dá `new` em tudo que precisa** · `service/EventHubService.java:17-22`
São seis dependências criadas dentro da própria classe: `PaymentLegacyGateway`, `QrCodeLegacyApi`,
`EmailLegacyApi`, `SupplierLegacyApi`, `PricingService` e `EventPublisher`. Nada é recebido de fora e nenhuma
delas é uma interface. Pra testar a inscrição você é obrigado a cobrar de verdade e mandar e-mail de verdade,
e trocar qualquer integração significa editar o serviço.

Como está:
```java
private final PaymentLegacyGateway payment=new PaymentLegacyGateway();
private final QrCodeLegacyApi qr=new QrCodeLegacyApi();
private final EmailLegacyApi email=new EmailLegacyApi();
private final SupplierLegacyApi suppliers=new SupplierLegacyApi();
private final PricingService pricing=new PricingService();
private final EventPublisher publisher=new EventPublisher();
```

Como fica (as dependências chegam pelo construtor, e quem monta tudo é o `Main`):
```java
private final PaymentAdapter payment;
private final TicketingAdapter ticketing;
private final EmailLegacyApi email;
private final SupplierLegacyApi suppliers;
private final PricingService pricing;
private final EventPublisher publisher;

public EventHubService(PaymentAdapter payment,TicketingAdapter ticketing,EmailLegacyApi email,
                       SupplierLegacyApi suppliers,PricingService pricing,EventPublisher publisher){
 this.payment=payment; this.ticketing=ticketing; this.email=email;
 this.suppliers=suppliers; this.pricing=pricing; this.publisher=publisher;
}
```

### Baixa coesão

**P04 — `EventHubService` faz de tudo** · `service/EventHubService.java:11-67`
Em 67 linhas a classe cuida de: guardar dados (4 repositórios), calcular preço, cobrar, gerar QR Code, mandar
e-mail, notificar, contratar fornecedor, fazer check-in e cancelar evento. Só o `register()` (linhas 29-49)
faz seis coisas diferentes. Qualquer mudança em qualquer um desses assuntos cai no mesmo arquivo.

Como está (só os cabeçalhos, pra ver o tamanho do assunto):
```java
public class EventHubService {
 public final InMemoryRepository<Event> events;      // dados
 public final InMemoryRepository<Attendee> attendees;
 public final InMemoryRepository<Venue> venues;
 public final InMemoryRepository<Ticket> tickets;

 public Ticket register(...)   // preço + cobrança + ingresso + QR + e-mail + notificação
 public void hireSupplier(...) // fornecedor
 public void checkIn(...)      // portaria
 public void cancelEvent(...)  // cancelamento
}
```

Não corrigimos nesta aula: quebrar isso em serviços menores mexe em quase todo o projeto e fica pra próxima.

**P05 — Os modelos são só sacos de dados** · `model/Event.java`, `model/Ticket.java`, `model/Attendee.java`, `model/Venue.java`
Todos têm só campos públicos e nenhum método.

Como está:
```java
public class Event {
 public String id; public String name; public String venueId; public String status="PLANNED";
 public int capacity; public List<String> attendeeIds=new ArrayList<>();
}
```

Como fica (as perguntas que são do próprio objeto voltam pra ele):
```java
// Event.java
public boolean hasSpace(){return attendeeIds.size()<capacity;}
public boolean isRegistered(String attendeeId){return attendeeIds.contains(attendeeId);}
public void addAttendee(String attendeeId){attendeeIds.add(attendeeId);}

// Ticket.java
public boolean isUsed(){return "USED".equals(status);}
public void markUsed(){status="USED";}
```

E o serviço passa a perguntar em vez de decidir sozinho:
```java
if(e.isRegistered(attendeeId)) return null;
if(!e.hasSpace()) return null;
...
if(t.isUsed()) return;    // no checkIn
```

### Duplicação

**P06 — Capacidade está guardada em dois lugares** · `model/Event.java:5` e `model/Venue.java:3`
`Event.capacity` e `Venue.capacity` coexistem, sem nada dizendo qual manda.

Como está:
```java
public class Event { ... public int capacity; ... }
public class Venue { public String id; public String name; public int capacity; }
```

Como fica (a capacidade que vale é a do evento; o `Venue` deixa de ter o campo):
```java
public class Venue { public String id; public String name; }
```

**P07 — O id do ingresso é montado em um lugar e escrito na mão em outro** · `service/EventHubService.java:40`, `Main.java:19-20`
O serviço monta `"T-"+eventId+"-"+attendeeId`, e o `Main` chama `checkIn("T-E1-A1")` com a string digitada
direto. Se o formato mudar, o `Main` quebra.

Como está:
```java
String ticketId="T-"+eventId+"-"+attendeeId;   // EventHubService
s.checkIn("T-E1-A1");                          // Main, string na mão
```

Como fica (um lugar só monta o id, e o `Main` usa o ingresso que recebeu):
```java
// Ticket.java
public static String buildId(String eventId,String attendeeId){return "T-"+eventId+"-"+attendeeId;}

// EventHubService
String ticketId=Ticket.buildId(eventId,attendeeId);

// Main
Ticket t1=s.register("E1","A1","STANDARD",100.0);
s.checkIn(t1.id);
```

**P08 — Dois caminhos pra mesma API de QR Code** · `service/EventHubService.java:18,45` e `patterns/adapter/TicketingAdapter.java:4-6`
O serviço usa `QrCodeLegacyApi` direto, enquanto o `TicketingAdapter.issueQr()` faz exatamente a mesma coisa —
e nunca é chamado por ninguém. São duas portas pro mesmo legado, uma delas inútil.

Como está:
```java
private final QrCodeLegacyApi qr=new QrCodeLegacyApi();   // EventHubService
String qrCode=qr.generate(ticketId);

public String issueQr(String ticketId){return legacy.generate(ticketId);}   // TicketingAdapter, sem uso
```

Como fica (sobra uma porta só):
```java
String qrCode=ticketing.issueQr(ticketId);
```

### Responsabilidades confusas

**P09 — Não dá pra saber quem é o dono do pagamento** · `patterns/facade/EventHubFacade.java:5-11`
A fachada recebe `PaymentAdapter` e `TicketingAdapter` no construtor, guarda em campos públicos e **não usa
nenhum dos dois**. Quem realmente cobra é o `EventHubService`, com outra instância, criada por ele mesmo. Dois
lugares parecem responsáveis pelo pagamento e nenhum é de fato.

Como está:
```java
public final EventHubService service;
public final PaymentAdapter payment;      // nunca usado
public final TicketingAdapter ticketing;  // nunca usado
public EventHubFacade(EventHubService s,PaymentAdapter p,TicketingAdapter t){
 service=s;payment=p;ticketing=t;
}
```

Como fica (a fachada fica só com o que ela usa; quem cobra é o serviço, e agora recebe o adapter de fora):
```java
public final EventHubService service;
public EventHubFacade(EventHubService s){service=s;}
```

**P10 — Regra de negócio e infraestrutura misturadas no mesmo método** · `service/EventHubService.java:29-49`
O `register()` valida (ou deveria), calcula preço, cobra, cria o ingresso, salva, gera QR, manda e-mail e
notifica, tudo em sequência.

Como está:
```java
double finalPrice=pricing.price(ticketType,basePrice);
String paymentResult=payment.charge(a.id,finalPrice);   // resultado só vira texto
...
tickets.save(ticketId,t);
String qrCode=qr.generate(ticketId);
email.send(a.email,"Ingresso "+ticketId+" QR="+qrCode+" pagamento="+paymentResult);
publisher.publish(eventId,"REGISTRATION_CREATED");
```

Não corrigimos nesta aula: separar regra de infraestrutura depende de quebrar a `EventHubService` (P04), então
os dois andam juntos na próxima. Fica registrado que, por causa disso, o ingresso continua saindo mesmo quando
o pagamento é recusado.

### Antipadrões

**P11 — Observer que não observa** · `patterns/observer/EventPublisher.java:3-5`
O campo é `private EventObserver observer` (um só) e o `subscribe()` **substitui** em vez de adicionar. Em
`EventHubService.java:25-26` o `AttendeeObserver` é registrado e apagado na linha seguinte pelo
`OrganizerObserver` — tem até um comentário `// replaces attendee` assumindo isso. Na saída não aparece nenhum
`ATTENDEE`: o participante nunca é notificado.

Como está:
```java
private EventObserver observer;
public void subscribe(EventObserver o){observer=o;}
public void publish(String id,String event){if(observer!=null)observer.update(id,event);}
```

Como fica:
```java
private final List<EventObserver> observers=new ArrayList<>();
public void subscribe(EventObserver o){observers.add(o);}
public void publish(String id,String event){
 for(EventObserver o:observers) o.update(id,event);
}
```

**P12 — Strategy que é um if/else com outro nome** · `patterns/strategy/PricingService.java:5-9`
A interface `PricingStrategy` existe, mas o preço sai de `if("VIP")` / `if("STUDENT")`. A estratégia injetada
só é consultada como plano B, e o `setStrategy()` nunca é chamado. Pra adicionar um tipo de ingresso é preciso
editar o `if`.

Como está:
```java
public double price(String ticketType,double basePrice){
 if("VIP".equals(ticketType)) return basePrice*2.0;
 if("STUDENT".equals(ticketType)) return basePrice*0.5;
 return strategy==null?basePrice:strategy.calculate(basePrice);
}
```

Como fica (cada tipo vira uma classe e entra num mapa; tipo novo não mexe em `if`):
```java
private final Map<String,PricingStrategy> strategies=new HashMap<>();

public PricingService(){
 strategies.put("STANDARD",new StandardPricing());
 strategies.put("VIP",new VipPricing());
 strategies.put("STUDENT",new StudentPricing());
}

public void register(String ticketType,PricingStrategy s){strategies.put(ticketType,s);}

public double price(String ticketType,double basePrice){
 PricingStrategy s=strategies.get(ticketType);
 if(s==null) return basePrice;
 return s.calculate(basePrice);
}
```

**P13 — Factories que não fabricam nada** · `patterns/factory/TicketFactory.java:4-6`, `patterns/abstractfactory/EventPartnerFactory.java:4-6`
`TicketFactory.create()` recebe o `type` e só repassa pro construtor, sem decidir nada com ele. E a
`EventPartnerFactory` recebe `String family`, **ignora o parâmetro**, devolve `Object` e sempre instancia a
mesma classe legada — além de não ser usada por ninguém.

Como está:
```java
public static Ticket create(String type,String id,String eventId,String attendeeId,double price){
 return new Ticket(id,eventId,attendeeId,type,price);
}

public Object payment(String family){return new PaymentLegacyGateway();}
public Object ticketing(String family){return new QrCodeLegacyApi();}
public Object supplier(String family){return new SupplierLegacyApi();}
```

Não corrigimos nesta aula: as duas só fazem sentido depois de existirem tipos de ingresso e famílias de
parceiro de verdade, o que ainda não temos.

**P14 — Bastante código morto** · vários arquivos
`PaymentLegacyGateway.refund()` nunca é chamado (por isso cancelar evento não estorna), `setStrategy()` nunca
é chamado, `EventPartnerFactory` nunca é instanciada, `TicketingAdapter` é criado no `Main` só pra ser
ignorado, e o `Main` documenta os defeitos em comentário (`// over capacity`, `// duplicate registration`,
`// repeated check-in`) em vez de teste.

Como está:
```java
public void cancelEvent(String eventId){
 Event e=events.find(eventId); if(e==null)return;
 e.status="CANCELLED";
 // no automatic refund or supplier cancellation
 publisher.publish(eventId,"EVENT_CANCELLED");
}

public String raw(String customer,double amount){return charge(customer,amount);}  // PaymentAdapter, sem uso
public void setStrategy(PricingStrategy strategy){this.strategy=strategy;}         // sem uso
```

Como fica (o estorno passa a acontecer, e os métodos sem uso saem):
```java
public void cancelEvent(String eventId){
 Event e=events.find(eventId); if(e==null)return;
 e.status="CANCELLED";

 for(Ticket t:tickets.all()){
  if(t.eventId.equals(eventId)&&!"REFUNDED".equals(t.status)){
   payment.refund(t.attendeeId,t.price);
   t.status="REFUNDED";
  }
 }
 publisher.publish(eventId,"EVENT_CANCELLED");
}
```

O `TicketingAdapter` deixa de ser código morto porque passa a ser o caminho do QR (P08). A
`EventPartnerFactory` continua sem uso, junto com o P13.

---

## Alteração realizada

- **P01** — as dependências passam a ser recebidas no construtor, em vez de `new` dentro da classe;
- **P05** — as regras de capacidade, inscrição repetida e ingresso já usado vão pro `Event` e pro `Ticket`;
- **P06** — a capacidade fica só no `Event`;
- **P07/P08** — o id do ingresso passa a ser gerado num lugar só e o QR sai pelo `TicketingAdapter`;
- **P09** — a fachada perde as dependências que não usa;
- **P11** — `EventPublisher` guarda uma lista de assinantes;
- **P12** — preço resolvido por estratégia de verdade, uma classe por tipo;
- **P14** — `refund()` passa a ser chamado no cancelamento e o que sobrar sem uso é removido.

Quebrar a `EventHubService` em serviços menores (P04), separar regra de infraestrutura no `register()` (P10) e
arrumar as factories (P13) ficam pra uma próxima aula, junto com os testes automatizados.

---

## Evidências

- **Repositório:** https://github.com/arthurllucass/aula_design_software
- **Branch:** `aula-03` — https://github.com/arthurllucass/aula_design_software/tree/aula-03
- **Commits:** padrão `aula-03: <descrição>`, um por problema corrigido
- **Arquivos:** `entregas/aula-03/README.md` (este documento) e `src/main/java/br/edu/eventhub/**`

### Compilação

```bash
$ javac -d out $(find src/main/java -name "*.java")
$ echo $?
0
```

### Saída do `Main` antes das correções

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

O que dá pra ver aqui: `INSCRITOS=3` num evento de capacidade 1 e o mesmo `A1` inscrito duas vezes (P05/P06),
o ingresso `T-E1-A1` emitido duas vezes com o mesmo id (P07), dois check-ins no mesmo ingresso (P05), nenhuma
linha `ATTENDEE` (P11) e o evento cancelado sem nenhum estorno (P14).

### Saída do `Main` depois das correções

```bash
$ java -cp out br.edu.eventhub.Main
EMAIL a1@exemplo.com => Ingresso T-E1-A1 QR=QR::T-E1-A1 pagamento=true
ATTENDEE E1 REGISTRATION_CREATED
ORGANIZER E1 REGISTRATION_CREATED
ATTENDEE E1 CHECKIN
ORGANIZER E1 CHECKIN
ATTENDEE E1 EVENT_CANCELLED
ORGANIZER E1 EVENT_CANCELLED
INSCRITOS=1
STATUS=CANCELLED
```

O que mudou na prática: `INSCRITOS=1` respeitando a capacidade e a segunda tentativa do `A1` recusada (P05/P06),
um único `EMAIL` em vez de três, um check-in só apesar das duas chamadas (P05), o `ATTENDEE` aparecendo junto do
`ORGANIZER` em todos os eventos (P11) e o cancelamento passando pelo estorno (P14). O QR agora sai pelo
`TicketingAdapter` e o id do ingresso vem do `Ticket.buildId` (P07/P08), então o `Main` não escreve mais
`"T-E1-A1"` na mão.

Continua aparecendo `pagamento=true` no e-mail sem ninguém decidir nada com isso: é o P10, que ficou pra
próxima aula.
