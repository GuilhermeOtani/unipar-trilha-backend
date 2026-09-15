# Unipar Trilha API

Backend do piloto **Unipar Trilha**, desenvolvido para entregar primeiro o ciclo:

```text
Professor cria e publica
→ distribui para a turma
→ aluno pratica e recebe feedback
→ professor acompanha os resultados
```

O projeto é novo e usa o mesmo padrão de organização por camada técnica do GulaPay, sem reaproveitar regras ou entidades do domínio FoodService.

## Estado atual

Backend e frontend possuem os contratos e jornadas do ciclo principal implementados. O frontend de produção consome a API real; mocks existem somente em testes e no preview isolado.

| Área | Entrega | Estado |
|---|---|---|
| Infraestrutura | Spring Boot, perfis, Flyway, Swagger, health, CORS, Hibernate e auditoria | concluído |
| Autenticação | usuários, JWT, segurança, seeds, perfil corrente e gestão administrativa | concluído |
| Autoria | contexto, rascunhos, árvore completa, ownership e versões imutáveis | concluído |
| Distribuição | criação e listagem isolada por professor/turma | concluído |
| Aluno | catálogo, caminho, sessão, tentativas, feedback, retomada e conclusão | concluído |
| Prazo e ativação | retomada após prazo, bloqueio de nova sessão e bloqueio por `ativo=false` | concluído |
| Acompanhamento | indicadores e histórico de versões | concluído |
| Validação | 24 testes backend e 123 testes frontend | concluído automaticamente |

## Tecnologias

- Java 21.
- Maven 3.9+.
- Spring Boot 3.3.4.
- Spring Web, Validation, Data JPA com Hibernate ORM e Security.
- PostgreSQL 14+ para `dev` e `prod`.
- H2 in-memory para testes.
- Flyway com seis migrations.
- JWT HS256 com JJWT 0.12.6.
- SpringDoc OpenAPI 2.6.0.
- Actuator.
- Lombok.
- JUnit 5, MockMvc, AssertJ e Spring Security Test.

O Flyway é responsável por criar/evoluir o schema. O Hibernate ORM, incluído pelo
`spring-boot-starter-data-jpa`, faz o mapeamento das entidades e usa
`spring.jpa.hibernate.ddl-auto=validate`, sem criar ou alterar tabelas automaticamente.

## Estrutura

```text
src/main/java/br/unipar/trilha/
├── bootstrap/      dados iniciais do perfil dev
├── configs/        segurança, CORS, auditoria e OpenAPI
├── controllers/    endpoints REST
├── dtos/           contratos de entrada e saída
├── entities/       entidades JPA
├── enums/          perfis e estados do domínio
├── exceptions/     RFC 7807 e exceções de negócio
├── repositories/   Spring Data JPA e consultas agregadas
├── security/       token e filtro JWT
└── services/       regras de negócio e transações
```

O plano detalhado usado na implementação está em `PLANO_IMPLEMENTACAO_BACKEND.md` dentro deste projeto.

## Como executar

### Pré-requisitos

- JDK 21.
- Maven 3.9+.
- PostgreSQL 14+.

### Banco de desenvolvimento

```sql
CREATE DATABASE unipar_trilha;
```

Variáveis disponíveis:

