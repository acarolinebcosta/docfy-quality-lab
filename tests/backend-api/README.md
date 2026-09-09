# Testes de Backend/REST API

Suíte black-box do Docfy em Java 21, JUnit Jupiter e REST Assured.

## Pré-requisitos

- Docfy executando com PostgreSQL;
- Java 21;
- acesso ao Maven Central na primeira execução;
- senha do seed de desenvolvimento para os testes de login válido.

## Configuração

```bash
export DOCFY_API_BASE_URL=http://localhost:8080
export DOCFY_TEST_PASSWORD='a-mesma-senha-de-DOCFY_DEV_SEED_PASSWORD'
export DOCFY_HTTP_TIMEOUT_MS=10000
```

`DOCFY_TEST_PASSWORD` é opcional para os cenários negativos e de contrato. Os testes que
dependem dos usuários seed são reportados como ignorados quando a variável não está presente.

## Execução

```bash
./mvnw verify
```

`verify` executa testes funcionais, regras ArchUnit e a verificação de formatação. Durante o
desenvolvimento, `./mvnw test` executa somente os testes.

Por tipo de teste:

```bash
./mvnw test -Dgroups=smoke
./mvnw test -Dgroups=contract
./mvnw test -Dgroups=security
./mvnw test -Dgroups=workflow
./mvnw test -Dgroups=observability
```

Também é possível substituir a URL por propriedade Java:

```bash
./mvnw test -Ddocfy.base.url=https://ambiente.exemplo
```

## Evidências

O JUnit produz resultados em `target/surefire-reports` e o adaptador Allure grava dados em
`target/allure-results`. O CI também publica um Step Summary com os totais reais da execução,
os commits testados e o estado dos quality gates. Os artifacts preservam esses resultados e o
log do backend por tempo limitado.

As credenciais de CI são geradas a cada execução e mascaradas antes de entrarem no ambiente.
Antes do upload, as evidências são verificadas e bloqueadas caso contenham qualquer uma dessas
credenciais. O logging do REST Assured mantém headers sensíveis e corpos de request/response fora
dos diagnósticos automáticos. Resultados gerados não são versionados no Git.

## Tags

| Tag | Uso |
| --- | --- |
| `smoke` | Disponibilidade e caminho crítico mínimo |
| `contract` | Status, headers, JSON Schema e OpenAPI |
| `authentication` | Emissão e rejeição de credenciais |
| `security` | Fronteiras de autenticação e autorização |
| `rbac` | Permissões por papel, ownership e estado do recurso |
| `workflow` | Transições válidas, inválidas e permissões do ciclo documental |
| `documents` | Comportamento e integridade do domínio de documentos |
| `observability` | Correlation ID e diagnóstico |
| `regression` | Cobertura funcional recorrente |
| `requires-seed` | Depende dos usuários do perfil `dev` |
| `architecture` | Regras de dependência e ausência de ciclos |
