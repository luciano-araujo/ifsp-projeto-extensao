$ErrorActionPreference = "Stop"
$api = $args[0]   # ex.: http://<DNS do ELB>
$demoPass = $args[1]   # senha dos usuarios de demonstracao

function Call($method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ Method = $method; Uri = "$api$path"; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $body) {
        # Send UTF-8 bytes so accents arrive intact
        $params["Body"] = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 10))
        $params["ContentType"] = "application/json; charset=utf-8"
    }
    $r = Invoke-WebRequest @params
    $text = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
    if ($text) { return $text | ConvertFrom-Json }
    return $null
}

function Login($email) { (Call POST "/api/auth/login" $null @{ email = $email; password = $demoPass }).token }

$ana = Login "ana.admin@creatorworkbench.test"
$rui = Login "rui.revisor@creatorworkbench.test"
$bia = Login "bia.criadora@creatorworkbench.test"

# Team first, so it is Ana's first team (no empty personal team in front of it)
$team = (Call POST "/api/v1/teams" $ana @{ name = "Equipe Office Resolve" }).id
# Plano AGENCIA antes de adicionar membros (limites do plano Gratuito); hook opcional: args[2] = executavel, args[3] = argumento
if ($args[2]) { & $args[2] $args[3] | Out-Host }
Call POST "/api/v1/teams/$team/members" $ana @{ email = "bia.criadora@creatorworkbench.test"; role = "CREATOR" } | Out-Null
Call POST "/api/v1/teams/$team/members" $ana @{ email = "rui.revisor@creatorworkbench.test"; role = "REVIEWER" } | Out-Null
$cols = (Call GET "/api/v1/teams/$team/board" $ana).columns
$col = @{}; foreach ($c in $cols) { $col[$c.name] = $c.id }
Write-Output ("colunas: " + (($cols | ForEach-Object { $_.name }) -join ", "))

# Inbox ideas with tags and votes
$i1 = (Call POST "/api/v1/teams/$team/ideas" $bia @{ title = "Podcast: carreira em TI para iniciantes"; description = "Entrevistas curtas com profissionais da área sobre o primeiro emprego."; tag = "Podcast" }).id
$i2 = (Call POST "/api/v1/teams/$team/ideas" $ana @{ title = "Tutorial: atalhos do Excel que economizam tempo"; description = "Dez atalhos que todo escritório deveria conhecer."; tag = "Tutorial" }).id
$i3 = (Call POST "/api/v1/teams/$team/ideas" $bia @{ title = "Artigo: boas práticas com planilhas compartilhadas"; description = "Como evitar versões duplicadas e perda de dados."; tag = "Artigo" }).id
$i4 = (Call POST "/api/v1/teams/$team/ideas" $ana @{ title = "Minicurso: introdução ao Power BI"; description = "Primeiros painéis a partir de planilhas do escritório."; tag = "Curso" }).id
foreach ($t in @($ana, $bia, $rui)) { Call POST "/api/v1/teams/$team/cards/$i1/vote" $t | Out-Null }
foreach ($t in @($ana, $rui)) { Call POST "/api/v1/teams/$team/cards/$i2/vote" $t | Out-Null }
Call POST "/api/v1/teams/$team/cards/$i3/vote" $bia | Out-Null

function Project($token, $title, $description, $tag, $audience, $objective, $column) {
    $id = (Call POST "/api/v1/teams/$team/ideas" $token @{ title = $title; description = $description; tag = $tag }).id
    Call POST "/api/v1/teams/$team/cards/$id/convert?targetColumnId=$column" $token @{ targetAudience = $audience; pedagogicalObjective = $objective } | Out-Null
    return $id
}
function Tasks($cardId, $done, $open) {
    foreach ($t in $done) {
        $task = Call POST "/api/v1/teams/$team/cards/$cardId/tasks" $ana @{ title = $t }
        Call PATCH "/api/v1/teams/$team/cards/$cardId/tasks/$($task.id)/toggle" $ana | Out-Null
    }
    foreach ($t in $open) { Call POST "/api/v1/teams/$team/cards/$cardId/tasks" $ana @{ title = $t } | Out-Null }
}

$today = Get-Date
$p1 = Project $ana "Curso de Excel do básico ao avançado" "Trilha completa para a equipe administrativa." "Curso" "Profissionais administrativos" "Dominar fórmulas, tabelas dinâmicas e gráficos no dia a dia" $col["Pesquisa"]
Tasks $p1 @("Levantar referências e cursos concorrentes") @("Definir a ementa dos módulos", "Validar a ementa com a Office Resolve")

