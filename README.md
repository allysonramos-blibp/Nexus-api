# ⚡ Nexus API — Core Backend & Engine de Produtividade

> **API RESTful de alta performance desenvolvida em Spring Boot 4.1 e Java 21, responsável pelo gerenciamento unificado de estudos para concursos, produtividade, rotina de treinos e finanças pessoais com suporte a IA generativa.**

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot 4.1" />
  <img src="https://img.shields.io/badge/Spring_Security-6.x-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/PostgreSQL-16+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/JWT-Stateless-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Anthropic_Claude-AI-D97706?style=for-the-badge&logo=anthropic&logoColor=white" alt="Claude AI" />
  <img src="https://img.shields.io/badge/Apache_PDFBox-3.x-D22128?style=for-the-badge&logo=apache&logoColor=white" alt="PDFBox" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

---

## 📌 Sumário

1. [Visão Geral](#-visão-geral)
2. [Arquitetura & Tecnologias](#-arquitetura--tecnologias)
3. [Domínios & Módulos do Sistema](#-domínios--módulos-do-sistema)
4. [Matriz de Endpoints (REST API)](#-matriz-de-endpoints-rest-api)
5. [Processamento Inteligente de PDFs com IA](#-processamento-inteligente-de-pdfs-com-ia)
6. [Segurança, CORS & RBAC](#-segurança-cors--rbac)
7. [Como Executar Localmente](#-como-executar-localmente)
8. [Variáveis de Ambiente](#-variáveis-de-ambiente)
9. [Build & Deploy em Produção](#-build--deploy-em-produção)
10. [Estrutura do Projeto](#-estrutura-do-projeto)
11. [Autor](#-autor)

---

## 🌟 Visão Geral

A **Nexus API** é a espinha dorsal do ecossistema Nexus. Desenvolvida sob os princípios de arquitetura limpa e desacoplamento de domínios, ela provê serviços robustos para:

- 🔐 Autenticação stateless via tokens JWT de alta segurança e autorização granular.
- 📋 Gestão de tarefas com duplo paradigma (fluxo operacional de rotina vs. verticalização de tópicos de edital).
- 🎓 Sistema completo de estudos: planos, disciplinas, assuntos, banco de questões, gabaritos e simulados oficiais com cronômetro.
- 🔁 Algoritmo de repetição espaçada e caderno de erros automatizado.
- 🤖 Extração automática de questões de provas em PDF através da integração com Apache PDFBox e LLMs (Claude / Gemini).
- 🏋️ Registro fisiológico de treinos de musculação, progressão de cargas e fotos de evolução.
- 💵 Controle financeiro com lançamentos de receitas, despesas e status de contas pendentes.

---

## 🛠️ Arquitetura & Tecnologias

- **Linguagem & Runtime:** Java 21 (LTS) aproveitando Virtual Threads e Pattern Matching.
- **Framework Core:** Spring Boot 4.1.0 (Spring Framework 7).
- **Acesso a Dados:** Spring Data JPA / Hibernate com PostgreSQL.
- **Segurança:** Spring Security 6 com filtro `JwtAuthenticationFilter` stateless e hash BCrypt para senhas.
- **Inteligência Artificial:** Integração via cliente HTTP para Anthropic Claude API (análise semântica e estruturação de questões em JSON).
- **Processamento de Documentos:** Apache PDFBox para parsing e extração textual de PDFs.
- **Produtividade de Código:** Project Lombok para redução de boilerplate.
- **Contêineres:** Dockerfile multi-stage pronto para implantação em Cloud Run, AWS ECS ou VPS.

---

## 🧩 Domínios & Módulos do Sistema

### 1. Autenticação & Gestão de Usuários
- Registro de novos usuários com atribuição automática de planos (`FREE`, `PRO`, `ENTERPRISE`).
- Emissão e validação de tokens JWT com expiração configurável.
- Isolamento absoluto de dados: cada recurso pertence obrigatoriamente ao `userId` extraído do token JWT autenticado, prevenindo vulnerabilidades de Broken Object Level Authorization (BOLA/IDOR).

### 2. Módulo de Tarefas (Dual Workflow)
O sistema suporta duas lógicas operacionais de tarefas no mesmo modelo:
1. **Tarefas de Rotina (`ehTopicoEdital = false`):**
   - Gerenciadas por `workflowStatus` (`PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`).
   - Suporte a horários específicos, prioridades (`BAIXA`, `MEDIA`, `ALTA`) e categorias.
2. **Tópicos de Edital (`ehTopicoEdital = true`):**
   - Gerenciados por `status` de maturidade do estudo (`PENDENTE`, `TEORIA_VISTA`, `QUESTOES_FEITAS`, `DOMINADO`).

### 3. Módulo de Estudos & Concursos Públicos
- **Planos de Estudos (`StudyPlan`):** Definição de concurso-alvo, data da prova e carga horária disponível.
- **Matérias (`Subject`) & Assuntos (`Topic`):** Árvore de conhecimento hierárquica.
- **Questões & Respostas (`Question` & `Answer`):** Cadastro de enunciados, alternativas, gabarito e explicação teórica.
- **Simulados Oficiais (`MockExam`):**
  - Geração de simulados customizados por quantidade de questões e matérias.
  - Cronômetro regressivo e registro inviolável de respostas (uma resposta por questão).
  - Cálculo consolidado de aproveitamento geral e por matéria.

### 4. Caderno de Erros & Repetição Espaçada
- Toda resposta incorreta em questões ou simulados é automaticamente indexada na entidade `StudyError`.
- Suporte a ciclo de revisões periódicas para fixação de longo prazo.

### 5. Extração de PDFs via IA (`POST /api/questions/extract-pdf`)
- O usuário envia um arquivo PDF de prova de concurso (FCC, Cebraspe, FGV, etc.).
- O Apache PDFBox extrai os blocos textuais.
- O payload é enviado para a IA através de um prompt de engenharia reversa que estrutura enunciado, alternativas A/B/C/D/E, gabarito e matéria em formato JSON rigorosamente validado.
- Inserção em lote via `POST /api/topics/{topicId}/questions/bulk`.

### 6. Módulo de Treinos & Fisiologia
- Registro diário de treinos (`Workout`) associado a grupos musculares trabalhados.
- Lista estruturada de exercícios com séries, repetições e cargas (kg).
- Armazenamento de fotos de evolução física em disco persistente (`/uploads`).
- Definição de metas semanais de frequência física (`WorkoutGoal`).

### 7. Módulo Financeiro
- Transações de receitas e despesas com valores em ponto flutuante de alta precisão.
- Controle de status: transações podem ser registradas como `PENDENTE` (contas a pagar/receber) e liquidadas com `PATCH /api/transactions/{id}/concluir`.

---

## 📡 Matriz de Endpoints (REST API)

Todas as rotas possuem o prefixo `/api`.  
Todas as rotas (exceto as marcadas como 🔓 **Pública**) exigem o header `Authorization: Bearer <seu_token_jwt>`.

### 🔐 Autenticação & Usuários
| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/auth/login` | 🔓 Pública | Autenticação de credenciais; retorna JWT e dados do usuário |
| `GET` | `/auth` | 🔓 Pública | Healthcheck da API e verificação de conectividade |
| `POST` | `/users/register` | 🔓 Pública | Criação de nova conta de usuário |

### 📋 Tarefas & Produtividade
| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/tasks/user/{userId}` | Lista todas as tarefas do usuário autenticado |
| `POST` | `/tasks` | Cria nova tarefa ou tópico de edital |
| `PUT` | `/tasks/{id}` | Atualiza dados descritivos (título, descrição, horário) |
| `PATCH` | `/tasks/{id}/workflow-status` | Altera status de tarefa de rotina (`CONCLUIDA`, `EM_ANDAMENTO`, etc.) |
| `PATCH` | `/tasks/{id}/status` | Altera maturidade de tópico de edital (`DOMINADO`, etc.) |
| `DELETE`| `/tasks/{id}` | Remove uma tarefa existente |

### 📚 Estudos, Planos & Assuntos
| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/study-plans/user/{userId}` | Lista os planos de estudos do usuário |
| `POST` | `/study-plans` | Cria um novo plano de estudo |
| `GET` | `/study-plans/{id}` | Detalhes do plano com matérias e progresso consolidado |
| `POST` | `/subjects` | Cadastra matéria vinculada a um plano |
| `POST` | `/topics` | Cadastra assunto vinculado a uma matéria |
| `GET` | `/study-stats/overview` | Estatísticas gerais de aproveitamento do usuário |

### ❓ Questões, Simulados & Caderno de Erros
| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/questions/topic/{topicId}` | Lista questões cadastradas para determinado assunto |
| `POST` | `/questions` | Cria questão avulsa |
| `POST` | `/questions/extract-pdf` | Extrai questões estruturadas a partir de arquivo PDF via IA |
| `POST` | `/topics/{topicId}/questions/bulk` | Salva questões em lote no assunto especificado |
| `POST` | `/answers` | Registra a resposta dada pelo usuário e calcula acerto/erro |
| `GET` | `/study-errors` | Lista questões erradas que exigem revisão |
| `PATCH` | `/study-errors/{id}/review` | Marca erro como revisado na repetição espaçada |
| `POST` | `/mock-exams` | Cria e inicia novo simulado cronometrado |
| `GET` | `/mock-exams/{id}` | Detalhes, questões e resultado do simulado |
| `POST` | `/mock-exams/{id}/finish` | Finaliza simulado e emite placar oficial |

### 🏋️ Treinos & Saúde
| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/workouts/user/{userId}` | Histórico de treinos realizados |
| `POST` | `/workouts` | Registra novo treino com exercícios, séries e cargas |
| `POST` | `/workouts/{id}/photo` | Upload de foto do treino (Multipart) |
| `GET` | `/workout-goals/{userId}` | Retorna a meta semanal de treinos (ex: 4x/semana) |
| `PUT` | `/workout-goals/{userId}` | Atualiza a meta semanal de treinos |

### 💰 Finanças Pessoais
| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/transactions/user/{userId}` | Lista transações financeiras |
| `POST` | `/transactions/user/{userId}` | Cria nova receita ou despesa |
| `PATCH` | `/transactions/{id}/concluir` | Confirma quitação de conta pendente |
| `DELETE`| `/transactions/{id}` | Exclui lançamento financeiro |
| `GET` | `/categories` | Lista categorias de receitas e despesas |

### 🤖 Tutor de Inteligência Artificial
| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/study-chat` | Envia mensagem pedagógica para o modelo Claude/Gemini com histórico |

---

## 🔒 Segurança, CORS & RBAC

- **JWT de 256-bits:** Assinatura criptográfica HMAC-SHA256 validada a cada requisição pelo `JwtAuthenticationFilter`.
- **Configuração de CORS:** Gerenciada centralmente em `SecurityConfig.java` através do bean `corsConfigurationSource()`. A variável de ambiente `CORS_ALLOWED_ORIGINS` permite especificar os domínios autorizados (ex: `https://nexus.meudominio.com`).
- **Proteção CSRF:** Desabilitada intencionalmente (`csrf.disable()`) visto que a API é 100% stateless via cabeçalho `Authorization: Bearer` e não utiliza cookies de sessão.

---

## 🚀 Como Executar Localmente

### Pré-requisitos
- **Java 21** instalado (`java -version`)
- **Maven 3.8+** (ou utilizar o wrapper `./mvnw`)
- **PostgreSQL 14+** rodando localmente com a database `nexus_db` criada

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/allysonramos-blibp/Nexus-api.git
   cd Nexus-api/nexus-api
   ```

2. **Crie o banco de dados PostgreSQL:**
   ```sql
   CREATE DATABASE nexus_db;
   ```

3. **Configure as credenciais locais:**
   Crie ou exporte as variáveis no seu terminal:
   ```bash
   export DB_URL="jdbc:postgresql://localhost:5432/nexus_db"
   export DB_USERNAME="postgres"
   export DB_PASSWORD="sua_senha_postgres"
   export JWT_SECRET="umaChaveAleatoriaSuperSeguraComPeloMenos32BytesDeComprimento123456"
   export ANTHROPIC_API_KEY="sk-ant-..." # Opcional: para tutor de IA e extrator de PDF
   ```

4. **Execute a aplicação:**
   ```bash
   ./mvnw spring-boot:run
   ```
   A API iniciará na porta `8080` (`http://localhost:8080/api/auth` para verificar o healthcheck).

---

## ⚙️ Variáveis de Ambiente

| Variável | Obrigatória? | Valor Padrão | Descrição |
|---|---|---|---|
| `DB_URL` | Sim | `jdbc:postgresql://localhost:5432/nexus_db` | URL JDBC de conexão ao PostgreSQL |
| `DB_USERNAME` | Sim | `postgres` | Usuário do banco de dados |
| `DB_PASSWORD` | Sim | — | Senha do banco de dados |
| `JWT_SECRET` | Sim | — | Chave de assinatura JWT (mínimo 256 bits) |
| `JWT_EXPIRATION_MS`| Não | `86400000` (24 horas) | Tempo de expiração do token em milissegundos |
| `CORS_ALLOWED_ORIGINS`| Sim | `http://localhost:5173,http://localhost:3000` | Domínios do frontend com permissão de acesso |
| `ANTHROPIC_API_KEY` | Não | — | Chave da Anthropic para o tutor e extrator de PDF |
| `FILE_UPLOAD_DIR` | Não | `./uploads` | Diretório em disco para salvar fotos de treinos |
| `SERVER_PORT` | Não | `8080` | Porta HTTP da aplicação |

---

## 📦 Build & Deploy em Produção

### 1. Build do Pacote JAR
```bash
./mvnw clean package -DskipTests
```
O artefato compilado e auto-executável será gerado em:  
`target/nexus-api-0.0.1-SNAPSHOT.jar`

### 2. Executando com Java direto
```bash
java -jar -Dspring.profiles.active=prod target/nexus-api-0.0.1-SNAPSHOT.jar
```

### 3. Exemplo de Dockerfile Multi-Stage
```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📂 Estrutura do Projeto

```
nexus-api/
├── src/
│   ├── main/
│   │   ├── java/com/nexus/
│   │   │   ├── config/          # SecurityConfig, CorsConfig, JwtFilter
│   │   │   ├── controllers/     # Endpoints REST (Auth, Tasks, Study, Workouts, Finance)
│   │   │   ├── dto/             # Request & Response Data Transfer Objects
│   │   │   ├── models/          # Entidades JPA (User, Task, StudyPlan, Workout, etc.)
│   │   │   ├── repositories/    # Interfaces Spring Data JPA
│   │   │   └── services/        # Regras de negócio e integrações (AI, PDFBox, Token)
│   │   └── resources/
│   │       └── application.properties # Configurações padrão do Spring Boot
│   └── test/                    # Testes unitários e de integração
├── Dockerfile                   # Configuração de imagem contêiner
├── mvnw & mvnw.cmd              # Maven Wrapper
└── pom.xml                      # Dependências do projeto Maven
```

---

## 👨‍💻 Autor

Desenvolvido por **Allyson Ramos**  
- **GitHub:** [@allysonramos-blibp](https://github.com/allysonramos-blibp)  
- **Email:** allysonr510@gmail.com  

---

<p align="center">
  <sub>Nexus API © Todos os direitos reservados. Arquitetura robusta para suportar sua melhor versão.</sub>
</p>
