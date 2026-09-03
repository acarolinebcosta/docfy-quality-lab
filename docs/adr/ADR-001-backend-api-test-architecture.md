# ADR-001: arquitetura dos testes de Backend/REST API

- **Status:** aceita
- **Data:** 2026-09-02
- **Escopo:** `tests/backend-api`

## Contexto

O Docfy possui backend Java/Spring Boot e testes internos no repositório do produto. O
laboratório precisa validar o comportamento observável por um consumidor HTTP sem depender de
classes, contexto Spring ou acesso direto ao banco do sistema sob teste.

A solução também deve demonstrar desenho de framework, legibilidade, rastreabilidade e execução
reproduzível, sem criar abstrações que ainda não tenham uso real.

## Decisão

Adotar Java 21, JUnit Jupiter e REST Assured em uma arquitetura em camadas orientada aos domínios
do Docfy.

O padrão principal é **API Client/Service Object**. Os padrões auxiliares entram sob demanda:

- Request Specification para configuração HTTP reutilizável;
- Test Data Builder para payloads de domínio com muitas variações;
- Workflow/Facade para jornadas com múltiplas chamadas;
- Custom Assertions para invariantes de negócio e observabilidade;
- JUnit Extensions para responsabilidades transversais de ciclo de vida.

Os clientes executam chamadas e retornam a resposta sem decidir o resultado esperado. Status,
contrato e regra de negócio permanecem explícitos na camada de teste.

## Regras de dependência

```text
tests -> flows -> clients -> specifications -> REST Assured -> Docfy
   |        |         |
   +---- assertions, models, test data e contracts
```

- Testes podem conhecer clientes, flows, assertions e modelos.
- Flows podem compor clientes, mas não conter assertions específicas de um teste.
- Clientes podem conhecer specifications e modelos HTTP.
- Specifications não conhecem testes nem domínios.
- Nenhuma camada acessa classes internas ou o banco do Docfy.

## Alternativas consideradas

### Pytest e Requests

São adequados para uma suíte black-box, mas foram descartados como stack principal desta trilha
para concentrar a demonstração de Java e evitar duas implementações equivalentes da mesma suíte.

### MockMvc ou Spring Boot Test

Continuam apropriados para testes internos do produto. Não foram escolhidos aqui porque acoplam
o laboratório ao runtime e à implementação Spring, reduzindo a independência da validação externa.

### Postman/Newman

Pode ser útil para exploração ou compartilhamento manual de chamadas, mas não será uma segunda
suíte automatizada paralela. O código Java será a fonte de verdade da regressão de API.

### Screenplay

Não foi adotado nesta fase. A quantidade inicial de atores, tarefas e interações não justifica
seu custo conceitual. A decisão pode ser revista se as jornadas crescerem significativamente.

## Consequências

### Positivas

- validação real da fronteira HTTP;
- alinhamento com Java 21 e ecossistema Maven;
- separação entre transporte, dados, fluxos e expectativas;
- suporte a JSON Schema, JWT, multipart e execução por tags;
- menor risco de duplicar testes internos do produto.

### Trade-offs

- o Docfy e suas dependências precisam estar executando;
- dados criados pelos testes exigem estratégia explícita de isolamento e limpeza;
- abstrações do framework também precisam de revisão e manutenção.