$p2 = Project $bia "Videoaula: funções PROCV e PROCX" "Aula prática com planilhas reais de clientes." "Tutorial" "Usuários intermediários de Excel" "Aplicar PROCV e PROCX em casos reais do escritório" $col["Roteiro"]
Tasks $p2 @("Separar planilhas de exemplo", "Escrever a introdução") @("Revisar o roteiro com o Rui")
Call PUT "/api/v1/kanban/$p2" $ana @{ dueDate = $today.AddDays(7).ToString("yyyy-MM-dd") } | Out-Null
Call PUT "/api/v1/kanban/$p2/script" $bia @{ content = "<h1>PROCV e PROCX na prática</h1><p>Nesta aula vamos buscar dados entre planilhas sem copiar e colar.</p>" } | Out-Null
Call POST "/api/v1/kanban/$p2/script/versions" $bia | Out-Null
Call PUT "/api/v1/kanban/$p2/script" $bia @{ content = "<h1>PROCV e PROCX na prática</h1><p>Nesta aula vamos buscar dados entre planilhas sem copiar e colar.</p><h2>1. Quando usar o PROCV</h2><p>Mostrar a planilha de clientes e buscar o telefone pelo CNPJ.</p><h2>2. Por que o PROCX é melhor</h2><p>Busca para a esquerda, valor padrão quando não encontra e menos erros.</p>" } | Out-Null
Call POST "/api/v1/kanban/$p2/script/versions" $bia | Out-Null

$p3 = Project $ana "Série: organização financeira para MEI" "Quatro episódios curtos com planilha modelo." "Podcast" "Microempreendedores individuais" "Organizar o fluxo de caixa e separar finanças pessoais e da empresa" $col["Gravação"]
Tasks $p3 @("Gravar episódio 1") @("Gravar episódio 2", "Gravar episódio 3", "Gravar episódio 4")
Call PUT "/api/v1/kanban/$p3" $ana @{ dueDate = $today.AddDays(3).ToString("yyyy-MM-dd") } | Out-Null

$p4 = Project $bia "Webinar: segurança da informação no escritório" "Golpes comuns e como a equipe pode se proteger." "Curso" "Equipes de pequenas empresas" "Reconhecer golpes de phishing e criar senhas fortes" $col["Edição"]
Tasks $p4 @("Cortar silêncios", "Inserir legendas", "Exportar em 1080p") @()
Call PUT "/api/v1/kanban/$p4" $ana @{ dueDate = $today.AddDays(-1).ToString("yyyy-MM-dd") } | Out-Null

# Course (Curso -> Módulo -> Aula), lessons linked to the cards that produce them
$course = (Call POST "/api/v1/teams/$team/courses" $ana @{ title = "Excel para Escritórios"; description = "Curso da Office Resolve para equipes administrativas." }).id
$m1 = (Call POST "/api/v1/teams/$team/courses/$course/modules" $ana @{ title = "Fundamentos" }).id
$m2 = (Call POST "/api/v1/teams/$team/courses/$course/modules" $ana @{ title = "Funções de busca" }).id
$l1 = (Call POST "/api/v1/teams/$team/courses/$course/modules/$m1/lessons" $ana @{ title = "Interface e atalhos"; type = "VIDEO"; projectId = $p1 }).id
Call PATCH "/api/v1/teams/$team/courses/$course/modules/$m1/lessons/$l1" $ana @{ published = $true } | Out-Null
Call POST "/api/v1/teams/$team/courses/$course/modules/$m1/lessons" $ana @{ title = "Fórmulas essenciais"; type = "TEXT" } | Out-Null
Call POST "/api/v1/teams/$team/courses/$course/modules/$m2/lessons" $ana @{ title = "PROCV e PROCX na prática"; type = "VIDEO"; projectId = $p2 } | Out-Null
Call POST "/api/v1/teams/$team/courses/$course/modules/$m2/lessons" $ana @{ title = "Exercícios de fixação"; type = "QUIZ" } | Out-Null

# Rui and Bia list their teams once, so nothing else is created for them later
Call GET "/api/v1/teams" $rui | Out-Null
Call GET "/api/v1/teams" $bia | Out-Null

$board = Call GET "/api/v1/teams/$team/board" $ana
foreach ($c in $board.columns) { Write-Output ("{0}: {1}" -f $c.name, (($c.cards | ForEach-Object { "$($_.title) [$($_.tasksDone)/$($_.tasksTotal)]" }) -join " | ")) }
$ideas = Call GET "/api/v1/teams/$team/ideas" $ana
foreach ($i in $ideas) { Write-Output ("ideia: {0} [{1}] votos={2}" -f $i.title, $i.tag, $i.votes) }
Write-Output ("cursos: " + ((Call GET "/api/v1/teams/$team/courses" $ana) | ForEach-Object { "$($_.title) ($($_.modulesCount) módulos)" }))
Write-Output ("equipes da Ana: " + ((Call GET "/api/v1/teams" $ana) | ForEach-Object { "$($_.name) ($($_.role))" }) -join ", ")
