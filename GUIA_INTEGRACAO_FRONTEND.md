# Guia de Integracao - Backend Creator Workbench

Este documento explica tudo que o frontend precisa saber para se comunicar com o backend.
Todas as respostas e requisicoes usam JSON. O backend roda em `http://localhost:8080`.

---

## Como funciona a autenticacao

O backend usa JWT (JSON Web Token). O fluxo e:

1. O usuario faz login ou se cadastra
2. O backend retorna um `token` (uma string longa)
3. O frontend salva esse token (cookie, localStorage, etc.)
4. Em TODA requisicao para rotas protegidas, o frontend envia o token no header:

```
Authorization: Bearer <token>
```

Rotas que comecam com `/api/auth/` sao publicas (nao precisam de token).
Todas as outras rotas precisam do token. Sem ele, o backend retorna `401`.

---

## 1. AUTENTICACAO (rotas publicas)

### 1.1 Solicitar codigo de verificacao para cadastro

O primeiro passo do cadastro. O backend envia um codigo de 6 digitos para o e-mail do usuario.

```
POST /api/auth/register/request-token
```

**O que enviar:**
```json
{
  "name": "Henrique",
  "email": "henrique@email.com"
}
```

**O que recebe de volta (200 OK):**
```json
{
  "message": "Codigo de verificacao enviado para henrique@email.com"
}
```

**Erros possiveis:**
- `400` - E-mail ja cadastrado: `{"message": "Este e-mail ja esta registrado no sistema."}`
- `400` - Campos vazios: `{"message": "must not be blank"}`

---

### 1.2 Confirmar cadastro com o codigo

O usuario digita o codigo de 6 digitos que recebeu no e-mail, junto com a senha desejada.

```
POST /api/auth/register/confirm
```

**O que enviar:**
```json
{
  "email": "henrique@email.com",
  "token": "482917",
  "password": "minhaSenha123"
}
```

**O que recebe de volta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": 1,
  "name": "Henrique",
  "email": "henrique@email.com"
}
```

O `token` retornado ja e o JWT. O usuario ja esta logado apos o cadastro.

**Erros possiveis:**
- `400` - Codigo errado: `{"message": "Codigo de verificacao invalido."}`
- `400` - Codigo expirado (expira em 10 minutos): `{"message": "Codigo de verificacao expirado. Solicite um novo."}`
- `400` - Senha com menos de 6 caracteres

---

### 1.3 Login

```
POST /api/auth/login
```

**O que enviar:**
```json
{
  "email": "henrique@email.com",
  "password": "minhaSenha123"
}
```

**O que recebe de volta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "userId": 1,
  "name": "Henrique",
  "email": "henrique@email.com"
}
```

Salve o `token` para usar nas proximas requisicoes.

**Erros possiveis:**
- `400` - E-mail ou senha errados: `{"message": "Credenciais invalidas."}`

---

## 2. KANBAN - Ideias e Projetos (rotas protegidas)

Todas estas rotas precisam do header `Authorization: Bearer <token>`.
O usuario so ve e manipula os itens dele. Nao e possivel acessar itens de outro usuario.

### Estados do Kanban

Cada item tem um `state` que representa onde ele esta no fluxo:

| State | Significado |
|-------|-------------|
| `IDEATION` | Ideia inicial, ainda nao virou projeto |
| `IN_PRODUCTION` | Projeto em producao ativa |
| `REVIEW` | Em revisao |
| `DONE` | Finalizado |

O fluxo normal e: `IDEATION` -> `IN_PRODUCTION` -> `REVIEW` -> `DONE`

---

### 2.1 Criar uma ideia

Cria um item novo no estado `IDEATION` com progresso 0.

```
POST /api/v1/kanban
```

**Header:** `Authorization: Bearer <token>`

**O que enviar:**
```json
{
  "title": "Aula sobre ponteiros em C",
  "description": "Criar video explicando ponteiros para iniciantes"
}
```

