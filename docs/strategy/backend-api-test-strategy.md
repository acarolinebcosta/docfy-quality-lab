# Estratégia de testes de Backend/REST API

## Missão

Fornecer feedback rápido e confiável sobre riscos funcionais, de segurança, compatibilidade e
observabilidade na fronteira HTTP do Docfy.

## Escopo inicial

- disponibilidade por `/actuator/health`;
- contrato OpenAPI publicado em `/v3/api-docs`;
- autenticação por `/api/v1/auth/login`;
- proteção de recursos sem token ou com token inválido;
- geração e propagação de `X-Correlation-ID`;
- estrutura padronizada de erros.

## Fora do escopo inicial

- comportamento interno de classes Spring;
- mapeamento JPA e migrações isoladamente;
- validação visual da interface;
- carga e performance;
- varredura automatizada de vulnerabilidades;
- acesso direto ao banco para confirmar respostas HTTP.

## Abordagem baseada em risco

Cada incremento parte do registro de riscos. Cenários de maior impacto entram na suíte smoke ou
na regressão crítica. A quantidade de testes por endpoint não é usada como medida isolada de
qualidade.

## Tipos de teste

| Tipo | Propósito | Frequência esperada |
| --- | --- | --- |
| Smoke | Confirmar que o ambiente aceita testes | Todo pipeline |
| Contract | Detectar quebra de schema e superfície HTTP | Todo pull request |
| Functional | Validar regras e resultados do domínio | Todo pull request |
| Security/RBAC | Impedir acesso indevido por recurso e papel | Todo pull request |
| Observability | Preservar diagnóstico e correlação | Todo pull request |
| Resilience | Avaliar entradas e falhas controladas | Regressão |

## Ambientes e configuração

A mesma suíte deve apontar para diferentes ambientes por `DOCFY_API_BASE_URL`. Credenciais são
injetadas externamente. A primeira execução reproduzível utiliza os usuários seed do perfil de
desenvolvimento do Docfy e uma senha efêmera definida durante a subida do ambiente.

## Critérios de entrada

- build alvo do Docfy identificado;
- backend e PostgreSQL disponíveis;
- `/actuator/health` respondendo `UP`;
- credenciais de teste conhecidas para cenários autenticados;
- ambiente autorizado para criação dos dados planejados.

## Critérios de saída do incremento

- build e coleta dos testes executam sem erro;
- cenários da fatia passam contra o Docfy local;
- nenhum segredo aparece no repositório ou nos relatórios;
- riscos cobertos apontam para evidências automatizadas;
- falhas apresentam contexto suficiente para triagem.

## Classificação

As tags JUnit representam intenção de execução: `smoke`, `contract`, `authentication`,
`security`, `observability`, `regression` e `requires-seed`.

## Gestão de falhas

Uma falha deve ser classificada antes de virar defeito:

1. confirmar disponibilidade e configuração do ambiente;
2. conferir dados e pré-condições;
3. identificar alteração intencional de contrato;
4. reproduzir com correlation ID e resposta anexados;
5. registrar defeito do produto apenas quando as causas anteriores forem descartadas.
