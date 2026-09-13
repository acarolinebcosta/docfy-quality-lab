# Relatório técnico de execução — Performance do Docfy

- **Data da execução:** 12 de setembro de 2026
- **Trilha:** performance, carga e estresse de protocolo HTTP
- **Sistema sob teste:** Docfy
- **Natureza da evidência:** execução local controlada

## 1. Objetivo

Este relatório registra as evidências reais produzidas pela primeira execução da trilha de
performance do Docfy Quality Lab. A suíte possui três objetivos complementares:

- detectar regressões funcionais e de latência com tráfego mínimo por meio do smoke;
- observar o comportamento da jornada crítica sob uma carga de referência reproduzível;
- avaliar estabilidade e degradação sob estresse progressivo, limitado e controlado.

Os resultados foram obtidos em um ambiente local isolado. Eles servem como baseline de engenharia
para comparação de execuções equivalentes, mas **não representam SLO, capacidade ou dimensionamento
de produção**. A infraestrutura local não reproduz uma topologia produtiva e CPU e memória do host
não foram coletadas para esta execução.

## 2. Ambiente de execução

| Item | Evidência |
| --- | --- |
| Sistema operacional | macOS 26.5.1, build 25F80 |
| Arquitetura | arm64 |
| Java do Docfy | 21.0.12.1 |
| Gerador de carga | k6 1.8.0, commit `23d89b9b7c`, Go 1.26.4, darwin/arm64 |
| Banco de dados | PostgreSQL 17.11 em container temporário isolado |
| Sistema sob teste | Backend Docfy real, sem mocks |
| Docfy SUT SHA | `7f804254d0e69f20314a218bf3a85f88b69f00cc` |
| Identificador enviado à evidência k6 | `local-working-tree` |
| Base do working tree executado | `9b04c9ea6d042a955013647a55478b6edc16702f` |
| Commit que versionou a implementação após a execução | `6debd158c3bf88a914704550a4fa1a755fe88687` |
| URL do SUT | `http://localhost:8080` |
| Perfil do Docfy | `dev` |
| Conexão PostgreSQL | loopback, porta local 5434 |
| Storage de arquivos | diretório local temporário |
| Identidade da jornada | usuário seed com papel `MANAGER` |
| Timeout HTTP k6 | 10 segundos |
| Think time | 1 segundo por iteração |
| CPU e memória do host | não coletado |

A execução ocorreu antes do commit da implementação. Por isso, a evidência identifica corretamente
o Quality Lab como `local-working-tree`, com base no commit `9b04c9e`. O commit `6debd15` é a revisão
que posteriormente versionou os scripts executados; ele não é apresentado como se já existisse no
momento da medição.

JWT e senha do seed foram gerados apenas para o ambiente temporário. Seus valores não foram
registrados neste relatório, nas tags de métricas ou no repositório.

## 3. Workloads executados

### 3.1 Smoke

Executor `shared-iterations`, com 1 VU, 5 iterações e duração máxima configurada de 30 segundos. O
objetivo é confirmar que script, ambiente, autenticação, checks e thresholds funcionam antes de
executar perfis mais pesados.

### 3.2 Reference load

Executor `ramping-vus`, com a seguinte progressão durante 90 segundos:

1. 0 até 5 VUs em 15 segundos;
2. 5 até 10 VUs em 30 segundos;
3. sustentação de 10 VUs por 30 segundos;
4. redução até 0 VU em 15 segundos.

Esse perfil é a carga de referência do laboratório. Não existe evidência de tráfego produtivo que
permita classificá-la como carga média real de usuários do Docfy.

### 3.3 Controlled stress

Executor `ramping-vus`, com a seguinte progressão durante 90 segundos:

1. 0 até 10 VUs em 15 segundos;
2. 10 até 20 VUs em 20 segundos;
3. 20 até 35 VUs em 20 segundos;
4. sustentação de 35 VUs por 20 segundos;
5. redução até 0 VU em 15 segundos.

O perfil é denominado `controlled stress` porque a pressão é progressiva, possui teto de 35 VUs,
duração curta e proteção de interrupção por erro excessivo. Ele não é um breakpoint test e não
continua elevando carga até a falha do produto.

### 3.4 Jornada medida