- `title` e obrigatorio (minimo 3 caracteres)
- `description` e opcional

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "title": "Aula sobre ponteiros em C",
  "description": "Criar video explicando ponteiros para iniciantes",
  "state": "IDEATION",
  "targetAudience": null,
  "pedagogicalObjective": null,
  "progress": 0,
  "userId": 1,
  "createdAt": "2026-05-31T18:30:00",
  "updatedAt": "2026-05-31T18:30:00"
}
```

---

### 2.2 Listar itens do usuario

Retorna todos os itens do usuario logado. Pode filtrar por estado.

```
GET /api/v1/kanban
```

**Header:** `Authorization: Bearer <token>`

**Sem filtro (todos os itens):**
```
GET /api/v1/kanban
```

**Filtrar por estado:**
```
GET /api/v1/kanban?state=IDEATION
GET /api/v1/kanban?state=IN_PRODUCTION
GET /api/v1/kanban?state=REVIEW
GET /api/v1/kanban?state=DONE
```

**O que recebe de volta (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Aula sobre ponteiros em C",
    "description": "Criar video explicando ponteiros para iniciantes",
    "state": "IDEATION",
    "targetAudience": null,
    "pedagogicalObjective": null,
    "progress": 0,
    "userId": 1,
    "createdAt": "2026-05-31T18:30:00",
    "updatedAt": "2026-05-31T18:30:00"
  },
  {
    "id": 2,
    "title": "Curso de Java",
    "description": null,
    "state": "IN_PRODUCTION",
    "targetAudience": "Iniciantes em programacao",
    "pedagogicalObjective": "Ensinar fundamentos da linguagem Java",
    "progress": 45,
    "userId": 1,
    "createdAt": "2026-05-30T10:00:00",
    "updatedAt": "2026-05-31T14:00:00"
  }
]
```

Se o usuario nao tem itens, retorna um array vazio: `[]`

---

### 2.3 Buscar um item especifico

```
GET /api/v1/kanban/{id}
```

**Header:** `Authorization: Bearer <token>`

**Exemplo:**
```
GET /api/v1/kanban/1
```

**O que recebe de volta (200 OK):** o objeto do item (mesmo formato da listagem).

**Erro:** `404` se o item nao existe ou nao pertence ao usuario.

---

### 2.4 Converter ideia em projeto

Transforma um item que esta em `IDEATION` em um projeto (`IN_PRODUCTION`).
Adiciona informacoes de publico-alvo e objetivo pedagogico.

So funciona se o item estiver no estado `IDEATION`.

```
POST /api/v1/kanban/{id}/convert
```

**Header:** `Authorization: Bearer <token>`

**O que enviar:**
```json
{
  "title": "Curso Completo de Ponteiros em C",
  "targetAudience": "Estudantes de Ciencia da Computacao",
  "pedagogicalObjective": "Ensinar o conceito de ponteiros, alocacao de memoria e manipulacao de enderecos"
}
```

