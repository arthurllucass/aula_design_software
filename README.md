# EventHub — Projeto Semestral

O EventHub simula uma plataforma de gestão inteligente de eventos com inscrições, ingressos, pagamentos, locais, fornecedores, check-in e notificações.

O projeto inicial **compila e executa**, mas representa um legado propositalmente imperfeito. O aluno deverá analisá-lo e evoluí-lo ao longo do semestre conforme os conceitos apresentados em aula.

## Escopo inicial
- cadastro de eventos;
- locais e capacidade;
- inscrições;
- emissão de ingressos;
- pagamento;
- fornecedores;
- notificações;
- check-in;
- integrações externas.

## Execução
Requer Java 17.

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out br.edu.eventhub.Main
```

As atividades estão em `atividades/README_EventHub_AulaXX.md`.

> O aluno deve avaliar o comportamento real do código. A existência de nomes como Strategy, Factory, Adapter, Observer ou Facade não comprova aplicação correta de um padrão.
