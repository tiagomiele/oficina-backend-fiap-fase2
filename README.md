# Oficina Backend — Sistema de Gestão de Oficina Mecânica

[![CI](https://github.com/tiagomiele/oficina-backend-fiap-fase2/actions/workflows/ci.yml/badge.svg)](https://github.com/tiagomiele/oficina-backend-fiap-fase2/actions/workflows/ci.yml)

**Back-end para gestão completa de uma oficina mecânica de médio porte** — Tech Challenge SOAT / FIAP — Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Clean Architecture · DDD

---

## Sumário

- [Sobre o Projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Arquitetura — Clean Architecture](#arquitetura--clean-architecture)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Passo a Passo para Execução](#passo-a-passo-para-execução)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Endpoints Principais](#endpoints-principais)
- [Fluxo da Ordem de Serviço](#fluxo-da-ordem-de-serviço)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes e Cobertura](#testes-e-cobertura)
- [Validação de Arquitetura (ArchUnit)](#validação-de-arquitetura-archunit)

---

## Sobre o Projeto

Sistema que permite uma oficina mecânica controlar o **ciclo de vida completo de uma Ordem de Serviço (OS)**: desde o recebimento do veículo até o pagamento e entrega, passando por diagnóstico, orçamento, aprovação do cliente e execução do reparo.

O projeto aplica **Domain-Driven Design (DDD)** com padrões táticos (agregados, value objects, entidades ricas) e está estruturado em **Clean Architecture** com 3 camadas isoladas.

### O que o sistema oferece

- Cadastro e gestão de **clientes** e **veículos**
- Catálogo de **serviços** e **peças** com controle de preços
- Controle de **estoque** de peças com rastreabilidade de movimentações
- Registro de **notas fiscais de fornecedor** com entrada automática no estoque
- Ciclo completo de **Ordens de Serviço** com múltiplos orçamentos
- **Conta corrente** da oficina (contas a pagar e contas a receber)
- **Relatórios** de tempo médio de execução por OS
- **Consulta pública** de status da OS pelo cliente (sem autenticação)
- **Notificação fictícia** ao cliente nas transições de status (log via SLF4J)

---

## Funcionalidades

### Gestão de Clientes e Veículos
- Cadastro com validação de **CPF/CNPJ** (dígitos verificadores)
- Suporte a **placa antiga** (ABC1234) e **Mercosul** (ABC1D23)
- Vínculo veículo ↔ cliente com PK composta (placa + idCliente)

### Ordens de Serviço
- **Abertura unificada** — uma única chamada POST cria a OS já com os itens (serviços e/ou peças) no corpo da requisição
- **Listagem de OS ativas** com ordenação por prioridade: `EM_EXECUCAO(1) > AGUARDANDO_APROVACAO(2) > EM_DIAGNOSTICO(3) > RECEBIDA(4)` — exclui automaticamente ENTREGUE e CANCELADA
- Suporte a **múltiplos orçamentos** (rejeitar e refazer)
- Transições de status controladas com **máquina de estados**
- **Notificação fictícia** ao cliente em cada transição de status (log com marcador `[NOTIFICAÇÃO FICTÍCIA]`)

### Estoque e Suprimentos
- Saldo de estoque **nunca negativo** (invariante no domínio)
- 4 tipos de movimentação: `ENTRADA_NF`, `ESTORNO_NF`, `CONSUMO_ORCAMENTO`, `DEVOLUCAO_ORCAMENTO`
- Peças consumidas ao serem adicionadas ao orçamento; devolvidas se o orçamento for rejeitado
- Registro de NF de fornecedor com crédito automático no estoque e geração de conta a pagar

### Financeiro
- **Contas a pagar**: geradas na emissão de NF de fornecedor
- **Contas a receber**: geradas na confirmação de pagamento da OS
- Suporte a estorno de lançamentos

### Relatórios
- Tempo médio de execução por OS (baseado nos timestamps `inicio_execucao` e `fim_execucao`)

### Segurança
- Autenticação via **JWT** (HMAC-SHA256) com validade configurável (1–1440 min)
- Senhas hash com **BCrypt(12)**
- 2 perfis de acesso: `FUNCIONARIO_DA_OFICINA` (admin) e `TECNICO_DA_OFICINA`
- Hierarquia: funcionário herda todas as permissões do técnico
- Consulta pública do status da OS **sem autenticação**

---

## Arquitetura — Clean Architecture

O projeto segue os princípios da **Clean Architecture (Uncle Bob)**, organizado em 3 camadas com dependências sempre apontando para dentro:

```
┌──────────────────────────────────────────────────┐
│                INFRASTRUCTURE                     │
│  Controllers · JPA · Security · Notification      │
│  (frameworks, drivers, adaptadores externos)      │
│                                                   │
│  ┌──────────────────────────────────────────┐     │
│  │              USECASE                      │     │
│  │  Services · Gateways (interfaces)         │     │
│  │  (casos de uso, regras de aplicação)      │     │
│  │                                           │     │
│  │  ┌──────────────────────────────────┐     │     │
│  │  │            DOMAIN                 │     │     │
│  │  │  Entities · Value Objects · Enums │     │     │
│  │  │  (regras de negócio puras)        │     │     │
│  │  └──────────────────────────────────┘     │     │
│  └──────────────────────────────────────────┘     │
└──────────────────────────────────────────────────┘
```

**Regras de dependência:**
- `domain` → **puro**: sem Spring, sem JPA, sem Servlet — apenas Java puro
- `usecase` → depende de `domain`; define interfaces `*Gateway` e `*Repository` que a infraestrutura implementa
- `infrastructure` → implementa gateways e repositórios; depende de `usecase` e `domain`

Essas regras são **validadas automaticamente** por 4 testes ArchUnit a cada build.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) |
| Build | Maven 3.9 (via wrapper `./mvnw`) |
| Framework | Spring Boot 3.3.4 |
| Persistência | Spring Data JPA + Hibernate 6 + Flyway |
| Banco de Dados | PostgreSQL 16 |
| Segurança | Spring Security 6 + JWT (jjwt) + BCrypt |
| Validação | Jakarta Validation |
| Documentação API | springdoc-openapi (Swagger UI) |
| Observabilidade | Spring Actuator + Logback |
| Containerização | Docker (multi-stage) + Docker Compose |
| Testes | JUnit 5, AssertJ, Mockito, ArchUnit, Testcontainers, RestAssured |
| Cobertura | JaCoCo (gate ≥ 80% em `domain.model`) |
| Segurança (CI) | SBOM CycloneDX + Trivy |

---

## Pré-requisitos

- **Docker Desktop** (Windows/Mac) ou **Docker Engine + Docker Compose v2** (Linux)
- **Git**
- (Opcional) **Java 21** — apenas se quiser rodar testes localmente fora do container

---

## Passo a Passo para Execução

### 1. Clonar o repositório

```bash
git clone https://github.com/tiagomiele/oficina-backend-fiap-fase2.git
cd oficina-backend-fiap-fase2
```

### 2. Subir a aplicação com Docker Compose

```bash
docker compose up --build -d
```

Esse comando sobe 3 serviços:
- **db** — PostgreSQL 16 (porta 5432)
- **app** — Aplicação Spring Boot (porta 8080)
- **adminer** — Interface web para o banco de dados (porta 8081)

> Para subir apenas o banco (e rodar a aplicação localmente):
> ```bash
> docker compose up db adminer -d
> ./mvnw spring-boot:run
> ```

### 3. Aguardar a aplicação ficar pronta

```bash
# Verificar se a aplicação está saudável
curl http://localhost:8080/actuator/health
```

Resposta esperada:
```json
{"status":"UP"}
```

### 4. Fazer login e obter o token JWT

A aplicação cria automaticamente um usuário administrador no primeiro boot:

| Campo | Valor padrão |
|---|---|
| E-mail | `admin@oficina.local` |
| Senha | `admin123` |

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@oficina.local","senha":"admin123"}'
```

Resposta:
```json
{
  "token": "eyJhbGciOiJIUzI1NiI...",
  "tipo": "Bearer",
  "expiraEm": "2026-05-24T19:24:00Z"
}
```

### 5. Usar o token nas requisições autenticadas

```bash
# Salvar o token em uma variável
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@oficina.local","senha":"admin123"}' | jq -r '.token')

# Exemplo: listar clientes
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/clientes
```

### 6. Acessar o Swagger UI

Abra no navegador: **http://localhost:8080/swagger-ui.html**

O Swagger UI lista todos os endpoints organizados em 4 grupos:
1. **01 — Autenticação** — login e cadastro de usuários
2. **02 — Administrativo** — clientes, veículos, serviços, peças, estoque, NF, OS, financeiro, relatórios
3. **03 — Técnico** — diagnóstico, orçamento, execução, finalização
4. **04 — Cliente** — aprovação, rejeição, pagamento, consulta pública

### 7. Acessar o Adminer (interface do banco)

Abra no navegador: **http://localhost:8081**

| Campo | Valor |
|---|---|
| Sistema | PostgreSQL |
| Servidor | db |
| Usuário | oficina |
| Senha | oficina |
| Base de dados | oficina |

### 8. Parar a aplicação

```bash
docker compose down
```

Para remover também os volumes (dados do banco):
```bash
docker compose down -v
```

---

## Variáveis de Ambiente

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/oficina` | URL JDBC do banco |
| `DB_USER` | `oficina` | Usuário do banco |
| `DB_PASSWORD` | `oficina` | Senha do banco |
| `JWT_SECRET` | placeholder (≥ 32 bytes) | Chave HMAC para tokens JWT. **Trocar em produção.** Gere com: `openssl rand -base64 48` |
| `ADMIN_EMAIL` | `admin@oficina.local` | E-mail do admin criado no bootstrap |
| `ADMIN_PASSWORD` | `admin123` | Senha do admin criado no bootstrap |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |
| `SPRING_PROFILES_ACTIVE` | (vazio) | Perfil Spring ativo |

---

## Documentação da API (Swagger)

Após subir a aplicação, a documentação interativa está disponível em:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

Cada endpoint possui:
- Descrição detalhada via `@Operation`
- Schemas de request/response com exemplos preenchidos
- Códigos de resposta documentados (200, 201, 204, 400, 401, 403, 404, 409, 422)

---

## Endpoints Principais

| Área | Rota | Método | Auth |
|---|---|---|---|
| **Login** | `/auth/login` | POST | Pública |
| **Cadastro de usuário** | `/usuarios` | POST | JWT (admin) |
| **Clientes** | `/api/v1/clientes` | POST/GET/PUT | JWT (admin) |
| **Veículos** | `/api/v1/veiculos` | POST/GET/PUT | JWT (admin) |
| **Serviços** | `/api/v1/servicos` | POST/GET/PUT | JWT (admin) |
| **Peças** | `/api/v1/pecas` | POST/GET/PUT | JWT (admin) |
| **Estoque** | `/api/v1/estoque` | POST/GET | JWT (admin) |
| **NF Fornecedor** | `/api/v1/notas-fiscais-fornecedor` | POST/GET | JWT (admin) |
| **Abrir OS (unificada)** | `/api/v1/ordens-servico` | POST | JWT (admin) |
| **Listar OS ativas** | `/api/v1/ordens-servico/ativas` | GET | JWT (admin) |
| **Contas a pagar** | `/api/v1/contas-a-pagar` | GET | JWT (admin) |
| **Contas a receber** | `/api/v1/contas-a-receber` | GET | JWT (admin) |
| **Relatórios** | `/api/v1/relatorios/tempo-medio-por-os` | GET | JWT (admin) |
| **Adicionar serviço à OS** | `/ordens-servico/{id}/servicos` | POST | JWT (técnico) |
| **Adicionar peça à OS** | `/ordens-servico/{id}/pecas` | POST | JWT (técnico) |
| **Enviar para aprovação** | `/ordens-servico/{id}/enviar-para-aprovacao` | PATCH | JWT (técnico) |
| **Concluir reparo** | `/ordens-servico/{id}/concluir-reparo` | PATCH | JWT (técnico) |
| **Entregar veículo** | `/ordens-servico/{id}/entregar` | PATCH | JWT (técnico) |
| **Aprovar orçamento** | `/ordens-servico/{id}/aprovar` | PATCH | Pública |
| **Rejeitar e refazer** | `/ordens-servico/{id}/rejeitar-refazer` | PATCH | Pública |
| **Rejeitar e cancelar** | `/ordens-servico/{id}/rejeitar-cancelar` | PATCH | Pública |
| **Confirmar pagamento** | `/ordens-servico/{id}/confirmar-pagamento` | PATCH | Pública |
| **Consultar status (público)** | `/consulta/ordens-servico/{numeroOs}/status` | GET | Pública |

---

## Fluxo da Ordem de Serviço

A OS segue uma máquina de estados controlada pelo domínio:

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → AGUARDANDO_PAGAMENTO → PAGA → ENTREGUE
                  ↑                    |
                  |         rejeitarRefazer()
                  └────────────────────┘
                                       |
                            rejeitarCancelar() → CANCELADA → ENTREGUE
```

**Passo a passo:**

1. **Abrir OS** (`POST /api/v1/ordens-servico`) → cria com status `RECEBIDA` ou `EM_DIAGNOSTICO` (se já houver itens)
2. **Adicionar serviços/peças** → transita para `EM_DIAGNOSTICO`. Peças consomem estoque imediatamente
3. **Enviar para aprovação** → `AGUARDANDO_APROVACAO`
4. **Cliente aprova** → `EM_EXECUCAO` (grava `inicio_execucao`)
5. **Concluir reparo** → `AGUARDANDO_PAGAMENTO` (grava `fim_execucao`)
6. **Confirmar pagamento** → `PAGA` (gera lançamento financeiro)
7. **Entregar veículo** → `ENTREGUE`

**Caminhos alternativos:**
- **Rejeitar e refazer**: cancela o orçamento atual, abre um novo, estorna peças → volta para `EM_DIAGNOSTICO`
- **Rejeitar e cancelar**: cancela o orçamento, estorna peças → `CANCELADA` → pode entregar o veículo

---

## Estrutura do Projeto

```
src/main/java/br/com/oficina/
├── OficinaApplication.java            # Ponto de entrada
│
├── config/                            # Configurações Spring
│   ├── SecurityConfig.java            #   Spring Security + filtros
│   ├── JwtProperties.java             #   Propriedades JWT (secret, issuer, TTL)
│   ├── AdminBootstrap.java            #   Criação do admin no primeiro boot
│   ├── OpenApiConfig.java             #   Swagger/OpenAPI
│   └── SwaggerOrderConfig.java        #   Ordenação customizada no Swagger UI
│
├── domain/                            # CAMADA DE DOMÍNIO (pura)
│   ├── model/                         #   Entidades e Value Objects
│   │   ├── OrdemServico.java          #     Raiz de agregado (ciclo da OS)
│   │   ├── ItemOrcamento.java         #     Itens do orçamento
│   │   ├── Cliente.java, Veiculo.java #     Entidades de cadastro
│   │   ├── Dinheiro.java              #     Value Object monetário
│   │   ├── Documento.java             #     Value Object CPF/CNPJ
│   │   ├── NumeroOS.java              #     Value Object formato OS-MMAAAA-NNNNNN
│   │   ├── Placa.java                 #     Value Object placa veicular
│   │   └── ...                        #     EstoquePeca, Servico, Peca, etc.
│   ├── enums/                         #   StatusOrdemServico, TipoItem, etc.
│   └── exception/                     #   BusinessException
│
├── usecase/                           # CAMADA DE CASOS DE USO
│   ├── OrdemServicoServiceImpl.java   #   Lógica de aplicação da OS
│   ├── ClienteServiceImpl.java        #   CRUD de clientes
│   ├── EstoqueServiceImpl.java        #   Gestão de estoque
│   ├── ...                            #   Demais services
│   └── gateway/                       #   Interfaces (contratos de saída)
│       ├── ClienteRepository.java     #     Contrato para persistência de clientes
│       ├── NotificacaoGateway.java    #     Contrato para notificações
│       ├── TokenGateway.java          #     Contrato para geração de tokens
│       ├── RelatorioGateway.java      #     Contrato para relatórios
│       └── ...                        #     Demais gateways
│
├── infrastructure/                    # CAMADA DE INFRAESTRUTURA
│   ├── controller/                    #   Controllers REST (4 controllers)
│   │   ├── AuthController.java
│   │   ├── AdministrativoOficinaController.java
│   │   ├── TecnicoOficinaController.java
│   │   └── ClienteOficinaController.java
│   ├── persistence/                   #   JPA Entities + Repository implementations
│   │   ├── *JpaEntity.java            #     Entidades JPA (mapeamento ORM)
│   │   ├── Jpa*Repository.java        #     Implementações dos gateways
│   │   └── SpringData*Repository.java #     Interfaces Spring Data
│   ├── security/                      #   JWT (geração e validação de tokens)
│   ├── notification/                  #   LogNotificacaoGateway (notificação fictícia)
│   ├── dto/                           #   Objetos de transporte (request/response)
│   └── exception/                     #   GlobalExceptionHandler, ApiError
│
src/test/java/br/com/oficina/
├── architecture/                      # Testes ArchUnit (4 regras)
├── domain/model/                      # Testes unitários do domínio
├── domain/exception/                  # Testes de exceções
└── integration/                       # Testes E2E (Testcontainers + RestAssured)
```

---

## Testes e Cobertura

### Executar todos os testes

```bash
./mvnw clean verify
```

**105 testes** no total:
- **97 testes unitários** do domínio (sem Spring context)
- **4 testes ArchUnit** (validação de regras arquiteturais)
- **4 testes de integração E2E** (Testcontainers + RestAssured com PostgreSQL 16)

### Cobertura (JaCoCo)

O build exige **≥ 80% de cobertura** (line + branch) no pacote `br.com.oficina.domain.model`. O build **falha** se a cobertura cair abaixo desse limiar.

Relatório HTML: `target/site/jacoco/index.html`

### Classes de domínio cobertas

| Classe | Tipo | Classe de Teste |
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

---

## Validação de Arquitetura (ArchUnit)

4 testes automatizados garantem a integridade da Clean Architecture:

1. **Domínio puro** — `domain.model` não pode depender de Spring, JPA ou Servlet
2. **Domínio isolado** — `domain` não depende de `usecase` nem `infrastructure`
3. **Usecase isolado** — `usecase` não pode depender de `infrastructure`
4. **Camadas unidirecionais** — `infrastructure` → `usecase` → `domain` (nunca o contrário)

Localização: `src/test/java/br/com/oficina/architecture/ArchitectureTest.java`

---

## CI/CD (GitHub Actions)

O pipeline (`ci.yml`) executa 3 jobs a cada push/PR:

1. **Build, Test & Coverage** — `./mvnw -B verify` (compilação + testes + JaCoCo)
2. **SBOM** — gera relatório CycloneDX de dependências
3. **Trivy Scan** — análise de vulnerabilidades (HIGH/CRITICAL)

---

## Banco de Dados

- **PostgreSQL 16** com **Flyway** para migrações versionadas
- Hibernate em modo `validate` (Flyway é o dono do schema)
- 2 migrações: `V1__schema_inicial.sql` (schema completo) + `V2__ordens_servico_inicio_fim_execucao.sql` (timestamps de execução)

---

## Documentação Adicional

Toda a documentação do projeto está organizada em [`/docs`](./docs/README-DOCS.md), incluindo:

| Tema | Local |
|---|---|
| DDD (Storytelling, Event Storming, Linguagem Ubíqua) | `docs/` |
| Decisões arquiteturais (ADR) | `docs/` |
| Segurança (Dependency-Track, Trivy) | `docs/04-security/` |
| Apresentação Tech Challenge | `README-apresentacao-tech-challenge-fase1.md` |