- `title` e obrigatorio (minimo 3 caracteres)
- `targetAudience` e obrigatorio (minimo 3 caracteres)
- `pedagogicalObjective` e obrigatorio (minimo 10 caracteres)

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "title": "Curso Completo de Ponteiros em C",
  "description": "Criar video explicando ponteiros para iniciantes",
  "state": "IN_PRODUCTION",
  "targetAudience": "Estudantes de Ciencia da Computacao",
  "pedagogicalObjective": "Ensinar o conceito de ponteiros, alocacao de memoria e manipulacao de enderecos",
  "progress": 0,
  "userId": 1,
  "createdAt": "2026-05-31T18:30:00",
  "updatedAt": "2026-05-31T19:00:00"
}
```

**Erros possiveis:**
- `400` - Item nao esta em IDEATION: `{"message": "Somente itens em fase de ideacao podem ser convertidos em projeto."}`
- `400` - Item nao encontrado: `{"message": "Item nao encontrado."}`

---

### 2.5 Atualizar um item

Atualiza qualquer campo de um item. Envie somente os campos que quer mudar.

```
PUT /api/v1/kanban/{id}
```

**Header:** `Authorization: Bearer <token>`

**Exemplos do que enviar:**

Atualizar apenas o progresso:
```json
{
  "progress": 75
}
```

Mudar o estado para REVIEW:
```json
{
  "state": "REVIEW"
}
```

Atualizar varios campos de uma vez:
```json
{
  "title": "Novo titulo",
  "description": "Nova descricao",
  "state": "IN_PRODUCTION",
  "targetAudience": "Desenvolvedores junior",
  "pedagogicalObjective": "Aprender boas praticas de codigo limpo",
  "progress": 30
}
```

Todos os campos sao opcionais. Campos nao enviados nao sao alterados.

- `progress` deve ser entre 0 e 100

**O que recebe de volta (200 OK):** o objeto atualizado (mesmo formato).

**Erro:** `400` - Item nao encontrado: `{"message": "Item nao encontrado."}`

---

### 2.6 Deletar um item

```
DELETE /api/v1/kanban/{id}
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta:** `204 No Content` (sem corpo na resposta).

**Erro:** `400` - Item nao encontrado: `{"message": "Item nao encontrado."}`

---

## 3. EDITOR DE ROTEIRO / SCRIPT (rotas protegidas)

Cada item do Kanban pode ter um roteiro (script) vinculado. O conteudo e HTML (vindo do editor TipTap do frontend).
O backend salva o conteudo atual e permite criar versoes (snapshots) para historico.

Todas estas rotas precisam do header `Authorization: Bearer <token>`.

### 3.1 Salvar conteudo do roteiro

Cria o script na primeira vez, ou atualiza o conteudo existente.

```
PUT /api/v1/kanban/{kanbanItemId}/script
```

**Header:** `Authorization: Bearer <token>`

**O que enviar:**
```json
{
  "content": "<h1>Introducao</h1><p>Nesta aula vamos aprender sobre...</p>"
}
```

O `content` e o HTML completo que vem do `editor.getHTML()` do TipTap.

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "kanbanItemId": 1,
  "content": "<h1>Introducao</h1><p>Nesta aula vamos aprender sobre...</p>",
  "createdAt": "2026-05-31T19:00:00",
  "updatedAt": "2026-05-31T19:05:00"
}
```

---

### 3.2 Buscar conteudo do roteiro

```
GET /api/v1/kanban/{kanbanItemId}/script
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta (200 OK):** o objeto do script (mesmo formato acima).

Use o campo `content` para carregar no editor TipTap com `editor.commands.setContent(content)`.

**Erro:** `400` se o item nao tem script ainda.

---

### 3.3 Criar snapshot (versao)

Tira uma "foto" do conteudo atual do roteiro. Util para o botao "Salvar versao" do editor.

```
POST /api/v1/kanban/{kanbanItemId}/script/versions
```

**Header:** `Authorization: Bearer <token>`

**Nao precisa enviar body.** O backend copia automaticamente o conteudo atual do script.

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "scriptId": 1,
  "content": "<h1>Introducao</h1><p>Nesta aula vamos aprender sobre...</p>",
  "createdAt": "2026-05-31T19:10:00"
}
```

**Erro:** `400` se o script ainda nao foi salvo (precisa salvar o conteudo antes de criar versao).

---

### 3.4 Listar historico de versoes

Retorna todas as versoes salvas, da mais recente para a mais antiga.

```
GET /api/v1/kanban/{kanbanItemId}/script/versions
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta (200 OK):**
```json
[
  {
    "id": 3,
    "scriptId": 1,
    "content": "<h1>Versao 3</h1><p>Conteudo mais recente...</p>",
    "createdAt": "2026-05-31T20:00:00"
  },
  {
    "id": 2,
    "scriptId": 1,
    "content": "<h1>Versao 2</h1><p>Conteudo anterior...</p>",
    "createdAt": "2026-05-31T19:30:00"
  },
  {
    "id": 1,
    "scriptId": 1,
    "content": "<h1>Versao 1</h1><p>Primeiro rascunho...</p>",
    "createdAt": "2026-05-31T19:10:00"
  }
]
```