A autenticação acontece uma vez no `setup`. Cada iteração medida executa, sequencialmente:

```text
GET /api/v1/categories
        ↓
GET /api/v1/documents?page=0&size=20
        ↓
GET /api/v1/documents/{documentId}
        ↓
GET /api/v1/documents/{documentId}/audit
        ↓
GET /api/v1/documents/{documentId}/files
```

Cada resposta é submetida a checks mínimos de status, semântica e presença de correlation ID. A
jornada é read-only depois do `setup` e não consulta banco, filesystem ou classes internas do SUT.

## 4. Resultados

### 4.1 Smoke

| Métrica | Resultado observado |
| --- | ---: |
| VUs máximos | 1 |
| Iterações | 5 |
| Requisições HTTP | 27 |
| Checks aprovados | 100,00% |
| Erros HTTP | 0,00% |
| Latência global p95 | 15,62 ms |
| Latência global p99 | 22,99 ms |
| Threshold result | PASS |

Os 27 requests incluem os requests de preparação e os cinco requests da jornada em cada uma das
cinco iterações.

### 4.2 Reference load

| Métrica | Resultado observado |
| --- | ---: |
| VUs máximos | 10 |
| Iterações | 603 |
| Requisições HTTP | 3.017 |
| Checks aprovados | 100,00% |
| Erros HTTP | 0,00% |
| Latência global p95 | 11,33 ms — valor bruto disponível: 11,3336 ms |
| Latência global p99 | 14,02 ms — valor bruto disponível: 14,02302 ms |
| Threshold result | PASS |

### 4.3 Controlled stress

| Métrica | Resultado observado |
| --- | ---: |
| VUs máximos | 35 |
| Iterações | 1.822 |
| Requisições HTTP | 9.112 |
| Checks aprovados | 100,00% |
| Erros HTTP | 0,00% |
| Latência global p95 | 11,46 ms — valor bruto disponível: 11,4551 ms |
| Latência global p99 | 21,42 ms — valor bruto disponível: 21,41982 ms |
| Threshold result | PASS |

Os valores com duas casas decimais reproduzem a apresentação da execução. Quando o JSON local
preservou precisão adicional, o valor bruto também é registrado para evitar arredondamento
agressivo.

## 5. Interpretação técnica

Dentro do workload testado, não houve aumento significativo da latência global nem deterioração de
checks ou taxa de erro quando a concorrência subiu de 1 para 10 e depois para 35 VUs. Os três perfis
registraram 100% de checks aprovados e 0% de erro HTTP.

Os percentis menores em load do que no smoke não demonstram ganho de performance sob concorrência.
Aquecimento da JVM, caches, pequena quantidade de amostras no smoke e variação do host local podem
explicar essa diferença. São necessárias repetições equivalentes antes de classificar tendência ou
regressão.

O resultado de 35 VUs demonstra estabilidade somente até o patamar e durante o tempo exercitados.
Não houve crescimento de carga até falha, portanto:

- o ponto de saturação não foi encontrado;
- a capacidade máxima não foi determinada;
- não foi comprovado comportamento durante indisponibilidade;
- não foi comprovada recuperação depois de saturação.

A última etapa do stress apenas reduz a quantidade de VUs até zero. Não existe uma fase explícita de
carga reduzida após uma saturação observada para validar recuperação. Por isso, o resultado não deve
ser classificado como teste de recovery ou resiliência pós-falha.

## 6. Decisões tomadas durante a implementação

A primeira tentativa de smoke utilizou um usuário `COLLABORATOR`. O endpoint público de audit trail
rejeitou a consulta com `403`, conforme a política real do Docfy. Essa resposta provocou falha nos
checks e no gate; o `403` não foi aceito, ignorado nem reclassificado como sucesso.

Como a jornada inclui o audit trail e representa a tela gerencial de detalhe, a identidade padrão foi
alterada para um usuário seed com papel `MANAGER`. Com essa identidade, as cinco operações são
permitidas pelo contrato real e continuaram sendo validadas como respostas `200`.

Outras decisões relevantes:

- autenticar somente no `setup`, evitando carga artificial de hashing de senha em toda iteração;
- utilizar uma jornada read-only para não inflar banco e storage durante load/stress;
- compartilhar a mesma jornada entre os perfis e variar somente workload e gates;
- usar tags estáveis por operação para impedir que uma rota lenta seja ocultada pelo resultado
  agregado;
