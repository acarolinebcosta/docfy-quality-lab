# Registro de riscos de Backend/REST API

Escala: probabilidade e impacto de 1 a 5. A exposição é o produto dos dois valores.

| ID | Risco | ISO/IEC 25010 | Prob. | Impacto | Exposição | Controle automatizado inicial |
| --- | --- | --- | ---: | ---: | ---: | --- |
| API-SEC-001 | Recurso protegido aceita requisição sem identidade válida | Segurança | 3 | 5 | 15 | Ausência e invalidez de Bearer token |
| API-SEC-002 | Papel acessa operação incompatível com sua permissão | Segurança | 3 | 5 | 15 | Matriz RBAC por recurso, fase 2 |
| API-FUN-001 | Transição inválida corrompe o ciclo do documento | Adequação funcional | 3 | 5 | 15 | Matriz de estados do workflow, fase 4 |
| API-DAT-001 | Resposta ou persistência perde dados do documento | Confiabilidade | 3 | 4 | 12 | CRUD e leitura posterior, fase 3 |
| API-CON-001 | Mudança incompatível quebra consumidores da API | Compatibilidade | 3 | 4 | 12 | OpenAPI e JSON Schemas versionados |
| API-OBS-001 | Erro não pode ser rastreado entre cliente e servidor | Manutenibilidade | 3 | 4 | 12 | Geração e propagação de correlation ID |
| API-REL-001 | Serviço indisponível é confundido com falha funcional | Confiabilidade | 3 | 3 | 9 | Health check como precondição da execução |
| API-FIL-001 | Upload permite conteúdo, tamanho ou acesso indevido | Segurança | 2 | 5 | 10 | Matriz multipart e autorização, fase 5 |
| API-AUD-001 | Ação relevante não aparece ou pode ser alterada na auditoria | Segurança | 2 | 5 | 10 | Eventos e imutabilidade observável, fase 5 |

## Rastreabilidade da primeira fatia

| Risco | Suíte |
| --- | --- |
| API-SEC-001 | `AuthenticationBoundaryTest` |
| API-CON-001 | `PlatformContractTest`, `AuthenticationApiTest` |
| API-OBS-001 | `CorrelationIdTest`, assertions de erro |
| API-REL-001 | `PlatformContractTest` |

O registro será revisto quando surgirem novos endpoints, incidentes, defeitos relevantes ou
alterações nas regras de negócio.