Use o `createdAt` para mostrar a data/hora de cada versao na sidebar do editor.

---

### 3.5 Restaurar uma versao anterior

Substitui o conteudo atual do roteiro pelo conteudo de uma versao anterior.

```
POST /api/v1/kanban/{kanbanItemId}/script/versions/{versionId}/restore
```

**Header:** `Authorization: Bearer <token>`

**Nao precisa enviar body.**

**O que recebe de volta (200 OK):** o objeto do script com o conteudo restaurado.

**Dica:** Antes de restaurar, crie um snapshot da versao atual para nao perder o trabalho.

---

## 4. CURRICULUM / GRADE CURRICULAR (rotas protegidas)

Cada item do Kanban (projeto) pode ter uma grade curricular com modulos e aulas.
A estrutura e hierarquica: Projeto > Modulos > Aulas.

Todas estas rotas precisam do header `Authorization: Bearer <token>`.

### Tipos de aula

| Tipo | Significado |
|------|-------------|
| `VIDEO` | Aula em video |
| `TEXT` | Aula em texto |
| `QUIZ` | Questionario/quiz |

---

### 4.1 Criar um modulo

```
POST /api/v1/kanban/{kanbanItemId}/curriculum/modules
```

**Header:** `Authorization: Bearer <token>`

**O que enviar:**
```json
{
  "title": "01_INTRODUCAO_AO_CONCEITO"
}
```

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "kanbanItemId": 1,
  "title": "01_INTRODUCAO_AO_CONCEITO",
  "sortOrder": 0,
  "lessons": [],
  "createdAt": "2026-05-31T19:00:00"
}
```

O `sortOrder` e definido automaticamente (proximo numero disponivel).

---

### 4.2 Listar modulos de um projeto

Retorna todos os modulos com suas aulas, ordenados por `sortOrder`.

```
GET /api/v1/kanban/{kanbanItemId}/curriculum/modules
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta (200 OK):**
```json
[
  {
    "id": 1,
    "kanbanItemId": 1,
    "title": "01_INTRODUCAO_AO_CONCEITO",
    "sortOrder": 0,
    "lessons": [
      {
        "id": 1,
        "moduleId": 1,
        "title": "Visao Geral e Mindset",
        "type": "VIDEO",
        "published": true,
        "sortOrder": 0,
        "createdAt": "2026-05-31T19:05:00"
      },
      {
        "id": 2,
        "moduleId": 1,
        "title": "Quiz de Conceitos Basicos",
        "type": "QUIZ",
        "published": false,
        "sortOrder": 1,
        "createdAt": "2026-05-31T19:10:00"
      }
    ],
    "createdAt": "2026-05-31T19:00:00"
  },
  {
    "id": 2,
    "kanbanItemId": 1,
    "title": "02_FUNDAMENTOS_PRATICOS",
    "sortOrder": 1,
    "lessons": [],
    "createdAt": "2026-05-31T19:15:00"
  }
]
```

---

### 4.3 Atualizar um modulo

```
PUT /api/v1/kanban/{kanbanItemId}/curriculum/modules/{moduleId}
```

**Header:** `Authorization: Bearer <token>`

**O que enviar (todos os campos sao opcionais):**
```json
{
  "title": "01_NOVO_TITULO",
  "sortOrder": 2
}
```

**O que recebe de volta (200 OK):** o objeto do modulo atualizado (com suas aulas).

---

### 4.4 Deletar um modulo

Deleta o modulo e TODAS as aulas dentro dele.

