# Registro de riscos de Backend/REST API

Escala: probabilidade e impacto de 1 a 5. A exposição é o produto dos dois valores.

| ID | Risco | ISO/IEC 25010 | Prob. | Impacto | Exposição | Controle automatizado |
| --- | --- | --- | ---: | ---: | ---: | --- |
| API-SEC-001 | Recurso protegido aceita requisição sem identidade válida | Segurança | 3 | 5 | 15 | Ausência e invalidez de Bearer token |
| API-SEC-002 | Papel acessa operação ou documento incompatível com sua permissão | Segurança | 3 | 5 | 15 | RBAC na consulta, edição, busca e paginação |
| API-FUN-001 | Transição inválida corrompe o ciclo do documento | Adequação funcional | 3 | 5 | 15 | Ciclo completo, permissões e matriz de transições inválidas |
| API-DAT-001 | Resposta ou persistência perde dados do documento | Confiabilidade | 3 | 4 | 12 | Criação, atualização parcial, leitura posterior e JSON Schema |
| API-FUN-002 | Busca, filtros ou paginação retornam um conjunto incorreto | Adequação funcional | 3 | 4 | 12 | Busca isolada, filtros combinados e paginação após autorização |
| API-CON-001 | Mudança incompatível quebra consumidores da API | Compatibilidade | 3 | 4 | 12 | OpenAPI e JSON Schemas versionados |
| API-OBS-001 | Erro não pode ser rastreado entre cliente e servidor | Manutenibilidade | 3 | 4 | 12 | Geração e propagação de correlation ID |
| API-REL-001 | Serviço indisponível é confundido com falha funcional | Confiabilidade | 3 | 3 | 9 | Health check como precondição da execução |
| API-FIL-001 | Upload permite conteúdo, tamanho ou acesso indevido | Segurança | 2 | 5 | 10 | Matriz multipart e autorização, planejada |
| API-AUD-001 | Ação relevante não aparece ou pode ser alterada na auditoria | Segurança | 2 | 5 | 10 | Eventos e imutabilidade observável, planejada |

## Rastreabilidade atual

| Risco | Suíte |
| --- | --- |
| API-SEC-001 | `AuthenticationBoundaryTest` |
| API-SEC-002 | `DocumentAuthorizationTest`, `DocumentUpdateApiTest`, `DocumentDiscoveryApiTest`, `DocumentWorkflowApiTest` |
| API-FUN-001 | `DocumentWorkflowApiTest` |
| API-DAT-001 | `DocumentsApiTest`, `DocumentUpdateApiTest` |
| API-FUN-002 | `DocumentDiscoveryApiTest` |
| API-CON-001 | `PlatformContractTest`, `AuthenticationApiTest`, suítes de documentos |
| API-OBS-001 | `CorrelationIdTest`, assertions de erro |
| API-REL-001 | `PlatformContractTest` |

## Riscos ainda sem cobertura automatizada completa

| Risco | Próxima evolução |
| --- | --- |
| API-FIL-001 | Cobrir upload multipart, limites e autorização |
| API-AUD-001 | Validar geração e integridade observável dos eventos de auditoria |

O registro deve ser revisto quando surgirem novos endpoints, incidentes, defeitos relevantes
ou alterações nas regras de negócio.
