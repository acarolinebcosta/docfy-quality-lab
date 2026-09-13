# ADR-002: arquitetura dos testes de performance

- **Status:** aceita
- **Data:** 2026-09-12
- **Escopo:** `tests/performance`

## Contexto

O laboratório precisa avaliar a eficiência de desempenho e a confiabilidade do Docfy sob
concorrência sem duplicar a regressão funcional em REST Assured. A solução deve gerar carga com
baixo overhead, declarar critérios objetivos de aprovação, ser reproduzível no CI e impedir a
execução acidental de carga contra um ambiente não autorizado.

O ambiente efêmero do GitHub Actions não representa a infraestrutura de produção. Os resultados
obtidos nele são adequados para detectar regressões sob uma carga de referência constante, mas
não para declarar capacidade produtiva ou SLOs.

## Decisão

Adotar Grafana k6 para testes HTTP de protocolo, com uma jornada crítica reutilizada por três
perfis de carga:

- `smoke`: valida script, ambiente e jornada com tráfego mínimo;
- `load`: aplica a carga de referência do laboratório;
- `stress`: aumenta a concorrência acima da referência de forma progressiva e controlada.

Os perfis definem executores e thresholds como código. O smoke é portão automático de pull
request. Carga e estresse exigem acionamento manual porque consomem mais recursos e produzem
resultados úteis apenas quando executados de forma supervisionada.

A jornada autentica uma vez no `setup` e mede operações de leitura usadas pelo frontend:
catálogo de categorias, lista de documentos, detalhe, auditoria e arquivos. Ela não cria dados a
cada iteração e não acessa banco, filesystem ou classes internas do Docfy.

## Regras de segurança

- o destino padrão é loopback;
- destinos remotos exigem `K6_ALLOW_REMOTE_TARGET=true` e autorização prévia;
- token e senha não são adicionados a tags, mensagens ou evidências;
- o CI mascara credenciais efêmeras e bloqueia artifacts contaminados;
- o perfil de stress possui interrupção antecipada quando a taxa de erro ultrapassa o limite de
  segurança.

## Alternativas consideradas

### Apache JMeter

É maduro e extensível, mas seus planos XML e maior custo operacional adicionariam complexidade
desnecessária para a jornada HTTP atual.

### Gatling

É uma opção forte para workloads de protocolo, especialmente em equipes Scala/Java. Não foi
escolhido porque o k6 oferece uma DSL JavaScript menor, binário autocontido e integração direta
com thresholds e evidências no CI.

### REST Assured

Permanece a fonte de verdade da regressão funcional. Não foi reutilizado como gerador de carga
porque sua responsabilidade é validação de comportamento e contrato, não modelagem eficiente
de usuários virtuais.

## Consequências

### Positivas

- perfis e gates versionados;
- uma única jornada evita duplicação entre smoke, carga e estresse;
- execução local ou em CI sem serviço externo obrigatório;
- evidência legível e machine-readable;
- separação clara entre testes funcionais e de desempenho.

### Trade-offs

- medições no runner compartilhado possuem variabilidade;
- os limites precisam ser recalibrados quando o ambiente ou a jornada mudar;
- capacity planning real exige infraestrutura semelhante à produção e histórico de métricas.

## Referências

- [Grafana k6: automated performance testing](https://grafana.com/docs/k6/latest/testing-guides/automated-performance-testing/)
- [Grafana k6: scenarios](https://grafana.com/docs/k6/latest/using-k6/scenarios/)
- [Grafana k6: thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/)
- [Grafana k6: custom summaries](https://grafana.com/docs/k6/latest/results-output/end-of-test/custom-summary/)
