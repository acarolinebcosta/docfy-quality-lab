# Estratégia de testes de performance, carga e estresse

## Missão

Detectar regressões de latência e confiabilidade nas jornadas HTTP críticas do Docfy e observar o
comportamento do serviço quando a concorrência ultrapassa a carga de referência do laboratório.

## Escopo atual

- autenticação do usuário de teste como precondição;
- catálogo de categorias;
- listagem paginada de documentos visíveis;
- detalhe do documento;
- histórico público de auditoria;
- metadata de arquivos;
- propagação de `X-Correlation-ID` sob concorrência;
- latência, taxa de erro, taxa de checks, iterações e volume HTTP.

## Fora do escopo

- performance do navegador e Core Web Vitals;
- upload/download de arquivos grandes;
- criação massiva de dados;
- soak, spike e breakpoint testing;
- capacity planning de produção;
- diagnóstico interno por banco ou profiler;
- execução de stress em produção.

## Modelo de workload

A jornada representa a navegação de um manager do catálogo até o detalhe e o audit trail do
documento. A autenticação ocorre uma vez no `setup`, evitando que hashing de senha seja executado
artificialmente em todas as iterações.
Após uma pausa de um segundo para representar think time, o usuário virtual repete a jornada.

| Perfil | Modelo | Propósito | Frequência |
| --- | --- | --- | --- |
| Smoke | 1 VU, 5 iterações | Validar script, ambiente e caminho crítico | Pull request e `main` |
| Load | Rampa até 10 VUs e sustentação | Avaliar carga de referência | Manual, por release ou mudança relevante |
| Stress | Rampa progressiva até 35 VUs | Avaliar degradação acima da referência | Manual e supervisionada |

Os números são uma referência reproduzível para o ambiente efêmero atual. Eles não representam o
tráfego real de produção, ainda não conhecido.

## Portões de qualidade

| Perfil | Checks | Erros HTTP | p95 global/por operação | p99 global |
| --- | ---: | ---: | ---: | ---: |
| Smoke | > 99,99% | < 0,01% | < 1000 ms | < 1500 ms |
| Load | > 99% | < 1% | < 750 ms | < 1500 ms |
| Stress | > 95% | < 5% | < 1500 ms | < 3000 ms |

No stress, uma taxa de erro de 20% após os primeiros 20 segundos interrompe a execução. O limite
evita insistir em carga que já deixou o ambiente claramente instável.

Os thresholds são guardrails de engenharia. Uma mudança de limite exige evidência histórica,
justificativa e revisão; o limite não deve ser relaxado apenas para tornar o pipeline verde.

## Critérios de entrada

- ambiente explicitamente autorizado para receber carga;
- build do Docfy identificado;
- PostgreSQL e Docfy saudáveis;
- seed de desenvolvimento disponível;
- credencial de teste fornecida fora do código;
- ausência de outra carga relevante no ambiente de referência.

## Critérios de saída

- todos os checks funcionais do perfil atendidos;
- thresholds do perfil aprovados;
- SHAs do laboratório e do Docfy registrados;
- Step Summary e JSON gerados;
- secret scan aprovado antes da publicação do artifact;
- falhas classificadas como produto, ambiente, dados ou teste antes de alterar gates.

## Interpretação

Resultados só devem ser comparados quando jornada, perfil, dados e infraestrutura forem
equivalentes. Uma falha isolada de latência em runner compartilhado deve ser repetida antes de ser
classificada como regressão; erros funcionais e aumento consistente de percentis exigem análise
imediata.
