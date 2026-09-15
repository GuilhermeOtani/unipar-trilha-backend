# Plano de implementação — Backend

## 1. Objetivo da entrega

Entregar até **24/09/2026** o ciclo completo:

```text
Professor cria e publica uma trilha
→ disponibiliza para uma turma
→ aluno pratica e recebe feedback
→ sistema registra tentativas e progresso
→ professor acompanha os resultados
```

Este arquivo usa exatamente as mesmas etapas do `../PLANO_IMPLEMENTACAO_FRONTEND.md`. A conclusão de uma etapa exige integração com o frontend real e regressão das etapas anteriores.

### Estado em 14/09/2026

Os contratos do ciclo principal estão implementados e ligados ao Flutter de produção. Foram acrescentadas, sem nova migration, as consultas de rascunhos/publicações do professor, distribuições por turma, catálogo ampliado e caminho do aluno. A suíte backend possui 24 testes aprovados; a suíte Flutter possui 123 testes aprovados e um teste externo opcional ignorado.

## 2. Regra central de conclusão

Uma etapa não está pronta apenas porque o endpoint funciona no Swagger.

Para concluir uma etapa:

1. backend, frontend e banco reais devem estar conectados;
2. os testes automatizados da etapa devem passar;
3. o cenário integrado descrito no aceite deve funcionar;
4. todas as funcionalidades das etapas anteriores devem continuar funcionando;
5. nenhum mock pode estar ativo no fluxo entregue.

Mocks e exemplos JSON servem somente para o frontend e o backend trabalharem em paralelo. Eles nunca substituem a integração real.

## 3. Escopo obrigatório e limites

### Obrigatório

- Login JWT de administrador, professor e aluno.
- Perfil corrente e gestão de usuários apenas pela API.
- Uma disciplina e uma turma piloto.
- Trilha com módulos, lições e desafios de múltipla escolha.
- Rascunho editável e versões publicadas imutáveis.
- Distribuição de versão publicada para turma.
- Sessão do aluno, tentativas, feedback, retomada e conclusão.
- Indicadores básicos da turma e desafios com maior taxa de erro.

### Fora do MVP

- Execução de código, IA, ranking, conquistas, chat e notas oficiais.
- Recuperação de senha e refresh token.
- Tipos de desafio diferentes de `MULTIPLA_ESCOLHA`.
- Cadastro acadêmico pelo frontend.
- Múltiplas instituições.

## 4. Organização da equipe

| Pessoa | Responsabilidade principal | Par integrado |
|---|---|---|
| B1 | fundação, usuários, segurança, integração e revisão | F1 |
| B2 | estrutura acadêmica, trilhas, versões e distribuição | F2 |
| B3 | sessões, tentativas, progresso e indicadores | F3 |

Cada pessoa mantém somente um item em andamento. Cada item gera uma alteração pequena e revisável. O backend pode iniciar após o contrato do item estar definido; não precisa esperar a tela correspondente.

## 5. Estrutura e tecnologias

Projeto novo, seguindo a estrutura por camada técnica do GulaPay:

```text
src/main/java/br/unipar/trilha/
├── bootstrap/
├── configs/
├── controllers/
├── dtos/
├── entities/
├── enums/
├── exceptions/
├── repositories/
├── security/
└── services/
```

Tecnologias obrigatórias:

- Java 21 e Maven 3.9+.
- Spring Boot 3.3.4.
- Spring Web, Validation, Data JPA com Hibernate ORM e Security.
- JWT HS256 com JJWT e validade padrão de 480 minutos.
- PostgreSQL 14+ em `dev` e `prod`.
- H2 in-memory em `test`.
- Flyway para todas as alterações de banco.
- Hibernate configurado com `ddl-auto: validate`; criação e alteração de schema ficam exclusivamente no Flyway.
- SpringDoc OpenAPI e Swagger em `/swagger-ui.html`.
- Actuator health em `/actuator/health`.
- Lombok conforme o projeto de referência.
- RFC 7807 para erros.
- `BaseEntity` e `JpaAuditingConfig` para auditoria.

Dependências do `pom.xml` devem acompanhar as versões compatíveis usadas pelo GulaPay: JJWT `0.12.6` e SpringDoc `2.6.0`.

## 6. Convenções obrigatórias

- Pacote raiz: `br.unipar.trilha`.
- IDs persistidos: `Long` com `GenerationType.IDENTITY`.
- Rotas sem prefixo `/api`.
- Código, endpoints e JSON em português.
- JSON em `camelCase`; enums em `UPPER_SNAKE_CASE`.
- Perfis: `ADMINISTRADOR`, `PROFESSOR` e `ALUNO`.
- Controllers recebem/devolvem DTOs e nunca entidades JPA.
- Controller chama service; service chama repository.
- Operações que alteram uma árvore completa usam `@Transactional`.
- Senhas usam `BCryptPasswordEncoder` e nunca aparecem em respostas ou logs.
- Consultas de professor/aluno verificam papel e vínculo acadêmico.
- DTO enviado ao aluno nunca contém `correta` ou outra resposta esperada.
- Datas são ISO 8601. Persistir instantes de publicação e tentativa de forma consistente.

