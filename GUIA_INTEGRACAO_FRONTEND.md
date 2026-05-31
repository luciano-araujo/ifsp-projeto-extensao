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

## 3. USUARIO (rotas protegidas)

### 3.1 Buscar usuario por ID

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

### 3.2 Atualizar usuario

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

### 3.3 Deletar usuario

```
DELETE /api/v1/users/{id}
```

**Header:** `Authorization: Bearer <token>`

**O que recebe de volta:** `204 No Content` (sem corpo).

---

## 4. ERROS

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

## 5. RESUMO RAPIDO DE TODAS AS ROTAS

| Metodo | Rota | Protegida? | O que faz |
|--------|------|------------|-----------|
| POST | `/api/auth/register/request-token` | Nao | Envia codigo de verificacao por e-mail |
| POST | `/api/auth/register/confirm` | Nao | Confirma cadastro com o codigo e cria a conta |
| POST | `/api/auth/login` | Nao | Faz login e retorna o token JWT |
| POST | `/api/v1/kanban` | Sim | Cria uma nova ideia |
| GET | `/api/v1/kanban` | Sim | Lista todos os itens (aceita ?state=) |
| GET | `/api/v1/kanban/{id}` | Sim | Busca um item especifico |
| PUT | `/api/v1/kanban/{id}` | Sim | Atualiza um item |
| POST | `/api/v1/kanban/{id}/convert` | Sim | Converte ideia em projeto |
| DELETE | `/api/v1/kanban/{id}` | Sim | Deleta um item |
| GET | `/api/v1/users/{id}` | Sim | Busca dados de um usuario |
| PUT | `/api/v1/users/{id}` | Sim | Atualiza dados do usuario |
| DELETE | `/api/v1/users/{id}` | Sim | Deleta o usuario |

---

## 6. EXEMPLO DE FLUXO COMPLETO

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

---

## 7. CONFIGURACAO DO AXIOS/FETCH NO FRONTEND

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
```

---

## 8. SWAGGER (DOCUMENTACAO INTERATIVA)

Quando o backend estiver rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

La voce pode ver todas as rotas e testar elas direto no navegador.
