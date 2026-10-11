# Servidor do Ficha (sincronização e lembretes)

O Ficha funciona sem servidor. Este servidor é opcional e serve para duas coisas:

- **Sincronizar** treinos, fichas, dieta e medidas entre aparelhos (e não perder nada se trocar de celular);
- **Lembretes por notificação**: treino do dia, água, refeições, pesagem semanal e backup.

Ele roda de graça na Cloudflare (Workers + KV) e é só seu. Os dados chegam lá **criptografados no aparelho** com o seu código de sincronização (AES-GCM de 256 bits); o servidor guarda um arquivo embaralhado e não consegue ler nada.

## Criar pelo navegador (sem programar, uns 5 minutos)

1. Crie uma conta grátis em [dash.cloudflare.com](https://dash.cloudflare.com).
2. **Workers & Pages** → **Create** → comece pelo modelo **Hello World**. Dê o nome `ficha-sync` e toque em **Deploy**.
3. Toque em **Edit code**, apague todo o código, cole o conteúdo de [`worker.js`](worker.js) e toque em **Deploy**.
4. Crie o banco: **Storage & Databases** → **KV** → **Create** (pode chamar de `ficha`).
5. Volte ao Worker → **Settings** → **Bindings** → **Add binding** → **KV namespace**. Em *Variable name* escreva exatamente `FICHA` e escolha o KV criado. Confirme.
6. Ainda em **Settings** → **Triggers** → **Cron Triggers** → **Add**, use a expressão `*/5 * * * *` (a cada 5 minutos). É ela que envia os lembretes na hora certa; pode levar até 15 minutos para começar a valer.
7. Opcional, em **Settings** → **Variables and Secrets**:
   - `ALLOW_ORIGIN` = endereço do app (ex.: `https://drakrosmann.github.io`), para só o seu app usar o servidor;
   - `VAPID_SUBJECT` = `mailto:seu@email.com` (contato enviado aos serviços de notificação; sem espaços nem `< >`).
8. Copie o endereço do Worker (algo como `https://ficha-sync.seu-nome.workers.dev`). Abra `…/v1/ping` no navegador: deve aparecer `{"ok":true,"app":"ficha",…}`.

No app: **Ajustes → Nuvem e lembretes**, cole o endereço, toque em **Testar conexão** e depois em **Começar a sincronizar**. Guarde o código que aparece (no app Senhas ou nas Notas) e use **Já tenho um código** nos outros aparelhos.

Para os aparelhos já virem com o endereço preenchido, coloque-o em `CLOUD_DEFAULT_URL`, no início de `cloud.js`.

## Criar pela linha de comando (Wrangler)

```sh
cd server
npx wrangler login
npx wrangler kv namespace create FICHA   # copie o id que aparece para o wrangler.toml
npx wrangler deploy
```

O `wrangler.toml` já tem o Cron Trigger. Se você publicar com o Wrangler, os gatilhos do arquivo substituem os do painel.

## Notificações no iPhone

- Precisa do **iOS 16.4 ou mais novo** e do app **instalado na Tela de Início** (Safari → Compartilhar → Adicionar à Tela de Início). Aberto no Safari comum, o iPhone não entrega notificações de sites.
- A permissão é pedida quando você liga **Notificações neste aparelho**. Se negar sem querer: Ajustes do iPhone → Notificações → Ficha.
- Cada aparelho tem os próprios lembretes e horários, no fuso dele. O app avisa o servidor do que já foi feito no dia (treino, refeição registrada, meta de água, pesagem), e esses lembretes não são enviados.

## Limites do plano grátis

Sobra para uso pessoal: 100 mil requisições por dia no Worker e, no KV, 100 mil leituras, 1.000 gravações e 1.000 listagens por dia, com 1 GB de espaço. O cron a cada 5 minutos usa 288 listagens por dia. Cada sincronização é uma gravação, e o app só envia quando algo muda (o treino em andamento e as fotos do progresso não vão para a nuvem).

## IA para assinantes (app Android)

O app Android vende a IA por assinatura (Ficha IA). Quem paga a Anthropic é você, então os pedidos passam por este
servidor: ele confere no Google Play que a assinatura está ativa e repassa à Anthropic com a **sua** chave, que
nunca vai para o app. Cada assinante tem um limite de gasto por mês e de pedidos por dia, para ninguém custar mais
do que paga.

1. **Chave da Anthropic**: crie em [platform.claude.com](https://platform.claude.com) → *API keys* e salve no Worker
   como segredo `ANTHROPIC_API_KEY` (**Settings → Variables and Secrets → Add → Secret**, ou `npx wrangler secret put ANTHROPIC_API_KEY`).
2. **Ligação com o Google Play** (para conferir as assinaturas):
   - No [Google Cloud](https://console.cloud.google.com), crie um projeto, ative a **Google Play Android Developer API**
     e crie uma **conta de serviço** com uma chave JSON.
   - No **Play Console → Usuários e permissões**, convide o e-mail da conta de serviço com a permissão
     **Ver dados financeiros** (e acesso ao app).
   - Salve o conteúdo inteiro do JSON como segredo `GOOGLE_SERVICE_ACCOUNT`.
3. **Variáveis** (opcionais, com os valores padrão):

| Variável | Padrão | O que é |
|---|---|---|
| `PLAY_PACKAGE` | `io.github.drakrosmann.ficha` | Pacote do app no Google Play |
| `AI_PRODUCT` | `ficha_ia` | ID da assinatura que libera a IA |
| `AI_MODEL` | `claude-opus-5-5` | Modelo usado (o app não escolhe) |
| `AI_MONTHLY_USD` | `2.5` | Gasto máximo de IA por assinante por mês, em US$ (renova todo dia 1º, UTC) |
| `AI_DAILY_LIMIT` | `40` | Pedidos à IA por assinante por dia |
| `AI_MAX_TOKENS` | `32000` | Teto de `max_tokens` por pedido |
| `AI_PRICE_IN` / `AI_PRICE_OUT` / `AI_PRICE_CACHE_READ` | `4` / `20` / `0.2` | Preço do modelo em US$ por milhão de tokens, para somar o gasto |

O gasto é medido pelos tokens que a própria Anthropic informa em cada resposta (se o app cancelar no meio, o servidor
estima pelo texto já enviado). Com o Opus 5.5, cada conversa com o treinador custa em torno de US$ 0,01–0,05 e cada
programa montado pela IA ou relatório da semana, US$ 0,10–0,30. Ajuste `AI_MONTHLY_USD` ao preço da assinatura:
depois da taxa do Google (15%), sobre o resto.

Cada pedido à IA grava 2 vezes no KV (contador do dia e gasto do mês). O plano grátis do KV (1.000 gravações por dia)
serve para testar; com assinantes, use o plano pago do Workers (US$ 5/mês, 1 milhão de gravações).

Para testar sem o Google Play, a variável `DEV_TOKENS` aceita tokens fixos (separados por vírgula) como se fossem
assinaturas ativas. **Nunca a deixe ligada no servidor de verdade.** A build de debug do app usa o token `dev-token`
quando você simula o plano IA em Ajustes → Assinatura.

## Como funciona

| Rota | O que faz |
|---|---|
| `GET /v1/ping` | Confere se o servidor está no ar |
| `GET/PUT/DELETE /v1/data/:id` | Arquivo criptografado do usuário. `id` e a autorização são derivados do código (SHA-256); o servidor guarda só o hash da autorização. O `PUT` leva a versão em que se baseou: se outro aparelho enviou antes, responde `409` com a versão atual e o app junta as duas |
| `GET /v1/push/key` | Chave pública VAPID (o par de chaves é criado na primeira vez e fica no KV) |
| `PUT/DELETE /v1/push/:sid` | Inscrição de notificação do aparelho, fuso e lista de lembretes |
| `POST /v1/push/:sid/done` | O que já foi feito hoje (para não lembrar) |
| `POST /v1/push/:sid/test` | Envia uma notificação de teste |
| `POST /v1/ai/v1/messages` | IA para assinantes: o app usa o SDK da Anthropic com `…/v1/ai` como endereço e o token da compra do Google Play. Confere a assinatura (cache de 30 min), aplica os limites, troca o token pela chave do servidor, força o modelo, tira campos não permitidos (como ferramentas) e devolve a resposta em streaming |
| `GET /v1/ai/usage` | Quanto do limite do mês o assinante já usou (o app mostra em Ajustes → Assinatura) |

As notificações seguem os padrões Web Push: assinatura VAPID (RFC 8292) e criptografia `aes128gcm` (RFC 8291), feitas com a WebCrypto do próprio Worker, sem bibliotecas.

A junção entre aparelhos é de três vias: compara o que mudou em cada lado desde a última sincronização. Mudou só de um lado, fica a mudança; mudou o mesmo campo nos dois, vale o aparelho que mexeu por último; excluir de um lado sem mexer do outro, exclui.
