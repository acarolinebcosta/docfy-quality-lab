# Docfy Quality Lab

Laboratório independente de engenharia de qualidade para o
[Docfy](https://github.com/acarolinebcosta/Docfy). O objetivo é exercitar o produto como um
consumidor externo, produzir evidências reproduzíveis e evoluir a cobertura a partir dos riscos
do sistema.

## Estado atual

A primeira trilha implementada é a de Backend/REST API. Ela estabelece a arquitetura do
framework e cobre a superfície inicial de plataforma, contrato, autenticação, fronteira de
segurança e observabilidade.

| Trilha | Estado |
| --- | --- |
| Backend/REST API | Primeira fatia implementada |
| E2E web | Planejada |
| Performance, carga e estresse | Planejada |
| Acessibilidade | Planejada |
| Usabilidade | Planejada |

## Princípios

- Testes externos não duplicam os testes unitários e de integração do produto.
- Riscos, contratos e regras de negócio orientam a cobertura.
- Testes devem ser independentes, determinísticos e seguros para execução paralela.
- Segredos e dados de ambiente nunca são versionados.
- Relatórios são evidências de execução, não substitutos para assertions relevantes.

## Documentação

- [Arquitetura de testes de Backend/API](docs/architecture/backend-api-test-architecture.md)
- [ADR-001: arquitetura e stack](docs/adr/ADR-001-backend-api-test-architecture.md)
- [Estratégia de testes](docs/strategy/backend-api-test-strategy.md)
- [Registro de riscos](docs/risks/backend-api-risk-register.md)
- [Como executar a suíte](tests/backend-api/README.md)

## Estrutura

```text
docfy-quality-lab/
├── docs/                    # Estratégia, arquitetura, decisões e riscos
└── tests/
    └── backend-api/         # Testes black-box em Java e REST Assured
```

Cada trilha possui dependências, instruções e ciclo de execução próprios. Isso evita que uma
ferramenta seja usada fora do problema para o qual foi escolhida.