```
DELETE /api/v1/kanban/{kanbanItemId}/curriculum/modules/{moduleId}
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta:** `204 No Content` (sem corpo).

---

### 4.5 Criar uma aula dentro de um modulo

```
POST /api/v1/kanban/{kanbanItemId}/curriculum/modules/{moduleId}/lessons
```

**Header:** `Authorization: Bearer <token>`

**O que enviar:**
```json
{
  "title": "Visao Geral e Mindset",
  "type": "VIDEO"
}
```

- `title` e obrigatorio
- `type` e opcional (padrao: `VIDEO`). Valores aceitos: `VIDEO`, `TEXT`, `QUIZ`

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "moduleId": 1,
  "title": "Visao Geral e Mindset",
  "type": "VIDEO",
  "published": false,
  "sortOrder": 0,
  "createdAt": "2026-05-31T19:05:00"
}
```

---

### 4.6 Atualizar uma aula

```
PUT /api/v1/kanban/{kanbanItemId}/curriculum/modules/{moduleId}/lessons/{lessonId}
```

**Header:** `Authorization: Bearer <token>`

**O que enviar (todos os campos sao opcionais):**
```json
{
  "title": "Novo titulo da aula",
  "type": "TEXT",
  "published": true,
  "sortOrder": 3
}
```

**O que recebe de volta (200 OK):** o objeto da aula atualizado.

---

### 4.7 Deletar uma aula

```
DELETE /api/v1/kanban/{kanbanItemId}/curriculum/modules/{moduleId}/lessons/{lessonId}
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta:** `204 No Content` (sem corpo).

---

## 5. USUARIO (rotas protegidas)

### 5.1 Buscar usuario por ID

```
GET /api/v1/users/{id}
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta (200 OK):**
```json
{
  "id": 1,
  "name": "Henrique",
  "email": "henrique@email.com",
  "createdAt": "2026-05-31T18:00:00"
}
```

A senha nunca e retornada nas respostas.

**Erro:** `404` se o usuario nao existe.

---

### 5.2 Atualizar usuario

```
PUT /api/v1/users/{id}
```

**Header:** `Authorization: Bearer <token>`

**O que enviar (todos os campos sao opcionais):**
```json
{
  "name": "Henrique Orsoni",
  "email": "novo@email.com",
  "password": "novaSenha456"
}
```

Envie somente o que quer alterar.

**O que recebe de volta (200 OK):** o objeto do usuario atualizado.

---

### 5.3 Deletar usuario