- bloquear destinos não loopback, salvo autorização explícita por configuração.

## 7. Thresholds e gates

### 7.1 Thresholds configurados

| Perfil | Checks da jornada | Erro HTTP da jornada | p95 global | p99 global | p95 por operação |
| --- | --- | --- | --- | --- | --- |
| Smoke | `rate > 0.9999` | `rate < 0.0001` | `< 1000 ms` | `< 1500 ms` | `< 1000 ms` |
| Reference load | `rate > 0.99` | `rate < 0.01` | `< 750 ms` | `< 1500 ms` | `< 750 ms` |
| Controlled stress | `rate > 0.95` | `rate < 0.05` | `< 1500 ms` | `< 3000 ms` | `< 1500 ms` |

O p95 por operação é aplicado individualmente a `categories_list`, `documents_list`,
`document_detail`, `document_audit` e `document_files`. O objetivo é evitar que a agregação global
mascare degradação localizada.

No stress existe ainda um threshold de proteção `http_req_failed rate < 0.2`, com
`abortOnFail: true` e avaliação depois de 20 segundos. Seu propósito é interromper pressão que já
tenha produzido uma taxa excessiva de erros, em vez de manter carga sem valor diagnóstico.

### 7.2 Camadas de resultado

As três camadas abaixo têm significados diferentes e não devem ser combinadas:

| Camada | Significado | Evidência disponível |
| --- | --- | --- |
| Threshold result | Avaliação das métricas contra os limites definidos no script | PASS nos três perfis |
| k6 process exit code | Resultado numérico do processo, incluindo exceções de script e thresholds | não coletado numericamente nas execuções locais originais; persistido pelo smoke de CI, cujo processo concluiu com sucesso |
| Workflow result | Resultado completo do job, incluindo ambiente, k6, summary, secret scan e upload | PASS no smoke do PR e no smoke pós-merge da `main`; load/stress ainda não executados no CI |

As execuções selecionadas concluíram sem exceção reportada e seus thresholds passaram. Entretanto,
como o exit code numérico original não foi persistido, ele permanece documentado como “não
coletado”. O workflow versionado passa a armazenar esse valor em artifact e apresenta separadamente
o resultado real do step.

## 8. Confiabilidade da evidência

O gerador de summary contém um gate adicional: a execução somente pode ser apresentada como
completa quando existem pelo menos uma iteração e checks efetivamente executados. Assim, ausência de
amostras não transforma thresholds sem dados em falso sucesso.

O workflow separa o `threshold result` calculado pelo k6 do resultado do processo. Exceções ou exit
code diferente de zero fazem o step falhar mesmo que métricas parciais tenham thresholds aprovados.
O exit code é armazenado para diagnóstico antes de o step devolver seu resultado.

Os três workloads passaram por `k6 inspect`. Essa inspeção é executada para smoke, load e stress em
todo pipeline da trilha, mesmo quando somente o smoke gera tráfego.

Frequência definida:

- smoke: automático em pull requests e mudanças da trilha na `main`;
- reference load: execução dedicada e manual;
- controlled stress: execução dedicada, manual e supervisionada.

Os JSONs, logs e summaries brutos locais permanecem em `tests/performance/artifacts`, caminho
ignorado pelo Git. Eles não são documentação permanente nem devem ser versionados. O valor de smoke
selecionado neste relatório foi preservado no output da execução; um smoke posterior, usado para
validar o formato final do summary, sobrescreveu o JSON local e também ficou verde. Essa limitação de
retenção local é registrada para não atribuir ao JSON atual um valor que ele já não contém.

O secret scan local não encontrou as credenciais efêmeras nos arquivos de evidência. O fluxo de
segurança das evidências também concluiu com sucesso no smoke do PR e no smoke pós-merge da `main`.
Nos dois casos, o job terminou verde e o artifact condicionado ao resultado do scan foi publicado.

### 8.1 Validação no GitHub Actions

