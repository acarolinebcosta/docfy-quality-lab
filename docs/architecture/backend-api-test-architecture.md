# Arquitetura de testes de Backend/REST API

## Objetivo

Validar o Docfy pela sua interface HTTP pública, com independência da implementação Spring e
com diagnóstico suficiente para distinguir defeito de produto, contrato, ambiente ou dado de
teste.

## Visão de componentes

```text
┌─────────────────────────────────────────────────────────────┐
│ Test suites                                                 │
│ plataforma | contrato | auth | segurança | domínios        │
└───────────────────────────┬─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐
│ Flows          │ │ Assertions      │ │ Contracts       │
│ jornadas       │ │ semântica       │ │ JSON Schema     │
└───────┬────────┘ └─────────────────┘ └─────────────────┘
        │
┌───────▼────────────────────────────────────────────────┐
│ API clients por domínio                                   │
│ auth | categories | documents | workflow | files | audit │
└───────┬────────────────────────────────────────────────┘
        │
┌───────▼────────────────────────────────────────────────┐
│ Request specifications, config e filtros seguros          │
└───────┬────────────────────────────────────────────────┘
        │ HTTP real
┌───────▼────────────────────────────────────────────────┐
│ Docfy                                                       │
└────────────────────────────────────────────────────────┘
```

## Responsabilidades

### Test suites

Descrevem o comportamento esperado, classificam criticidade e mantêm Arrange/Act/Assert
visíveis. Um teste não monta detalhes repetitivos do protocolo HTTP.

### Flows

Compõem jornadas reutilizáveis, como criar e submeter um documento. Não escondem a expectativa
principal do teste e não são criados para chamadas isoladas.

### API clients

Representam capacidades do produto. Conhecem rota, verbo, parâmetros, headers e serialização,
mas retornam `Response` para permitir que o teste valide tanto sucesso quanto erro.

### Specifications

Centralizam base URL, timeout, content negotiation e proteção de headers sensíveis nos logs.
Cada método devolve uma specification nova para evitar estado global compartilhado.

### Models e test data

Records representam payloads estáveis. Builders só são adicionados quando um payload passa a
ter variações suficientes para justificar o padrão.

### Assertions e contracts

JSON Schema valida forma. Custom assertions validam significado, como a igualdade entre o
`X-Correlation-ID` da resposta e o valor presente no corpo de erro.

## Isolamento e paralelismo

- Nenhum teste depende da ordem de execução.
- Dados mutáveis recebem identificadores únicos.
- A limpeza usa IDs capturados durante o teste e nunca remove dados por padrão amplo.
- Tokens e clientes não mantêm estado mutável compartilhado.
- A paralelização só será habilitada depois de comprovado o isolamento dos domínios mutáveis.

## Observabilidade e segurança

- `Authorization` nunca deve aparecer em logs ou relatórios.
- Falhas HTTP devem preservar status, path e correlation ID para diagnóstico.
- Logs completos só são produzidos quando uma validação falha.
- Senhas são recebidas por variável de ambiente ou secret do pipeline.

## Evolução

1. Plataforma, contrato, autenticação e observabilidade.
2. RBAC por papel e recurso.
3. CRUD de documentos e integridade dos dados.
4. Máquina de estados do workflow.
5. Arquivos multipart e auditoria.
6. Resiliência e concorrência controlada.

## Regras executáveis de arquitetura

O ArchUnit verifica que clientes e specifications não dependem das camadas de teste ou de
assertions e que os pacotes do framework permanecem livres de ciclos. Essas regras rodam junto
com a suíte, reduzindo a diferença entre a arquitetura documentada e a arquitetura real.

O Spotless verifica formatação, imports não utilizados e imports wildcard durante `mvn verify`.
O Maven Enforcer bloqueia Java ou Maven incompatíveis e conflitos de limite superior nas
dependências.