```
DELETE /api/v1/users/{id}
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta:** `204 No Content` (sem corpo).

---

## 6. ERROS

Todos os erros seguem o mesmo formato:

```json
{
  "message": "Descricao do erro aqui"
}
```

| Codigo HTTP | Significado |
|-------------|-------------|
| `400` | Dados invalidos ou regra de negocio violada |
| `401` | Token ausente, expirado ou invalido |
| `403` | Acesso negado (sem permissao) |
| `404` | Recurso nao encontrado |

---

## 7. RESUMO RAPIDO DE TODAS AS ROTAS

### Autenticacao (publicas)

| Metodo | Rota | O que faz |
|--------|------|-----------|
| POST | `/api/auth/register/request-token` | Envia codigo de verificacao por e-mail |
| POST | `/api/auth/register/confirm` | Confirma cadastro com o codigo e cria a conta |
| POST | `/api/auth/login` | Faz login e retorna o token JWT |

### Kanban (protegidas)

| Metodo | Rota | O que faz |
|--------|------|-----------|
| POST | `/api/v1/kanban` | Cria uma nova ideia |
| GET | `/api/v1/kanban` | Lista todos os itens (aceita ?state=) |
| GET | `/api/v1/kanban/{id}` | Busca um item especifico |
| PUT | `/api/v1/kanban/{id}` | Atualiza um item |
| POST | `/api/v1/kanban/{id}/convert` | Converte ideia em projeto |
| DELETE | `/api/v1/kanban/{id}` | Deleta um item |

### Editor de Roteiro (protegidas)

| Metodo | Rota | O que faz |
|--------|------|-----------|
| PUT | `/api/v1/kanban/{id}/script` | Salva conteudo do roteiro (HTML) |
| GET | `/api/v1/kanban/{id}/script` | Busca conteudo do roteiro |
| POST | `/api/v1/kanban/{id}/script/versions` | Cria snapshot da versao atual |
| GET | `/api/v1/kanban/{id}/script/versions` | Lista historico de versoes |
| POST | `/api/v1/kanban/{id}/script/versions/{versionId}/restore` | Restaura uma versao anterior |

### Grade Curricular (protegidas)

| Metodo | Rota | O que faz |
|--------|------|-----------|
| POST | `/api/v1/kanban/{id}/curriculum/modules` | Cria um modulo |
| GET | `/api/v1/kanban/{id}/curriculum/modules` | Lista modulos com aulas |
| PUT | `/api/v1/kanban/{id}/curriculum/modules/{moduleId}` | Atualiza modulo |
| DELETE | `/api/v1/kanban/{id}/curriculum/modules/{moduleId}` | Deleta modulo e suas aulas |
| POST | `.../modules/{moduleId}/lessons` | Cria uma aula |
| PUT | `.../modules/{moduleId}/lessons/{lessonId}` | Atualiza uma aula |
| DELETE | `.../modules/{moduleId}/lessons/{lessonId}` | Deleta uma aula |

### Usuario (protegidas)

| Metodo | Rota | O que faz |
|--------|------|-----------|
| GET | `/api/v1/users/{id}` | Busca dados de um usuario |
| PUT | `/api/v1/users/{id}` | Atualiza dados do usuario |
| DELETE | `/api/v1/users/{id}` | Deleta o usuario |

---

## 8. EXEMPLO DE FLUXO COMPLETO

### Cadastro + Criar ideia + Converter em projeto

```
1. POST /api/auth/register/request-token
   Body: { "name": "Henrique", "email": "henrique@email.com" }
   -> Recebe: { "message": "Codigo de verificacao enviado..." }

2. (usuario olha o e-mail e pega o codigo)

3. POST /api/auth/register/confirm
   Body: { "email": "henrique@email.com", "token": "482917", "password": "minhaSenha" }
   -> Recebe: { "token": "eyJ...", "userId": 1, "name": "Henrique", ... }
   -> SALVAR o token!

4. POST /api/v1/kanban
   Header: Authorization: Bearer eyJ...
   Body: { "title": "Video sobre APIs REST", "description": "Explicar GET, POST, PUT, DELETE" }
   -> Recebe: item com id=1, state="IDEATION"

5. POST /api/v1/kanban/1/convert
   Header: Authorization: Bearer eyJ...
   Body: { "title": "Curso APIs REST", "targetAudience": "Devs junior", "pedagogicalObjective": "Ensinar consumo e criacao de APIs RESTful" }
   -> Recebe: item com state="IN_PRODUCTION"

6. PUT /api/v1/kanban/1
   Header: Authorization: Bearer eyJ...
   Body: { "progress": 50 }
   -> Recebe: item com progress=50

7. PUT /api/v1/kanban/1
   Header: Authorization: Bearer eyJ...
   Body: { "state": "REVIEW", "progress": 100 }
   -> Recebe: item com state="REVIEW", progress=100

8. PUT /api/v1/kanban/1
   Header: Authorization: Bearer eyJ...
   Body: { "state": "DONE" }
   -> Recebe: item com state="DONE"
```

### Escrevendo o roteiro de um projeto

```
1. PUT /api/v1/kanban/1/script
   Header: Authorization: Bearer eyJ...
   Body: { "content": "<h1>Aula 1</h1><p>Primeiro rascunho do roteiro...</p>" }
   -> Recebe: script salvo com id=1