| Execução | Evento | Revisão do Quality Lab | Resultado | Duração | Artifact |
| --- | --- | --- | --- | ---: | --- |
| [PR #11](https://github.com/acarolinebcosta/docfy-quality-lab/actions/runs/34785426538) | `pull_request` | `85bb7cc193a95949dd754c6c5baa0728baa66721` | PASS | 1m19s | `performance-smoke-evidence-34785426538` (10,6 KB) |
| [`main` após o merge](https://github.com/acarolinebcosta/docfy-quality-lab/actions/runs/34785733206) | `push` | `8ac1138cacd18c493577b679651a6bb81288a1cf` | PASS | 57s | `performance-smoke-evidence-34785733206` (10,7 KB) |

As duas execuções usaram o perfil smoke. A execução da `main` publicou o artifact com digest
`sha256:889604aee711f91ec39a446ee3d17f650a28ee5852ca21656512cf86d35f9670`. Os valores
numéricos de latência deste relatório continuam sendo os resultados locais identificados na seção
de ambiente; métricas do CI não foram substituídas ou inferidas a partir do status do workflow.

Reference load e controlled stress permanecem como execuções dedicadas a serem disparadas
manualmente no GitHub Actions. Os resultados locais desses dois perfis não devem ser apresentados
como resultados do runner do GitHub.

## 9. Riscos cobertos

| Risco | Workload/controle | Estado comprovado por esta execução |
| --- | --- | --- |
| `PERF-REL-001` | Load e controlled stress; checks e taxa de erro | Coberto até 10/35 VUs pelo período testado, sem erros observados |
| `PERF-EFF-001` | Smoke, load e controlled stress; p95/p99 global e p95 por operação | Coberto dentro dos thresholds e do ambiente local testado |
| `PERF-REL-002` | Controlled stress; rampa e abort por erro excessivo | Controle configurado; caminho de abort não exercitado porque não houve erro excessivo |
| `PERF-SEC-001` | Bloqueio de destino remoto, credenciais efêmeras e secret scan | Controles locais e caminho seguro de publicação validados nos smokes de CI; rejeição deliberada de artifact contaminado não exercitada |
| `PERF-OBS-001` | Summary, identificação dos SHAs e relatório permanente | Summary e artifact validados para smoke; execuções dedicadas de load/stress ainda pendentes no CI |

Nenhum risco de recuperação após saturação é marcado como coberto. A execução não incluiu
fault injection, saturação confirmada nem uma fase de recuperação sob carga reduzida.

## 10. Conclusão

Foi comprovado que, no ambiente local descrito e para a jornada HTTP selecionada:

- smoke, reference load e controlled stress executaram com dados e checks válidos;
- a aplicação permaneceu funcional até o patamar testado de 35 VUs;
- 100% dos checks foram aprovados e nenhuma resposta HTTP foi classificada como erro;
- p95 e p99 globais permaneceram abaixo dos thresholds configurados;
- todos os thresholds dos três perfis foram aprovados;
- o uso de `MANAGER` está alinhado à autorização real do audit trail.

Não foi comprovado:

- SLO, throughput ou capacidade de produção;
- ponto de saturação ou breakpoint;
- recuperação após saturação ou falha;
- comportamento em spike, soak ou carga prolongada;
- performance do navegador e Core Web Vitals;
- repetibilidade estatística em múltiplas execuções e máquinas equivalentes;
- repetição de reference load e controlled stress no runner do GitHub Actions.

Próximos passos recomendados:

1. executar load e controlled stress manualmente no GitHub Actions para obter evidências dedicadas;
2. repetir o mesmo perfil em condições equivalentes antes de estabelecer tendência;
3. coletar características de CPU e memória quando o objetivo passar a ser comparação de baseline;
4. criar um perfil separado de saturação e recuperação somente em ambiente autorizado e adequado;
5. avaliar spike e soak como incrementos independentes, sem tratá-los como cobertos agora.

## Tabela-resumo

| Cenário | VUs máx. | Iterações | Requests | Checks | Erros | p95 global | p99 global |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Smoke | 1 | 5 | 27 | 100,00% | 0,00% | 15,62 ms | 22,99 ms |
| Reference load | 10 | 603 | 3.017 | 100,00% | 0,00% | 11,33 ms | 14,02 ms |
| Controlled stress | 35 | 1.822 | 9.112 | 100,00% | 0,00% | 11,46 ms | 21,42 ms |
