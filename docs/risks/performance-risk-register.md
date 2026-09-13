# Registro de riscos de performance

Escala: probabilidade e impacto de 1 a 5. A exposição é o produto dos dois valores.

| ID | Risco | ISO/IEC 25010 | Prob. | Impacto | Exposição | Controle automatizado |
| --- | --- | --- | ---: | ---: | ---: | --- |
| PERF-REL-001 | A jornada crítica produz erros sob usuários concorrentes | Confiabilidade | 3 | 5 | 15 | Taxa de erro HTTP e checks nos perfis load/stress |
| PERF-EFF-001 | A latência degrada sob a carga de referência | Eficiência de desempenho | 3 | 4 | 12 | Thresholds p95/p99 globais e p95 por operação |
| PERF-REL-002 | Sobrecarga mantém pressão após falha generalizada | Confiabilidade | 2 | 5 | 10 | Rampa controlada e abort antecipado no stress |
| PERF-SEC-001 | Execução atinge ambiente não autorizado ou publica credencial | Segurança | 2 | 5 | 10 | Bloqueio de target remoto, masking e secret scan |
| PERF-OBS-001 | Resultado não identifica build, workload ou gate violado | Manutenibilidade | 3 | 3 | 9 | Step Summary, SHAs e artifacts Markdown/JSON |

## Rastreabilidade

| Risco | Perfil/evidência |
| --- | --- |
| PERF-REL-001 | `load.js`, `stress.js`, métricas `checks` e `http_req_failed` |
| PERF-EFF-001 | `smoke.js`, `load.js`, `stress.js`, métrica `http_req_duration` |
| PERF-REL-002 | `stress.js`, threshold de interrupção antecipada |
| PERF-SEC-001 | `runtime-config.js`, workflow `Performance Quality Gates` |
| PERF-OBS-001 | `evidence.js`, GitHub Step Summary e artifact de execução |

## Risco residual

O runner compartilhado é adequado para gates de regressão, mas não representa capacidade de
produção. Permanecem planejados para incrementos futuros: spike, soak, breakpoint, performance de
browser e baselines em infraestrutura equivalente à produção.

Este registro deve ser revisto após mudanças relevantes de jornada, infraestrutura, volume real,
incidentes ou estabelecimento de SLOs formais.
