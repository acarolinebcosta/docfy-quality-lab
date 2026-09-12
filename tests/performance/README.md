# Testes de performance, carga e estresse

Suíte black-box de protocolo do Docfy com Grafana k6. Os perfis reutilizam a mesma jornada crítica
e variam apenas a forma da carga e os thresholds.

## Pré-requisitos

- Docfy executando com PostgreSQL e perfil `dev`;
- k6 1.8.0;
- senha do seed de desenvolvimento;
- ambiente autorizado para receber a carga selecionada.

## Configuração

```bash
export DOCFY_API_BASE_URL=http://localhost:8080
export DOCFY_TEST_PASSWORD='a-mesma-senha-de-DOCFY_DEV_SEED_PASSWORD'
```

Configurações opcionais:

```bash
export DOCFY_PERFORMANCE_USER_EMAIL=manager@docfy.local
export K6_HTTP_TIMEOUT=10s
export K6_THINK_TIME_SECONDS=1
```

O destino padrão é local. Para reduzir o risco de carga acidental, qualquer URL não loopback é
bloqueada. `K6_ALLOW_REMOTE_TARGET=true` só deve ser usado depois da autorização explícita do
responsável pelo ambiente. Stress não deve ser executado em produção.

## Execução

Crie o diretório local de evidências, que é ignorado pelo Git:

```bash
mkdir -p artifacts
```

Execute a partir de `tests/performance`:

```bash
k6 run workloads/smoke.js
k6 run workloads/load.js
k6 run workloads/stress.js
```

Use smoke durante o desenvolvimento. Load e stress devem ser executados de forma controlada e
supervisionada.

## Jornada coberta

```text
autenticação no setup
        ↓
categorias + lista de documentos
        ↓
detalhe + audit trail + arquivos
        ↓
think time
```

A jornada é read-only após a preparação. Ela usa apenas a interface HTTP pública e verifica status,
forma semântica mínima e correlation ID enquanto coleta as métricas de desempenho.

## Perfis e gates

| Perfil | Carga | Checks | Erros HTTP | p95 | p99 |
| --- | --- | ---: | ---: | ---: | ---: |
| Smoke | 1 VU, 5 iterações | > 99,99% | < 0,01% | < 1000 ms | < 1500 ms |
| Load | Rampa até 10 VUs | > 99% | < 1% | < 750 ms | < 1500 ms |
| Stress | Rampa até 35 VUs | > 95% | < 5% | < 1500 ms | < 3000 ms |

O p95 também é aplicado individualmente a cada operação da jornada. Stress é interrompido se a
taxa de erro alcançar 20% depois do período inicial de aquecimento.

Esses números são guardrails do laboratório no ambiente efêmero de CI, não SLOs ou uma declaração
de capacidade de produção.

## CI e evidências

O workflow `Performance Quality Gates` executa smoke automaticamente em mudanças desta trilha.
Load e stress são selecionados manualmente por `workflow_dispatch`.

Cada execução preserva por tempo limitado:

- resultado agregado do k6 em JSON;
- relatório Markdown usado no Step Summary;
- inspeção do workload;
- log do k6;
- log do backend Docfy.

O artifact só é publicado depois do secret scan. Senha, JWT e `Authorization` não são incluídos em
tags ou mensagens. O diretório `artifacts` nunca deve ser versionado.

## Documentação relacionada

- [ADR-002](../../docs/adr/ADR-002-performance-test-architecture.md)
- [Arquitetura](../../docs/architecture/performance-test-architecture.md)
- [Estratégia](../../docs/strategy/performance-test-strategy.md)
- [Registro de riscos](../../docs/risks/performance-risk-register.md)