| Variável | Default em desenvolvimento |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/unipar_trilha` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `JWT_SECRET` | `CHANGE_ME_DEV_SECRET_USE_AT_LEAST_32_CHARS_FOR_HS256` |
| `JWT_EXPIRATION_MINUTES` | `480` |
| `SERVER_PORT` | `8080` |
| `SPRING_PROFILES_ACTIVE` | `dev` |
| `CORS_ALLOWED_ORIGINS` | `*` |

Comandos:

```bash
mvn clean verify
mvn spring-boot:run
```

Endereços locais:

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Dados criados automaticamente em dev

| Perfil | Login | Senha |
|---|---|---|
| Administrador | `admin` | `admin123` |
| Professor | `professor` | `prof123` |
| Aluno | `aluno` | `aluno123` |

O seed também cria:

- disciplina `ALG-001 — Algoritmos e Lógica de Programação`;
- turma `Turma Piloto — 2026/2`;
- vínculo do professor com a turma;
- matrícula do aluno na turma.

O seed roda somente com perfil `dev` e é idempotente.

## Endpoints implementados

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/auth/login` | público |
| `GET` | `/usuarios/me` | autenticado |
| `POST` | `/usuarios` | administrador |
| `GET` | `/usuarios?perfil=ALUNO` | administrador |
| `GET` | `/professor/contexto` | professor |
| `GET` | `/trilhas` | professor; somente seus rascunhos |
| `POST` | `/trilhas` | professor |
| `GET` | `/trilhas/{id}` | professor dono |
| `PUT` | `/trilhas/{id}` | professor dono |
| `POST` | `/trilhas/{id}/publicacoes` | professor dono |
| `POST` | `/distribuicoes` | professor vinculado |
| `GET` | `/distribuicoes?turmaId={id}` | professor vinculado e criador |
| `GET` | `/aluno/distribuicoes` | aluno matriculado |
| `GET` | `/aluno/distribuicoes/{id}/caminho` | aluno matriculado |
| `POST` | `/aluno/distribuicoes/{id}/sessoes` | aluno matriculado |
| `GET` | `/aluno/sessoes/{id}` | aluno dono |
| `POST` | `/aluno/sessoes/{id}/respostas` | aluno dono |
| `GET` | `/professor/turmas/{id}/indicadores` | professor vinculado |

## Contratos essenciais para o frontend

### Login

```json
POST /auth/login
{
  "login": "professor",
  "senha": "prof123"
}
```

```json
200 OK
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresInMinutes": 480,
  "usuarioId": 2,
  "login": "professor",
  "nome": "Professor Demo",
  "perfil": "PROFESSOR"
}
```

O Flutter deve salvar `accessToken` e enviar `Authorization: Bearer <token>`.

### Criar rascunho

```json
POST /trilhas
{
  "titulo": "Fundamentos da lógica",
  "descricao": "Trilha piloto",
  "disciplinaId": 1
}
```

A resposta contém `id`, `status=RASCUNHO`, disciplina, professor, datas e `modulos=[]`.

### Listar rascunhos do professor

`GET /trilhas` devolve somente trilhas pertencentes ao professor autenticado. Cada item contém `id`, `titulo`, `descricao`, `status`, disciplina, `atualizadoEm` e `publicacoes` ordenadas da mais recente para a mais antiga, com `versaoId`, `numeroVersao` e `publicadaEm`.

### Salvar conteúdo completo

```json
PUT /trilhas/{id}
{
  "titulo": "Fundamentos da lógica",
  "descricao": "Trilha piloto",
  "disciplinaId": 1,
  "modulos": [{
    "titulo": "Estruturas condicionais",
    "ordem": 1,
    "licoes": [{
      "titulo": "If e else",
      "resumo": "Escolhendo caminhos",
      "ordem": 1,
      "desafios": [{
        "enunciado": "Qual saída será exibida?",
        "tipo": "MULTIPLA_ESCOLHA",
        "dificuldade": "FACIL",
        "explicacao": "A condição é verdadeira.",
        "ordem": 1,
        "opcoes": [
          {"texto": "A", "ordem": 1, "correta": true},
          {"texto": "B", "ordem": 2, "correta": false}
        ]
      }]
    }]
  }]
}
```

Somente `MULTIPLA_ESCOLHA` é aceito. Cada desafio precisa de pelo menos duas opções e exatamente uma correta.

### Publicar

```text
POST /trilhas/{id}/publicacoes
```

Não precisa de corpo. Retorna `201`:

