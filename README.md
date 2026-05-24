# oficina-backend

[![CI](https://github.com/tiagomiele/POC-Fiap/actions/workflows/ci.yml/badge.svg)](https://github.com/tiagomiele/POC-Fiap/actions/workflows/ci.yml)

**MVP do back-end para gestão de uma oficina mecânica** — Entrega: 12/05/2026 — **Fase 1 do Tech Challenge (SOAT / FIAP)**

## 🎯 Objetivo

Desenvolver um sistema que permita uma oficina mecânica de médio porte controlar o **ciclo de vida completo de uma Ordem de Serviço (OS)**: desde o recebimento do veículo até o pagamento final, passando por diagnóstico, orçamento, aprovação do cliente, execução, finalização e entrega.

### Escopo MVP

O sistema oferece funcionalidades para:

- **Gestão de clientes e veículos** — cadastro e manutenção de dados
- **Catálogo de serviços e peças** — controle de preços e disponibilidade
- **Estoque** — rastreamento de peças com movimentação e saldo
- **Notas fiscais de fornecedor** — registro de compras e entrada de peças
- **Ordens de Serviço (OS)** — ciclo completo com múltiplos orçamentos
- **Conta corrente** — contas a pagar (fornecedores) e contas a receber (clientes)
- **Relatórios** — tempo médio de execução por serviço
- **Canal público** — consulta de status da OS pelo cliente

> 🚀 Implementado com boas práticas de **Domain-Driven Design (DDD)**, **testes ≥ 80%**, **análise estática (SonarQube)**, **análise de vulnerabilidades (Dependency-Track)** e **CI/CD automatizado**.

## Sumário

- [Arquitetura (monolito em camadas)](#arquitetura-monolito-em-camadas)
- [Stack](#stack)
- [Como rodar localmente](#como-rodar-localmente)
- [Endpoints principais](#endpoints-principais)
- [Consulta pública do status da OS](#consulta-pública-do-status-da-os)
- [Token JWT com validade configurável](#token-jwt-com-validade-configurável)
- [Cálculo de tempo médio por serviço](#cálculo-de-tempo-médio-por-serviço)
- [Testes e cobertura ≥ 80%](#testes-e-cobertura--80)
- [SonarQube (Análise estática de segurança: SAST—Static Application Security Testing)](#sonarqube-análise-estática-de-segurança-sast--static-application-security-testing)
- [Segurança e análise de vulnerabilidades (SCA)](#segurança-e-análise-de-vulnerabilidades---análise-de-segurança-das-dependências-do-projeto-sca--software-composition-analysis)
- [Estrutura de pacotes](#estrutura-de-pacotes)
- [Documentação](#-documentação)

---

## Arquitetura (monolito em camadas)

**Monolito MVP com **arquitetura em camadas flat** — adequada para o escopo do Tech Challenge Fase 1.

### Estrutura de pacotes

```
src/main/java/br/com/oficina
├── config/                # SecurityConfig, JwtAuthenticationFilter, OpenApiConfig, AdminBootstrap
├── controller/            # Camada HTTP (REST)
├── service/               # Regras de negócio (interfaces) + service/impl/ (implementações)
├── domain/                # Modelo rico (DDD tático)
│   ├── model/             #   Entidades + Value Objects
│   ├── enums/             #   Status e demais enums de negócio
│   └── repository/        #   Interfaces (contratos) dos repositórios
├── infrastructure/        # Implementações técnicas
│   ├── repository/        #   Entities JPA + implementações dos repositórios
│   └── security/          #   JwtTokenService
├── dto/                   # Objetos de transporte
│   ├── request/           #   Records de entrada (com @Schema + @NotNull/@Size)
│   └── response/          #   Records de saída (com @Schema + example)
├── mapper/                # Conversão DTO ↔ domínio
├── exception/             # BusinessException, GlobalExceptionHandler, ApiError
└── OficinaApplication.java
```

Regras de camadas são validadas automaticamente com **ArchUnit** (ver `src/test/java/br/com/oficina/architecture/ArchitectureTest.java`):

- `domain.model` **não pode** depender de `controller`, `service`, `infrastructure`, Spring ou JPA.
- `controller` só pode depender de `service`, `dto`, `mapper`, `exception`.
- `infrastructure` só pode depender de `domain` e do próprio `infrastructure`.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Build | Maven 3.9 (via wrapper `./mvnw`) |
| Framework | Spring Boot 3.3 |
| Persistência | Spring Data JPA + Hibernate 6 + Flyway |
| Banco | PostgreSQL 16 |
| Segurança | Spring Security 6 + JWT (`jjwt`) + BCrypt |
| Validação | Jakarta Validation |
| OpenAPI | springdoc-openapi (`/swagger-ui.html`) |
| Observabilidade | Spring Actuator + Logback JSON (perfil `prod`) |
| Testes | JUnit 5, AssertJ, Mockito, Testcontainers, RestAssured, ArchUnit |
| Cobertura | JaCoCo (gate ≥ 80% em `br.com.oficina.domain.model`) |
| Qualidade | SonarQube Community Edition (via `sonar-maven-plugin`) |
| Segurança | SBOM CycloneDX + OWASP Dependency-Track + Trivy (Dependency-Check em profile opcional) |

## Como rodar localmente

### Pré-requisitos

- **Docker Desktop** (Windows/Mac) ou **Docker Engine + Docker Compose v2** (Linux)
  - RAM disponível para o Docker: **≥ 6 GB** (Settings → Resources)
- **Git**
- **Java 21 (LTS)** — só necessário se for rodar testes/sonar/SBOM fora do container (o container Docker já traz tudo)
- (Opcional) `curl` no PATH — no Windows o `curl.exe` já vem com o sistema desde o Win10

### 1. Clonar o repositório

```powershell
# Windows (PowerShell) - escolha qualquer pasta para os seus projetos
cd <sua-pasta>          # ex: cd C:\projetos
git clone https://github.com/seu-usario/seu-projeto.git
cd seu-projeto
```

> ⚠️ **IMPORTANTE**: todos os comandos `docker compose` mais abaixo precisam ser executados **a partir da pasta raiz do projeto** (onde estão os arquivos `docker-compose.yml`, `docker-compose.dep-track.yml`, `docker-compose.sonarqube.yml` e `pom.xml`).

### 2. Subir banco + aplicação (Docker Compose)

```bash
# sobe Postgres + Adminer + aplicação Spring Boot, faz build da imagem
docker compose up --build -d
```

> Para subir somente o banco (e rodar a aplicação localmente via `./mvnw spring-boot:run`):
> ```bash
> docker compose up db adminer -d
> ```

### 3. Acessar

| Serviço | URL | Credenciais |
|---|---|---|
| API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| Adminer (UI do Postgres) | http://localhost:8081 | system: `PostgreSQL`, server: `db`, user: `oficina`, senha: `oficina`, db: `oficina` |
| Actuator health | http://localhost:8080/actuator/health | — |

### 4. Primeiro admin (bootstrap)

| Campo | Valor padrão |
|---|---|
| e-mail | `admin@oficina.local` |
| senha | `admin123` |


```powershell
# Exemplo de override antes do up:
$env:ADMIN_EMAIL = "seu-admin@minhaempresa.com"
$env:ADMIN_PASSWORD = "uma-senha-forte"
docker compose up --build -d
```

### Variáveis de ambiente relevantes

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/oficina` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `oficina` / `oficina` | Credenciais DB (locais) |
| `JWT_SECRET` | placeholder em `application.yml` (≥ 32 bytes) | **Trocar em produção.** Gere com `openssl rand -base64 48` |
| `JWT_EXPIRATION_MINUTES` | `60` | Validade padrão do token em minutos |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@oficina.local` / `admin123` | Seed do primeiro admin |
| `SERVER_PORT` | `8080` | Porta HTTP |

## Endpoints principais

Após subir a aplicação, o **Swagger UI** em http://localhost:8080/swagger-ui.html lista todos os endpoints com:

- Descrição detalhada (`@Operation`) por endpoint.
- Nomes dos parâmetros descritivos (ex: `numeroOs` com formato `OS-MMAAAA-NNNNNN` e exemplo `OS-042026-000001`).
- Schemas de cada DTO com exemplo preenchido (`@Schema(example=...)`).
- Códigos de resposta documentados (200, 201, 204, 400, 401, 403, 404, 409, 422).

Papéis administrativos: `FUNCIONARIO_DA_OFICINA` (cadastros + abertura/entrega) e `TECNICO_DA_OFICINA` (diagnóstico, orçamento, execução, finalização).

| Área | Exemplo de rota | Auth |
|---|---|---|
| Autenticação | `POST /auth/login` | pública |
| Consulta pública | `GET /consulta/ordens-servico/{numeroOs}/status` | pública |
| Clientes | `POST/GET/PUT /api/v1/clientes/**` | admin |
| Veículos | `POST/GET/PUT /api/v1/veiculos/**` | admin |
| Catálogo | `/api/v1/servicos/**`, `/api/v1/pecas/**` | admin |
| Estoque | `POST /api/v1/estoque/ajustar` | admin |
| NF fornecedor | `/api/v1/notas-fiscais-fornecedor/**` | admin |
| Ordens de serviço | `POST /api/v1/ordens-servico/**` | admin |
| Financeiro | `GET /api/v1/contas-a-pagar`, `GET /api/v1/contas-a-receber` | admin |
| Relatórios | `GET /api/v1/relatorios/tempo-medio-por-os` | admin |

## Consulta pública do status da OS

Endpoint destinado ao cliente acompanhar o andamento do reparo pelo número da OS impresso no comprovante:

```
GET /consulta/ordens-servico/{numeroOs}/status
```

- **Sem autenticação.**
- **Input (path param)**: `numeroOs` no formato `OS-MMAAAA-NNNNNN` (ex.: `OS-042026-000001`).
- **200 OK**:
  ```json
  {
    "numeroOs": "OS-042026-000001",
    "status": "EM_EXECUCAO",
    "statusDescricao": "Em execução"
  }
  ```
- **404 Not Found** se a OS não existir (código `OS_NAO_ENCONTRADA`).

## Token JWT com validade configurável

O endpoint `POST /auth/login` aceita um campo **opcional** `validadeMinutos` no corpo da requisição:

```json
{
  "email": "admin@oficina.local",
  "senha": "admin123",
  "validadeMinutos": 120
}
```

- **Opcional**. Se ausente ou `null`, usa o default (`JWT_EXPIRATION_MINUTES`, padrão 60 min).
- **Faixa aceita**: 1 a 1440 minutos (24 horas). Valores fora da faixa retornam HTTP 400.
- O token devolvido vem com o `exp` (expiration claim) alinhado ao valor informado.

## Cálculo de tempo médio por execução de OS

Endpoint: `GET /api/v1/relatorios/tempo-medio-por-os`.

### Regra de preenchimento dos timestamps

Duas colunas na tabela `ordens_servico` rastreiam o tempo efetivo de execução:

- `inicio_execucao` (`TIMESTAMPTZ NULL`) — preenchido na **primeira transição** `AGUARDANDO_APROVACAO → EM_EXECUCAO` (quando o cliente aprova o 1º orçamento). Nunca é sobrescrito em aprovações subsequentes.
- `fim_execucao` (`TIMESTAMPTZ NULL`) — preenchido na **primeira transição** `EM_EXECUCAO → AGUARDANDO_PAGAMENTO` (conclusão do reparo). Nunca é sobrescrito.

### Fórmula

Considera apenas as OS com **ambos** `inicio_execucao` e `fim_execucao` preenchidos.

- **Duração de cada OS** (em horas decimais):
  ```sql
  duracaoHoras = EXTRACT(EPOCH FROM (fim_execucao - inicio_execucao)) / 3600.0
  ```
- **Média geral**: `AVG(duracaoHoras)` sobre o conjunto de OS encerradas.

Sem OS encerradas, a média retorna `0.0` e a lista vem vazia. OS antigas (anteriores à migration) ficam com `NULL` nos timestamps e não entram no cálculo.

Exemplo de payload:
```json
{
  "tempoMedioHoras": 2.75,
  "totalOrdens": 2,
  "ordens": [
    {
      "numeroOs": "OS-052026-000001",
      "inicioExecucao": "2026-05-11T09:00:00Z",
      "fimExecucao": "2026-05-11T11:30:00Z",
      "duracaoHoras": 2.5
    },
    {
      "numeroOs": "OS-052026-000002",
      "inicioExecucao": "2026-05-12T13:00:00Z",
      "fimExecucao": "2026-05-12T16:00:00Z",
      "duracaoHoras": 3.0
    }
  ]
}
```

## Testes e cobertura ≥ 80%

```bash
./mvnw clean verify
```

Relatório JaCoCo em `target/site/jacoco/index.html` (HTML) e `target/site/jacoco/jacoco.xml` (consumido pelo SonarQube).

### Domínios críticos cobertos ≥ 80%

O `jacoco-maven-plugin` está configurado com gate **obrigatório** de 80% de **line + branch coverage** sobre o pacote de domínio crítico `br.com.oficina.domain.model`. O build falha se a cobertura cair.

| Classe de domínio | Tipo | Testada em |
|---|---|---|
| `Dinheiro` | Value Object | `DinheiroTest` |
| `Documento` | Value Object (CPF/CNPJ) | `DocumentoTest` |
| `Placa` | Value Object | `PlacaTest` |
| `NumeroOS` | Value Object | `NumeroOSTest` |
| `Cliente` | Entidade | `ClienteTest` |
| `Veiculo` | Entidade | `VeiculoTest` |
| `Servico` | Entidade | `ServicoTest` |
| `Peca` | Entidade | `PecaTest` |
| `EstoquePeca` | Entidade | `EstoquePecaTest` |
| `MovimentacaoEstoque` | Entidade | `MovimentacaoEstoqueTest` |
| `LancamentoFinanceiro` | Entidade | `LancamentoFinanceiroTest` |
| `NotaFiscalFornecedor` | Agregado | `NotaFiscalFornecedorTest` |
| `ItemOrcamento` | Entidade | `ItemOrcamentoTest` |
| `Orcamento` | Agregado | `OrcamentoTest` |
| `OrdemServico` | Agregado raiz | `OrdemServicoTest` |

## SonarQube (Análise estática de segurança: SAST — Static Application Security Testing)

Análise de qualidade do código via **SonarQube Community Edition** localmente:

### 1. Subir o SonarQube

```powershell
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

Aguarde ~1 minuto e acesse <http://localhost:9000>.

Login inicial: **`admin`** / **`admin`** (será pedido para trocar a senha).

### 2. Criar projeto + token (UI)

1. **Projects → Create Project → Local**
2. **Project key**: `oficina-backend`
3. **Project Display Name**: `Oficina Backend`
4. Avança até a tela de geração de token → **Project Analysis Token**
5. Copia o token gerado (formato `sqp_...`)

> 🔒 **TOKEN PESSOAL**: o token gerado é **único pra cada execução do SonarQube de cada pessoa**. Quem rodar precisa gerar o seu próprio — não reutiliza tokens de outras instalações.

### 3. Rodar a análise

```powershell
# Windows (PowerShell)
$env:SONAR_HOST_URL = "http://localhost:9000"
$env:SONAR_TOKEN = "<COLE_AQUI_O_TOKEN_sqp_GERADO_NO_PASSO_2>"
./mvnw clean verify sonar:sonar
```

> ⚠️ **PowerShell**: **NUNCA** use `\` para quebrar linhas (isso é Bash). Use os env vars acima ou backtick `` ` `` com aspas em cada `-D...`.

### 4. Ver o resultado

Abra <http://localhost:9000/dashboard?id=oficina-backend>. O relatório traz:

- **Quality Gate**: PASSED / FAILED contra o perfil _Sonar way_.
- **Coverage**: lê `target/site/jacoco/jacoco.xml` (configurado via `sonar.coverage.jacoco.xmlReportPaths`).
- **Bugs / Vulnerabilities / Code Smells / Security Hotspots**.
- **Duplications**.

### Propriedades do projeto (já configuradas no `pom.xml`)

| Propriedade | Valor |
|---|---|
| `sonar.projectKey` | `oficina-backend` |
| `sonar.projectName` | `Oficina Backend` |
| `sonar.java.source` | `21` |
| `sonar.coverage.jacoco.xmlReportPaths` | `target/site/jacoco/jacoco.xml` |
| `sonar.exclusions` | `**/dto/**, **/mapper/**, **/config/**, **/infrastructure/**, OficinaApplication.java` |
| `sonar.coverage.exclusions` | acima + `**/controller/**` |

## Segurança e análise de vulnerabilidades - Análise de segurança das dependências do projeto: SCA — Software Composition Analysis

> Bloco resumido — o detalhe completo está em `docs/dependency-track-readme.md` (uso recorrente) e `docs/dependency-track-do-primeira-utilizacao.md` (bootstrap, ~1h17 de mirror NVD na primeira vez).

📖 **Veja**:
- [`docs/dependency-track-readme.md`](docs/04-security/dependency-track-readme.md) — fluxo recorrente do Dependency-Track (após o bootstrap inicial)
- [`docs/dependency-track-do-primeira-utilizacao.md`](docs/04-security/dependency-track-do-primeira-utilizacao.md) — bootstrap completo do Dependency-Track em PC novo (inclui mirror NVD de ~1h17min)

## Estrutura de pacotes

```
src/
├── main/
│   ├── java/br/com/oficina/
│   │   ├── OficinaApplication.java
│   │   ├── config/         (SecurityConfig, JwtAuthenticationFilter, OpenApiConfig, AdminBootstrap)
│   │   ├── controller/     (AuthController, Cliente/Veiculo/Servico/Peca/Estoque/NF/OS/Financeiro/Relatorio/Consulta)
│   │   ├── service/        (interfaces) + service/impl/ (Implementações transacionais)
│   │   ├── domain/
│   │   │   ├── model/      (Cliente, Veiculo, Servico, Peca, OrdemServico, Orcamento, …)
│   │   │   ├── enums/      (StatusOrdemServico, StatusOrcamentoItem, TipoItem, TipoLancamento, …)
│   │   │   └── repository/ (Interfaces puras, sem Spring/JPA)
│   │   ├── infrastructure/
│   │   │   ├── repository/ (Entities JPA + implementações dos repositórios)
│   │   │   └── security/   (JwtTokenService)
│   │   ├── dto/
│   │   │   ├── request/    (LoginRequest, AbrirOsRequest, …)
│   │   │   └── response/   (OrdemServicoResponse, OrdemServicoStatusResponse, …)
│   │   ├── mapper/
│   │   └── exception/      (BusinessException, GlobalExceptionHandler, ApiError)
│   └── resources/
│       ├── application.yml
│       ├── db/migration/V1__schema_inicial.sql
│       └── logback-spring.xml
└── test/
    └── java/br/com/oficina/
        ├── architecture/ArchitectureTest.java
        ├── domain/model/…
        └── exception/…
```

## 📚 Documentação

Toda a documentação do projeto está organizada em [`/docs`](./docs/README-DOCS.md), incluindo análises de segurança, evidências de validação e documentação de negócio.

| Quero…                                          | Vá para                                                                                                                              |
|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| 🎯 **Apresentação** (Tech Challenge FIAP)       | [Roteiro da apresentação](./README-apresentacao-tech-challenge-fase1.md)                                                             |
| 📖 **Documentações do projeto**                 | [Guia de documentações](./docs/README-DOCS.md) — DDD (Storytelling, Event Storming, Linguagem ubíqua), Decisões arquiteturais (ADR), API, Segurança e Evidências de validação |

---</content>
<parameter name="filePath">C:\aula01\oficina-backend-fiap\README_alterado.md
