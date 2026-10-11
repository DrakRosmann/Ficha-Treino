# Respostas para "Segurança dos dados"

Conferidas com o que o app e o servidor fazem de fato (ver `privacidade.html`).

## Perguntas gerais

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha algum dos tipos de dados exigidos? | **Sim** |
| Todos os dados coletados são criptografados em trânsito? | **Sim** (HTTPS em tudo; a sincronização ainda é criptografada no aparelho) |
| Os usuários podem pedir a exclusão dos dados? | **Sim** — Ajustes → Apagar todos os dados; Nuvem → apagar da nuvem; pedidos pelo GitHub |

## Tipos de dados

| Tipo (nome na Play Console) | Coletado | Compartilhado | Opcional? | Finalidade |
|---|---|---|---|---|
| **IDs do dispositivo ou outros IDs** (ID de publicidade) | Sim | Sim (Google AdMob) | Não no plano grátis; assinantes não | Publicidade ou marketing, Análise |
| **Interações com o app** (com os anúncios) | Sim | Sim (Google AdMob) | Não no plano grátis | Publicidade ou marketing, Análise |
| **Diagnóstico** (falhas do SDK de anúncios) | Sim | Sim (Google AdMob) | Não no plano grátis | Análise |
| **Saúde e fitness → Informações de condicionamento físico** (treinos, cargas) | Sim | Sim (Anthropic, só ao usar a IA) | **Sim** (só com Ficha IA) | Funcionalidade do app |
| **Saúde e fitness → Informações de saúde** (peso, medidas, dieta) | Sim | Sim (Anthropic, só ao usar a IA) | **Sim** (só com Ficha IA) | Funcionalidade do app |
| **Fotos e vídeos → Fotos** (foto do prato ou do rótulo) | Sim | Sim (Anthropic, só ao usar a IA) | **Sim** | Funcionalidade do app |
| **Compras no app** (token da assinatura) | Sim | Não | — | Funcionalidade do app (liberar o plano) |

Para os dados de saúde, fitness e fotos marque: **processados temporariamente** (enviados só para gerar a resposta;
o servidor do Ficha não guarda o conteúdo).

## O que **não** declarar

- **Sincronização**: criptografada de ponta a ponta no aparelho; o servidor não tem como ler. Pela regra da Play,
  dados criptografados de ponta a ponta que o desenvolvedor não consegue acessar **não contam como coletados**.
- **Fotos do progresso, histórico completo e backup**: ficam só no aparelho.
- **Código de barras**: só o número vai ao Open Food Facts, sem identificar a pessoa.
- **Localização, contatos, mensagens, áudio, e-mail, nome**: o app não acessa.
