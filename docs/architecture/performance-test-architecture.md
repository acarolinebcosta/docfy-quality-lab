# Arquitetura dos testes de performance

## Objetivo

Exercitar jornadas HTTP críticas do Docfy sob perfis controlados de concorrência, mantendo
workload, transporte, validações e evidências com responsabilidades explícitas.

## Visão de componentes

```text
┌─────────────────────────────────────────────────────────────┐
│ Workloads                                                   │
│ smoke | load | stress                                       │
└───────────────────────────┬─────────────────────────────────┘
                            │ selecionam
┌───────────────────────────▼─────────────────────────────────┐
│ Profiles                                                    │
│ executores | estágios | thresholds | proteção de abort      │
└───────────────────────────┬─────────────────────────────────┘
                            │ executam
┌───────────────────────────▼─────────────────────────────────┐
│ Critical journey                                            │
│ setup auth → catálogo → detalhe → audit → files             │
└───────────────────────────┬─────────────────────────────────┘
                            │ usa
┌───────────────────────────▼─────────────────────────────────┐
│ Docfy HTTP client                                            │
│ rotas | headers | timeout | tags de baixa cardinalidade     │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP real
┌───────────────────────────▼─────────────────────────────────┐
│ Docfy + PostgreSQL                                           │
└─────────────────────────────────────────────────────────────┘

Workload/resultados ──► Evidence builder ──► Markdown + JSON
```

## Responsabilidades

### Workloads

São pontos de entrada pequenos. Escolhem o perfil e conectam os hooks `setup`, execução e
`handleSummary`. Não repetem a jornada.

### Profiles

Modelam a forma da carga e os critérios de aprovação. Thresholds globais protegem taxa de checks,
erros HTTP e latência; thresholds por operação evitam que uma rota lenta seja escondida pela
agregação da jornada.

### Critical journey

Expõe a intenção de uso do produto e mantém checks semânticos mínimos durante a carga. O `setup`
autentica e seleciona um documento visível. Cada iteração percorre as consultas executadas pelo
catálogo e pelo detalhe do documento.

### HTTP client

Centraliza verbo, rota, autenticação, timeout e tags estáveis. Retorna responses para a jornada
decidir os checks. Nenhum segredo é usado como tag ou mensagem de diagnóstico.

### Evidence builder

Transforma a agregação nativa do k6 em JSON e Markdown. O Step Summary registra perfil, commits,
ambiente, volume, latência, taxa de erro e resultado de cada threshold.

## Fluxo no CI

```text
credenciais efêmeras e masking
              ↓
PostgreSQL real + Docfy real
              ↓
inspeção do workload
              ↓
k6 e thresholds
              ↓
Step Summary + JSON + log
              ↓
secret scan
       ┌──────┴──────┐
     seguro       contaminado
       ↓                ↓
artifact             bloqueio
```

## Isolamento e segurança

- a jornada é read-only depois do `setup`;
- usuários virtuais não dependem da ordem das iterações;
- não existe acesso direto à persistência ou ao storage;
- o destino remoto é bloqueado por padrão;
- resultados gerados são ignorados pelo Git;
- carga e estresse não são iniciados automaticamente em pull requests.