2. POST /api/v1/kanban/1/script/versions
   Header: Authorization: Bearer eyJ...
   (sem body)
   -> Recebe: snapshot versao id=1 criado

3. PUT /api/v1/kanban/1/script
   Header: Authorization: Bearer eyJ...
   Body: { "content": "<h1>Aula 1</h1><p>Versao revisada do roteiro...</p>" }
   -> Recebe: script atualizado

4. GET /api/v1/kanban/1/script/versions
   Header: Authorization: Bearer eyJ...
   -> Recebe: lista de versoes [{ id: 1, content: "...", createdAt: "..." }]

5. POST /api/v1/kanban/1/script/versions/1/restore
   Header: Authorization: Bearer eyJ...
   (sem body)
   -> Recebe: script com conteudo restaurado para a versao 1
```

### Montando a grade curricular de um projeto

```
1. POST /api/v1/kanban/1/curriculum/modules
   Header: Authorization: Bearer eyJ...
   Body: { "title": "01_INTRODUCAO" }
   -> Recebe: modulo com id=1

2. POST /api/v1/kanban/1/curriculum/modules/1/lessons
   Header: Authorization: Bearer eyJ...
   Body: { "title": "Visao Geral", "type": "VIDEO" }
   -> Recebe: aula com id=1, published=false

3. POST /api/v1/kanban/1/curriculum/modules/1/lessons
   Header: Authorization: Bearer eyJ...
   Body: { "title": "Quiz de Conceitos", "type": "QUIZ" }
   -> Recebe: aula com id=2

4. PUT /api/v1/kanban/1/curriculum/modules/1/lessons/1
   Header: Authorization: Bearer eyJ...
   Body: { "published": true }
   -> Recebe: aula com published=true

5. GET /api/v1/kanban/1/curriculum/modules
   Header: Authorization: Bearer eyJ...
   -> Recebe: lista de modulos com suas aulas dentro
```

---

## 9. CONFIGURACAO DO AXIOS/FETCH NO FRONTEND

Exemplo de como configurar um cliente HTTP:

```typescript
const API_BASE_URL = "http://localhost:8080";

// Funcao para fazer requisicoes autenticadas
async function apiRequest(method: string, path: string, body?: any) {
  const token = getCookie("creator_auth_token"); // ou localStorage

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { "Authorization": `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401) {
    // Token expirou ou invalido - redirecionar para login
    window.location.href = "/login";
    return;
  }

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  if (response.status === 204) return null; // DELETE retorna sem corpo
  return response.json();
}

// Exemplos de uso:
// await apiRequest("POST", "/api/auth/login", { email, password });
// await apiRequest("GET", "/api/v1/kanban");
// await apiRequest("POST", "/api/v1/kanban", { title: "Minha ideia" });
// await apiRequest("PUT", "/api/v1/kanban/1", { progress: 50 });
// await apiRequest("DELETE", "/api/v1/kanban/1");
//
// Roteiro:
// await apiRequest("PUT", "/api/v1/kanban/1/script", { content: editor.getHTML() });
// await apiRequest("GET", "/api/v1/kanban/1/script");
// await apiRequest("POST", "/api/v1/kanban/1/script/versions");
// await apiRequest("GET", "/api/v1/kanban/1/script/versions");
// await apiRequest("POST", "/api/v1/kanban/1/script/versions/1/restore");
//
// Curriculum:
// await apiRequest("POST", "/api/v1/kanban/1/curriculum/modules", { title: "Modulo 1" });
// await apiRequest("GET", "/api/v1/kanban/1/curriculum/modules");
// await apiRequest("POST", "/api/v1/kanban/1/curriculum/modules/1/lessons", { title: "Aula 1", type: "VIDEO" });
```

---

## 10. SWAGGER (DOCUMENTACAO INTERATIVA)

Quando o backend estiver rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

La voce pode ver todas as rotas e testar elas direto no navegador.