```json
{
  "trilhaId": 1,
  "versaoId": 1,
  "numeroVersao": 1,
  "publicadaEm": "2026-09-14T18:00:00"
}
```

### Distribuir

```json
POST /distribuicoes
{
  "versaoId": 1,
  "turmaId": 1,
  "disponivelDe": "2026-09-15T00:00:00",
  "disponivelAte": null
}
```

O frontend deve obter `turmaId` em `/professor/contexto` e `versaoId` na publicação; não deve pedir IDs manualmente ao usuário.

### Catálogo do aluno

```json
GET /aluno/distribuicoes
{
  "distribuicoes": [{
    "distribuicaoId": 1,
    "trilhaTitulo": "Fundamentos da lógica",
    "disciplinaNome": "Algoritmos e Lógica de Programação",
    "numeroVersao": 1,
    "totalDesafios": 3,
    "percentualProgresso": 0,
    "concluida": false,
    "sessaoId": null,
    "disponivelDe": "2026-09-15T00:00:00",
    "disponivelAte": null,
    "prazoEncerrado": false
  }]
}
```

Esse endpoint nunca devolve respostas corretas.

### Caminho do aluno

`GET /aluno/distribuicoes/{id}/caminho` retorna a identificação da distribuição e da versão, progresso, módulos ordenados e lições com `totalDesafios`, `desafiosConcluidos` e status `CONCLUIDA`, `ATUAL` ou `BLOQUEADA`.

A primeira lição incompleta é a atual. As seguintes ficam bloqueadas. O cálculo considera desafios respondidos corretamente ao menos uma vez e o JSON não contém opções nem indicadores de resposta correta.

### Prazo e distribuição inativa

- Uma sessão iniciada pode ser retomada e concluída depois de `disponivelAte`.
- Uma nova sessão depois do prazo retorna `409`.
- Uma distribuição vencida só permanece no catálogo do aluno quando já existe sessão.
- `ativo=false` bloqueia consulta, retomada e respostas de sessões incompletas.
- Sessões concluídas continuam consultáveis como histórico mesmo com a distribuição inativa.

### Iniciar ou retomar

```text
POST /aluno/distribuicoes/{distribuicaoId}/sessoes
```

Retorna `200` tanto ao criar quanto ao retomar. Campos principais:

```json
{
  "sessaoId": 1,
  "distribuicaoId": 1,
  "trilhaTitulo": "Fundamentos da lógica",
  "numeroVersao": 1,
  "licaoTitulo": "If e else",
  "status": "EM_ANDAMENTO",
  "progresso": {
    "respondidos": 0,
    "total": 3,
    "percentual": 0,
    "concluida": false
  },
  "desafioAtual": {
    "id": 1,
    "enunciado": "Qual saída será exibida?",
    "tipo": "MULTIPLA_ESCOLHA",
    "dificuldade": "FACIL",
    "opcoes": [{"id": 1, "texto": "A"}]
  }
}
```

Não existe campo `correta` nas opções do aluno.

### Responder

```json
POST /aluno/sessoes/{sessaoId}/respostas
{
  "desafioId": 1,
  "opcaoId": 1
}
```

```json
{
  "correta": true,
  "feedback": "Correto. A condição é verdadeira.",
  "progresso": {
    "respondidos": 1,
    "total": 3,
    "percentual": 33,
    "concluida": false
  },
  "proximoDesafio": {
    "id": 2,
    "enunciado": "Próxima questão",
    "tipo": "MULTIPLA_ESCOLHA",
    "dificuldade": "FACIL",
    "opcoes": [{"id": 3, "texto": "Opção"}]
  }
}
```

Com resposta incorreta, `proximoDesafio` representa o desafio atual para permitir outra tentativa. Ao concluir, `proximoDesafio` é `null`.

### Indicadores

`GET /professor/turmas/{turmaId}/indicadores` retorna:

