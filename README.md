# Docfy Quality Lab

Laboratório independente de engenharia de qualidade para o
[Docfy](https://github.com/acarolinebcosta/Docfy).

O objetivo deste repositório é exercitar o produto como um consumidor externo, produzir
evidências reproduzíveis de qualidade e evoluir a cobertura automatizada a partir dos riscos
do sistema.

## Estado atual

As trilhas de Backend/REST API e performance possuem fundação arquitetural, estratégia orientada
a risco, portões automatizados e evidências reproduzíveis. A regressão de API cobre a fronteira
HTTP crítica. A suíte k6 avalia a jornada de catálogo e detalhe sob smoke, carga de referência e
stress controlado.

| Trilha | Estado |
| --- | --- |
| Backend/REST API | Cobertura crítica orientada a risco concluída |
| E2E Web | Próxima fase |
| Performance, carga e stress | Implementada com k6 e thresholds como código |
| Acessibilidade | Planejada |
| Usabilidade | Planejada |

## Cobertura atual de Backend/API

A suíte automatizada cobre atualmente:

- health check e disponibilidade da aplicação;
- contrato OpenAPI;
- autenticação com credenciais válidas e inválidas;
- proteção de recursos sem autenticação ou com token inválido;
- RBAC por papel e recurso;
- criação, atualização parcial, listagem e consulta de documentos;
- validações de payload;
- contratos JSON Schema;
- busca, filtros combinados e paginação;
- visibilidade, ownership e integridade de documentos;
- ciclo completo do workflow e rejeição de transições inválidas;
- upload multipart, download, integridade binária e segurança de arquivos;
- auditoria de submissão, aprovação, rejeição e arquivamento;
- ordem cronológica, autoria, RBAC, correlation ID e append-only observável da auditoria;
- geração e propagação de correlation ID;
- regras arquiteturais com ArchUnit.

A qualidade da própria suíte também é protegida por Spotless, Maven Enforcer e execução
automatizada em CI.

## Cobertura atual de performance

A suíte k6 cobre uma jornada read-only de manager pela interface HTTP pública:

- autenticação segura como precondição;
- catálogo de categorias e documentos;
- detalhe, histórico de auditoria e metadata de arquivos;
- smoke automático em pull requests;
- carga de referência e stress progressivo por execução manual;
- thresholds globais e por operação para checks, erros HTTP e percentis de latência;
- interrupção de segurança quando o stress produz erro excessivo;
- Step Summary, resultado JSON, SHAs testados e secret scan antes do artifact.

Os limites são guardrails do ambiente efêmero do laboratório e não representam SLOs ou capacidade
de produção.

## Abordagem de qualidade

A estratégia segue uma abordagem orientada a risco.

Os testes são desenvolvidos a partir de riscos funcionais, de segurança, compatibilidade,
confiabilidade e observabilidade identificados para o produto. O registro de riscos mantém
a rastreabilidade entre os riscos identificados e as suítes automatizadas que os mitigam.

A automação de Backend/API é executada externamente ao Docfy, utilizando sua interface HTTP,
sem dependência das implementações internas do Spring.

## Princípios

- Testes externos não duplicam os testes unitários e de integração do produto.
- Riscos, contratos e regras de negócio orientam a cobertura.
- Testes devem ser independentes, determinísticos e seguros para execução paralela.
- API clients representam capacidades do produto e não concentram regras de assertion.
- JSON Schema valida estrutura; assertions específicas validam comportamento e semântica.
- Segredos e dados de ambiente nunca são versionados.
- Relatórios são evidências de execução, não substitutos para assertions relevantes.
- Falhas devem fornecer informações suficientes para investigação e diagnóstico.

## Stack atual

### Backend/API

- Java 21
- JUnit Jupiter
- REST Assured
- AssertJ
- JSON Schema
- ArchUnit
- Allure
- Maven
- Spotless
- Maven Enforcer
- GitHub Actions
- PostgreSQL efêmero em CI

### Performance

- Grafana k6 1.8
- JavaScript
- executores por cenário
- thresholds como código
- GitHub Actions
- PostgreSQL e Docfy reais no CI

## Quality Gates

A suíte possui verificações automatizadas para:

- execução dos testes;
- contratos de API;
- regras arquiteturais;
- sintaxe e expressões dos workflows com actionlint;
- formatação;
- compatibilidade da versão Java;
- resolução consistente de dependências;
- checks funcionais sob concorrência;
- taxa de erro e p95/p99 de latência;
- thresholds por operação;
- proteção de destino remoto, masking e secret scan das evidências.

A validação completa pode ser executada com:

```bash
./mvnw verify
```

no módulo `tests/backend-api`.

O smoke de performance pode ser executado com:

```bash
k6 run workloads/smoke.js
```

no módulo `tests/performance`. Carga e stress possuem acionamento controlado, conforme a estratégia
da trilha.

## Documentação

- [Arquitetura de testes de Backend/API](docs/architecture/backend-api-test-architecture.md)
- [ADR-001: arquitetura e stack](docs/adr/ADR-001-backend-api-test-architecture.md)
- [Estratégia de testes](docs/strategy/backend-api-test-strategy.md)
- [Registro de riscos](docs/risks/backend-api-risk-register.md)
- [Como executar a suíte](tests/backend-api/README.md)
- [Arquitetura de testes de performance](docs/architecture/performance-test-architecture.md)
- [ADR-002: arquitetura e stack de performance](docs/adr/ADR-002-performance-test-architecture.md)
- [Estratégia de performance](docs/strategy/performance-test-strategy.md)
- [Registro de riscos de performance](docs/risks/performance-risk-register.md)
- [Como executar performance, carga e stress](tests/performance/README.md)

## Estrutura

```text
docfy-quality-lab/
├── docs/
│   ├── adr/                 # Decisões arquiteturais
│   ├── architecture/        # Arquitetura das suítes
│   ├── risks/               # Registro e rastreabilidade de riscos
│   └── strategy/            # Estratégias de teste
│
└── tests/
    ├── backend-api/         # Testes black-box em Java e REST Assured
    └── performance/         # Performance, carga e stress HTTP com k6
```

Cada trilha possui dependências, estratégia e ciclo de execução próprios. Isso permite
escolher ferramentas de acordo com o tipo de risco e evita acoplamento desnecessário entre
as diferentes camadas de automação.

## Próximas evoluções

As próximas etapas planejadas são:

1. automação E2E Web com Playwright e TypeScript;
2. acessibilidade e usabilidade;
3. evolução futura de performance com baselines históricos, spike e soak quando houver ambiente
   adequado.

A evolução da suíte permanece orientada pelo risco e pela relevância das jornadas para o
produto.