Perfis e variáveis:

| Variável | Default em desenvolvimento |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/unipar_trilha` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `JWT_SECRET` | `CHANGE_ME_DEV_SECRET_USE_AT_LEAST_32_CHARS_FOR_HS256` |
| `JWT_EXPIRATION_MINUTES` | `480` |
| `SERVER_PORT` | `8080` |
| `SPRING_PROFILES_ACTIVE` | `dev` |

## 7. Migrations e modelo mínimo

| Migration | Responsabilidade |
|---|---|
| `V1__create_usuario_table.sql` | usuário, perfil e estado ativo |
| `V2__create_estrutura_academica.sql` | disciplina, turma, vínculo do professor e matrícula do aluno |
| `V3__create_conteudo_rascunho.sql` | trilha, módulo, lição, desafio e opção |
| `V4__create_versoes_publicadas.sql` | versão e snapshots imutáveis da árvore |
| `V5__create_distribuicao.sql` | distribuição de versão para turma |
| `V6__create_aprendizagem.sql` | sessão e tentativa |

Regras do modelo:

- `Trilha` representa o rascunho editável e pertence a um professor e uma disciplina.
- `Modulo`, `Licao`, `Desafio` e `OpcaoDesafio` possuem campo `ordem`.
- Publicar copia a árvore para tabelas de versão; não transforma as tabelas de rascunho em conteúdo publicado.
- `Distribuicao` referencia `TrilhaVersao`, nunca `Trilha`.
- `SessaoAprendizagem` referencia aluno e distribuição.
- `Tentativa` referencia sessão e desafio da versão publicada.
- Resposta errada registra tentativa e mantém o desafio atual.
- Resposta correta registra tentativa e avança.
- Progresso é `desafios acertados ao menos uma vez / total de desafios`.

## 8. Contratos compartilhados

Os nomes abaixo são obrigatórios nos dois projetos.

| Método e rota | Perfil | Uso |
|---|---|---|
| `POST /auth/login` | público | autenticar |
| `GET /usuarios/me` | autenticado | perfil corrente |
| `POST /usuarios` | administrador | cadastrar usuário |
| `GET /usuarios?perfil=` | administrador | listar por perfil |
| `GET /professor/contexto` | professor | consultar disciplina e turma |
| `GET /trilhas` | professor | listar os próprios rascunhos e publicações |
| `POST /trilhas` | professor | criar rascunho básico |
| `GET /trilhas/{id}` | professor dono | carregar rascunho completo |
| `PUT /trilhas/{id}` | professor dono | salvar árvore completa |
| `POST /trilhas/{id}/publicacoes` | professor dono | publicar snapshot |
| `POST /distribuicoes` | professor vinculado | distribuir versão |
| `GET /distribuicoes?turmaId={id}` | professor vinculado e criador | listar distribuições da turma |
| `GET /aluno/distribuicoes` | aluno matriculado | listar trilhas disponíveis |
| `GET /aluno/distribuicoes/{id}/caminho` | aluno matriculado | listar módulos e status das lições |
| `POST /aluno/distribuicoes/{id}/sessoes` | aluno matriculado | iniciar/retomar sessão |
| `GET /aluno/sessoes/{id}` | aluno dono | consultar sessão |
| `POST /aluno/sessoes/{id}/respostas` | aluno dono | responder desafio |
| `GET /professor/turmas/{id}/indicadores` | professor vinculado | acompanhar turma |

### Login

```json
{
  "login": "professor",
  "senha": "prof123"
}
```

```json
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

### Rascunho básico

```json
{
  "titulo": "Fundamentos da lógica",
  "descricao": "Trilha piloto",
  "disciplinaId": 1
}
```

### Árvore completa do rascunho