- `matriculados`, `iniciaram` e `concluiram`;
- `percentualConclusao`;
- `acertos`, `erros` e `acuracia`;
- `desafiosComMaisErros`, contendo `desafioId`, `enunciado`, `numeroVersao`, `tentativas`, `erros` e `taxaErro`.

## Erros

Todos os erros usam `Content-Type: application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Erro de validação",
  "status": 400,
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/trilhas",
  "errors": {
    "titulo": "título é obrigatório"
  }
}
```

Status utilizados:

- `400`: JSON ou campos inválidos.
- `401`: token ausente, inválido ou credencial incorreta.
- `403`: papel ou vínculo sem permissão.
- `404`: recurso inexistente ou não visível ao usuário.
- `409`: regra de negócio, versão duplicada, datas ou estado incompatível.

## Migrations

| Versão | Tabelas |
|---|---|
| V1 | `usuario` |
| V2 | `disciplina`, `turma`, `vinculo_professor`, `matricula_aluno` |
| V3 | `trilha`, `modulo`, `licao`, `desafio`, `opcao_desafio` |
| V4 | `trilha_versao` e snapshots de toda a árvore |
| V5 | `distribuicao` |
| V6 | `sessao_aprendizagem`, `tentativa` |

Não modificar migrations aplicadas. Qualquer mudança futura começa em V7.

## Testes executados

A suíte automatizada usa H2 no modo PostgreSQL e executa Flyway do zero.

`CicloCompletoIntegrationTest` cobre:

- login de professor e aluno;
- contexto acadêmico;
- criação e edição de trilha;
- publicação V1;
- distribuição;
- catálogo sem resposta correta;
- sessão e retomada;
- tentativa errada e correta;
- progresso de 0%, 50% e 100%;
- painel da turma;
- publicação V2 preservando indicadores da V1;
- bloqueio de aluno em recurso do professor;
- RFC 7807 para `400`, `401` e `403`.

Também são testados isolamento das listagens por professor, ordem V2/V1, caminho antes/durante/depois da prática, continuação após prazo, bloqueio de nova sessão vencida, distribuição inativa e ausência de respostas corretas no caminho.

Resultado validado em 14/09/2026:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

## Ajustes feitos em relação ao planejamento inicial

- O projeto foi criado limpo; apenas a estrutura por camadas do GulaPay foi mantida.
- `percentualComissao` foi removido por pertencer ao domínio FoodService.
- IDs são `Long`, nomes são em português e as rotas não usam `/api`, como combinado.
- A V3 cria toda a estrutura de rascunho antes das implementações 2.2/2.3; ela não é reeditada depois.
- A V6 cria sessão e tentativa de uma vez; ela não é reeditada na etapa 4.2.
- O endpoint de início/retomada retorna sempre `200`, pois a mesma chamada pode criar ou devolver uma sessão existente.
- Os novos contratos de listagem, catálogo ampliado e caminho são calculados com as entidades existentes; nenhuma migration nova foi necessária.
- O Flutter de produção foi ligado a todos os contratos; `main_preview.dart` continua isolado para demonstração visual.

## Checklist para o frontend

- Usar a base URL sem acrescentar `/api`.
- Enviar os campos `login` e `senha`.
- Salvar `accessToken` e usar Bearer.
- Usar IDs como inteiros.
- Usar `turmas` de `/professor/contexto` para escolher disciplina/turma.
- Guardar o `versaoId` devolvido na publicação.
- Usar `GET /trilhas` para reencontrar rascunhos e versões sem estado local.
- Usar `GET /distribuicoes?turmaId=` para mostrar o histórico real da turma.
- Não procurar `correta` no DTO do aluno.
- Não procurar opções no contrato de caminho; ele descreve módulos e lições, não desafios.
- Usar o progresso devolvido pelo backend, sem recalcular na tela.
- Recarregar `/aluno/distribuicoes` depois de uma resposta/conclusão.
- Exibir `detail` e `errors` dos Problem Details de forma amigável.
