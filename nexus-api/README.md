# Nexus API

Backend do Nexus — sistema pessoal de produtividade, estudos, treinos e finanças.
Spring Boot 4.1.0 (Spring Framework 7) + Java 21 + PostgreSQL + JWT.

## Stack

- **Java 21**, **Spring Boot 4.1.0** (Web, Data JPA, Security, Validation)
- **PostgreSQL** (via Hibernate/JPA, `ddl-auto=update`)
- **JWT** (`io.jsonwebtoken`) para autenticação stateless
- **Anthropic API** (Claude) para o tutor de estudos (`/api/study-chat`)
- **Lombok**

## Domínios implementados

| Área | Endpoints (prefixo `/api`) |
|---|---|
| Autenticação | `/auth/login`, `/auth` (healthcheck), `/users/register` |
| Tarefas / Edital | `/tasks` |
| Financeiro | `/transactions` |
| Treinos | `/workouts`, `/workout-goals` |
| Estudos — Planos/Matérias/Assuntos | `/study-plans`, `/subjects`, `/topics` |
| Estudos — Questões/Respostas | `/questions`, `/answers` |
| Estudos — Caderno de Erros | `/study-errors` |
| Estudos — Simulados | `/mock-exams` |
| Estudos — Estatísticas | `/study-stats` |
| Estudos — Notas/Materiais | `/study-notes`, `/study-files` |
| IA | `/study-chat` |

Todos exigem `Authorization: Bearer <token>`, exceto `POST /auth/login`, `GET /auth`
e `POST /users/register`.

## Rodando localmente

### Pré-requisitos
- Java 21+
- Maven (ou use o `mvnw` se o projeto tiver um)
- PostgreSQL rodando localmente, com um banco `nexus_db` criado

### Configuração

Nada é obrigatório para rodar localmente — todas as variáveis abaixo têm um valor
padrão de desenvolvimento em `application.properties`. Ainda assim, recomendo pelo
menos exportar `ANTHROPIC_API_KEY` se for usar o tutor de IA:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
# só se sua chave for do tipo "identity-linked" (a Anthropic avisa isso no erro):
export ANTHROPIC_WORKSPACE_ID=wrkspc_...
```

### Rodar

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Variáveis de ambiente (produção)

| Variável | Obrigatória em produção? | Descrição |
|---|---|---|
| `DB_URL` | Sim | JDBC da instância PostgreSQL (ex.: `jdbc:postgresql://host:5432/nexus_db`) |
| `DB_USERNAME` | Sim | Usuário do banco |
| `DB_PASSWORD` | Sim | Senha do banco — **nunca** commitar um valor real no `application.properties` |
| `JWT_SECRET` | Sim | Chave aleatória de pelo menos 256 bits (32 bytes). Gere com `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | Não | Validade do token em ms (padrão: 86400000 = 24h) |
| `ANTHROPIC_API_KEY` | Só se for usar a IA | Chave da API da Anthropic |
| `ANTHROPIC_WORKSPACE_ID` | Só p/ chaves "identity-linked" | Veja `console.anthropic.com` |
| `CORS_ALLOWED_ORIGINS` | Sim | Domínio(s) do frontend em produção, separados por vírgula sem espaço (ex.: `https://app.seudominio.com`) |
| `FILE_UPLOAD_DIR` | Recomendado | Caminho para uploads (imagens de treino, materiais de estudo). Em produção, aponte para um volume persistente — sem isso, os arquivos somem a cada deploy/restart do container |
| `SERVER_PORT` | Não | Porta da aplicação (padrão: 8080) — útil se a plataforma de hospedagem injeta a porta via essa variável |

## Build de produção

```bash
mvn clean package -DskipTests
```

Gera `target/nexus-api-0.0.1-SNAPSHOT.jar` (nome exato pode variar — confira em `target/`).

## Subindo num servidor

1. **Banco**: provisione um PostgreSQL (gerenciado ou na própria VM) e crie o banco `nexus_db`.
2. **Variáveis de ambiente**: defina todas as da tabela acima — via `systemd` (`EnvironmentFile=`), variáveis do painel da plataforma (Railway/Render/Fly.io/etc.), ou um `.env` carregado pelo processo que sobe o jar (nunca commitado).
3. **Rodar o jar**:
   ```bash
   java -jar nexus-api-0.0.1-SNAPSHOT.jar
   ```
4. **HTTPS**: coloque atrás de um proxy reverso (Nginx/Caddy) ou use o HTTPS já
   oferecido pela plataforma — o frontend está em HTTPS e navegadores bloqueiam
   chamadas para uma API em HTTP puro a partir de uma página HTTPS.
5. **CORS**: depois do deploy do frontend, atualize `CORS_ALLOWED_ORIGINS` com o
   domínio real (ex.: `https://nexus.seudominio.com`) e reinicie a API — sem isso o
   navegador bloqueia todas as chamadas do frontend com erro de CORS, mesmo com
   tudo o resto certo.
6. **Uploads**: se estiver em container/plataforma efêmera, monte um volume
   persistente e aponte `FILE_UPLOAD_DIR` para ele.

### Exemplo de serviço systemd

```ini
[Unit]
Description=Nexus API
After=network.target postgresql.service

[Service]
Type=simple
EnvironmentFile=/etc/nexus-api/env
ExecStart=/usr/bin/java -jar /opt/nexus-api/nexus-api.jar
Restart=on-failure
User=nexus

[Install]
WantedBy=multi-user.target
```

`/etc/nexus-api/env` (permissão restrita, nunca no git):
```
DB_URL=jdbc:postgresql://localhost:5432/nexus_db
DB_USERNAME=nexus
DB_PASSWORD=...
JWT_SECRET=...
ANTHROPIC_API_KEY=sk-ant-...
CORS_ALLOWED_ORIGINS=https://app.seudominio.com
```

## Antes do primeiro `git push`

- Confirme que `.gitignore` está no repo (inclui `target/`, `uploads/`, `.env`) —
  sem ele, o jar compilado e eventuais arquivos enviados por usuários entram no
  histórico do git.
- `application.properties` não tem mais nenhuma credencial real hardcoded — só
  placeholders de desenvolvimento local, que não fazem mal se vazarem.

## Observações de arquitetura

- `spring.jpa.hibernate.ddl-auto=update` cria/ajusta as tabelas automaticamente a
  partir das entidades — conveniente agora, mas para um ambiente de produção mais
  maduro vale migrar para Flyway/Liquibase (evita alterações de schema silenciosas
  em produção).
- Autenticação é 100% stateless via JWT no header `Authorization` — sem sessão,
  sem cookie, `csrf` desabilitado de propósito (não se aplica aqui).
- O dono de cada recurso (planos, tarefas, treinos, etc.) é sempre resolvido a
  partir do token, nunca de um `userId` na URL — evita que um usuário acesse dado
  de outro trocando um ID na requisição.