```json
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

### Publicação e distribuição

```json
{
  "trilhaId": 1,
  "versaoId": 1,
  "numeroVersao": 1,
  "publicadaEm": "2026-09-14T18:00:00"
}
```

```json
{
  "versaoId": 1,
  "turmaId": 1,
  "disponivelDe": "2026-09-15T00:00:00",
  "disponivelAte": null
}
```

### Resposta do aluno

```json
{
  "desafioId": 1,
  "opcaoId": 2
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
    "opcoes": [{"id": 3, "texto": "Opção"}]
  }
}
```

Erros usam `application/problem+json` com `title`, `status`, `detail`, `instance` e `errors` por campo quando houver validação.

## 9. Calendário dos incrementos

| Período | Incremento obrigatório |
|---|---|
| 09/09 | 1.1 — projetos executando e conectados pelo health |
| 10–11/09 | 1.2 e 1.3 — login, perfil e navegação reais |
| 12–14/09 | 2.1 a 2.5 — professor cria e publica V1 |
| 15–16/09 | 3.1 e 3.2 — distribuição e catálogo do aluno |
| 17–19/09 | 4.1 a 4.3 — prática, feedback, retomada e conclusão |
| 20–21/09 | 5.1 e 5.2 — acompanhamento e histórico de versões |
| 22/09 | 6.1 — regressão completa |
| 23/09 | 6.2 — builds e correções bloqueadoras |
| 24/09 | apresentação; nenhuma funcionalidade nova |

## 10. Etapas de implementação

### Etapa 1 — Base, login e perfis

#### 1.1 — Criar o backend

**Responsável:** B1. **Par:** F1.

- Criar projeto Maven com os starters definidos na seção 5.
- Criar os pacotes por camada e os quatro arquivos de configuração de perfil.
- Configurar bancos, Flyway, CORS, Swagger, health e auditoria.
- Criar teste `contextLoads` e teste do health público.

Testar: `mvn test` sem PostgreSQL e execução local com PostgreSQL.

Aceite integrado: frontend 1.1 consulta o health real sem erro de CORS.

#### 1.2 — Usuário e autenticação

**Responsável:** B1. **Par:** F1.

- Implementar V1, entidade, enum, repository e contratos de login.
- Implementar JWT, filtro, configuração stateless e BCrypt.
- Criar seed idempotente em `dev`: `admin/admin123`, `professor/prof123` e `aluno/aluno123`.

Testar: login válido por perfil, senha inválida, token ausente/expirado e seed sem duplicação.

Aceite integrado: frontend 1.2 autentica professor e aluno e usa o token em chamada protegida.

#### 1.3 — Perfil corrente e gestão mínima

**Responsável:** B1. **Par:** F1.

- Implementar perfil corrente, cadastro e listagem por perfil.
- Restringir gestão a `ADMINISTRADOR`.
- Finalizar tratamento RFC 7807 e DTOs sem senha.

Testar: perfil corrente, duplicidade, filtro e acesso negado.

Aceite da Etapa 1: login, perfil e logout funcionam no frontend real; regressão de 1.1 e 1.2.

### Etapa 2 — Professor cria e publica

#### 2.1 — Estrutura acadêmica

**Responsável:** B2. **Par:** F2.

- Implementar V2, entidades, repositories e `/professor/contexto`.
- Semear disciplina, turma, vínculo do professor e matrícula do aluno.

Testar: contexto permitido, aluno proibido e professor sem vínculo.

Aceite integrado: home do professor mostra disciplina e turma reais.

#### 2.2 — Criar rascunho básico

**Responsável:** B2. **Par:** F2.

- Aplicar a migration V3 completa, criando desde já todas as tabelas do conteúdo em rascunho; não editar essa migration nas etapas seguintes.
- Implementar inicialmente `Trilha`, `StatusTrilha`, DTOs, repository, service e `POST /trilhas`.
- Associar automaticamente professor autenticado e estado `RASCUNHO`.
- Validar título, disciplina e vínculo.

Aceite integrado: formulário cria o rascunho e exibe ID/status retornado.

#### 2.3 — Salvar a árvore completa

**Responsável:** B2. **Par:** F2.

- Implementar as entidades JPA de módulo, lição, desafio e opção sobre as tabelas já criadas pela V3.
- Implementar `PUT /trilhas/{id}` em transação.
- Validar árvore, ordens, tipo e exatamente uma opção correta.

Testar: árvore válida, rollback, ordem repetida e opções inválidas.

Aceite integrado: frontend salva a árvore e a consulta devolve a mesma estrutura.

#### 2.4 — Consultar e editar o rascunho

**Responsável:** B2. **Par:** F2.

- Implementar `GET /trilhas/{id}` com árvore ordenada.
- Finalizar ownership de GET/PUT e bloqueio de conteúdo arquivado.

Testar: carregar/editar, outro professor, ID inexistente e ordenação.

Aceite integrado: professor fecha, reabre e altera o rascunho.

#### 2.5 — Publicar versão imutável

**Responsável:** B2. **Par:** F2.

- Implementar V4, snapshots e publicação transacional.
- Copiar a árvore, incrementar versão e preservar versões anteriores.

Testar: V1, V2, snapshot preservado e rascunho inválido.

Aceite da Etapa 2: professor publica V1; editar rascunho não altera V1; Etapa 1 continua funcionando.

### Etapa 3 — Professor distribui e aluno visualiza

#### 3.1 — Distribuir versão para turma

**Responsável:** B2. **Par:** F2.

- Implementar V5, `Distribuicao` e `POST /distribuicoes`.
- Validar publicação, vínculo, disciplina, período e duplicidade.

Aceite integrado: frontend distribui V1 sem informar IDs manualmente.

#### 3.2 — Listar distribuições do aluno

**Responsável:** B2, revisão B3. **Par:** F3.

- Implementar catálogo filtrado por matrícula e período.
- Retornar resumo, totais, progresso, `sessaoId`, datas e `prazoEncerrado`.
- Implementar o caminho ordenado com lições `CONCLUIDA`, `ATUAL` ou `BLOQUEADA`.
- Garantir ausência de resposta correta no JSON.

Aceite da Etapa 3: aluno vê a trilha distribuída; Etapas 1 e 2 continuam funcionando.

### Etapa 4 — Aluno pratica

#### 4.1 — Iniciar ou retomar sessão

**Responsável:** B3. **Par:** F3.

- Aplicar a migration V6 completa, criando as tabelas de sessão e tentativa; não editar essa migration na etapa 4.2.
- Implementar inicialmente a entidade de sessão e o endpoint de início/retomada.
- Garantir uma sessão por aluno/distribuição.
- Retornar desafio atual sem resposta correta.
- Permitir continuação de sessão existente depois do prazo e retornar `409` para nova sessão vencida.
- Bloquear consulta e resposta de sessão incompleta quando `ativo=false`, preservando histórico concluído.

Aceite integrado: “Começar” abre o desafio e uma nova abertura retoma a sessão.

#### 4.2 — Registrar e corrigir resposta

**Responsável:** B3. **Par:** F3.

- Implementar a entidade `Tentativa` sobre a tabela já criada pela V6 e o endpoint de resposta.
- Validar sessão, desafio e opção.
- Registrar acerto/erro, produzir feedback e controlar avanço.

Testar: erro, acerto, opção/desafio alheio e sessão concluída.

Aceite integrado: frontend permite tentar novamente após erro e avança após acerto.

#### 4.3 — Retomar e concluir

**Responsável:** B3. **Par:** F3.

- Implementar consulta da sessão, cálculo do progresso e conclusão.
- Refletir progresso no catálogo após novo login.

Testar: sequência, último desafio, 100% e consulta posterior.

Aceite da Etapa 4: aluno erra, acerta, sai, retorna e conclui; Etapas 1 a 3 passam novamente.

### Etapa 5 — Professor acompanha

#### 5.1 — Calcular indicadores

**Responsável:** B3. **Par:** F3.

- Implementar totais, conclusão, acurácia e ranking de dificuldades.
- Filtrar por turma/vínculo e evitar N+1.
- Tratar divisão por zero.

Aceite integrado: tentativas reais alteram o painel do professor.

#### 5.2 — Proteger histórico e versões

**Responsável:** B2, revisão B1/B3. **Par:** F2.

- Automatizar o ciclo V1 distribuída/respondida seguido de edição e publicação V2.
- Confirmar histórico V1 intacto.
- Completar testes de `401`, `403`, `404` e `409`.

Aceite da Etapa 5: V2 não altera V1; todo o ciclo anterior continua funcional.

### Etapa 6 — Estabilização

#### 6.1 — Teste completo do ciclo

**Responsável:** B1 com todos.

- Criar teste integrado do caminho feliz.
- Validar migrations em PostgreSQL vazio.
- Executar o roteiro duas vezes com frontend real, sem mocks e sem limpar o banco.
- Corrigir somente falhas do ciclo principal.

#### 6.2 — Preparação do build

**Responsável:** B1.

- Confirmar execução pelo JAR e configuração de CORS/base URL.
- Documentar apenas comandos essenciais para iniciar banco e backend.
- Congelar contratos e migrations.

Aceite final: backend limpo suporta todo o roteiro do professor e do aluno.

## 11. Definição de pronto de qualquer item

- Compila em Java 21 e `mvn test` passa.
- Migration sobe em banco vazio sem editar migrations anteriores.
- Sucesso, validação e permissão possuem testes.
- Endpoint aparece no Swagger.
- DTO não vaza senha, hash ou resposta correta antecipada.
- Frontend correspondente usa a API real.
- Regressão das etapas anteriores foi executada.

## 12. Instrução para delegar um item a uma IA

```text
Implemente somente o item [número e título] deste plano backend.
Leia as seções de estrutura, convenções, migrations, contratos e definição de pronto.
Primeiro inspecione o projeto. Depois implemente migration, entidade, DTO, repository,
service, controller, segurança e testes necessários ao item. Preserve funcionalidades
anteriores, não altere o contrato sem informar e não amplie o escopo. Execute os testes
e relate arquivos alterados, comandos, resultados e o aceite integrado ainda pendente.
```
