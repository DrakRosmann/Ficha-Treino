# Ficha — registro de treinos (PWA)

App web instalável para montar fichas de treino, registrar cargas, séries e evolução, e acompanhar a alimentação.
Funciona no iPhone pela Tela de Início, offline, sem App Store, sem Mac e sem conta de desenvolvedor.

## Funcionalidades

- **Catálogo com ~900 exercícios** (em Fichas → Exercícios) em 15 grupos (musculação, funcional/LPO, cardio, alongamento e mobilidade), com busca em português ou inglês e filtro por grupo; crie exercícios personalizados.
- **Mapa muscular**: cada exercício mostra um desenho do corpo (frente e costas) com os músculos principais em destaque e os secundários mais claros.
- **Execução de cada exercício**: foto animada da posição inicial → final (873 exercícios), músculos trabalhados, atalho para vídeos no YouTube e opção de salvar o link do seu próprio vídeo (ex.: enviado pelo personal). Na ficha e no treino, cada exercício aparece com uma foto grande da execução, alternando a posição inicial e a final; toque nela (ou na miniatura das listas) para abrir a execução completa.
- **Programas e fichas**: um programa agrupa várias fichas (ex.: “Meu treino” → Push, Pull, Legs). Em cada ficha você escolhe dias da semana, séries, repetições, descanso e observações. Programas podem ser pausados; sem dias fixos, a tela Hoje sugere a próxima ficha na ordem (A → B → C…).
- **Modelos prontos**: 14 programas para adicionar e ajustar — ABC, ABCD, ABCDE, Push/Pull/Legs, Upper/Lower, PHUL, Arnold split, iniciante, foco em glúteos, só halteres, em casa sem equipamento, funcional, alongamento/mobilidade e abdômen.
- **Assistente de treino**: você informa objetivo, experiência, dias por semana, tempo por treino, local, peso, altura, idade, grupos prioritários e restrições (joelho, lombar, ombro, punho, quadril, sem impacto, pressão alta) e recebe 3 opções de programa para escolher, com a explicação de cada uma.
  - **Sem IA** (padrão): regras de treino que escolhem a divisão, os exercícios seguros para as restrições, séries, repetições e descanso conforme o objetivo, e cabem no tempo informado. Funciona offline e é grátis.
  - **Com IA (Claude)**: entende observações em texto livre (ex.: “hérnia de disco”). Precisa de internet e de uma chave da API da Anthropic, salva só no aparelho (fica fora do backup). O uso é cobrado pela Anthropic na sua conta.
- **Aparência**: tema automático, claro, escuro ou preto (OLED), 10 cores de destaque e estilo visual. No **Automático** (padrão), o app identifica o aparelho e usa o estilo nativo: Liquid Glass no iPhone e iPad, Material You no Android e Clássico no computador e nos demais. Também dá para escolher um dos três:
  - **Clássico**: visual sólido, sem transparências.
  - **Liquid Glass**: segue o iOS 26 — cores do sistema, conteúdo sólido e vidro só na navegação. Barra de abas em cápsula com a seleção que desliza e estica, vira lente ao tocar e acompanha o dedo ao arrastar entre as abas; a barra **minimiza ao rolar para baixo** e o descanso/treino em andamento fica ao lado dela (como o mini player do Apple Music). Botões do topo em vidro fixos, com o título pequeno aparecendo ao rolar, efeito de borda sob a barra de status, painéis flutuantes com cantos grandes, botões em cápsula, segmentado e interruptor do iOS 26 (a bolinha vira lente ao tocar) e, opcionalmente, reflexo do vidro que acompanha a inclinação do iPhone. Com “Reduzir transparência” o vidro fica sólido.
    - **Movimento líquido**: as animações usam molas de verdade (física de massa-mola, com o “passar do ponto” e o balanço ao parar), calculadas uma vez e entregues ao navegador como animações de transform/opacity, que rodam fora do JavaScript (fluidas mesmo quando a tela troca de conteúdo). Ao abrir uma tela, a nova desliza por cima da anterior e a barra de vidro continua viva durante a troca; trocar de aba é instantâneo, como no iPhone. A seleção da barra de abas escorre como uma gota: estica na direção do movimento, achata e assenta; ao tocar vira lente, segue o dedo, amplia o ícone embaixo e estica no elástico das pontas. Botões e controles de vidro crescem ao toque, acompanham um pouco o dedo, brilham no ponto tocado e balançam ao soltar. Os botões **se transformam no painel** que abrem (e o painel volta para o botão ao fechar), o segmentado desliza a seleção, a bolinha do interruptor vira gota no caminho e os avisos surgem do topo como a Dynamic Island. Desliga com “Reduzir movimento”.
  - **Material You**: no estilo do Android. As cores tonais (fundo, cartões, botões e contêineres) são geradas a partir da cor de destaque, como no Material 3. Tem cantos arredondados, botões em cápsula, barra de navegação com indicador, interruptores e campos do Material, efeito de toque e a fonte Google Sans Flex.
- **Animações**: transições entre as telas (avançar, voltar e trocar de aba), indicador de aba que desliza, painéis com efeito de mola que fecham ao arrastar para baixo, ✓ animado ao concluir a série, gráficos e anéis que se desenham e confete no fim do treino. As animações mexem só em posição, escala e transparência (que o celular desenha sem esforço), usam curvas de mola de verdade, e o app evita trabalho pesado durante elas: marcar uma série atualiza só aquele exercício, as gravações no aparelho são agrupadas e as listas longas (histórico, medidas) mostram os mais recentes, com “Mostrar mais”. Tudo é desligado quando o iPhone está com “Reduzir movimento”.
- **Hoje**: mostra a ficha do dia, a semana com dias planejados/treinados e estatísticas.
- **Timer do iPhone**: opção de usar o Timer do relógio do iPhone no descanso. Ao terminar uma série, o app chama um atalho do app **Atalhos** (criado uma vez, com a ação “Iniciar Timer”), e o timer de verdade aparece na tela bloqueada e na Dynamic Island, com o alarme do iPhone. O passo a passo está em **Ajustes → Configurar o Timer do iPhone**. Também dá para manter a tela ligada durante o treino. (Um app da web não pode criar Live Activities próprias; isso só existe em apps nativos da App Store.)
- **Modo treino**: marque cada série, veja o que fez no último treino, cronômetro de descanso com aviso sonoro, adicionar/trocar/reordenar exercícios no meio do treino.
  - **Progressão automática**: em cada exercício, o app sugere a carga e as repetições de hoje com base no último treino e na faixa da ficha (ex.: 8–12). É a dupla progressão: fez o topo da faixa em todas as séries → sobe a carga; ficou dentro da faixa → +1 rep; ficou abaixo da faixa duas vezes seguidas → reduz ~10%. Os campos já vêm com a sugestão.
  - **RIR** (repetições na reserva) opcional em cada série; quando registrado, a progressão ajusta o passo.
  - **Tipos de série** (toque no número da série): normal, aquecimento, drop set (sem descanso antes e com carga sugerida ~20% menor) e até a falha.
  - **Aquecimento automático**: séries leves calculadas pela carga de trabalho (menu ⋯ do exercício).
  - **Superséries e circuitos**: na ficha, toque em “Juntar em supersérie” entre dois exercícios (3 ou mais juntos viram circuito). No treino não há descanso entre os exercícios do grupo; o descanso vem no fim da rodada e o app leva você de um exercício para o outro.
- **Dieta** (aba própria):
  - **Metas** de calorias, proteína, carboidrato e gordura pelo objetivo (perder gordura, manter ou ganhar massa), ritmo semanal, nível de atividade e proteína por kg. O gasto parte da fórmula de Mifflin-St Jeor (ou Katch-McArdle, com o % de gordura da aba Corpo).
  - **Gasto adaptativo**, como no MacroFactor: toda semana o app compara o que você registrou com a tendência do seu peso (regressão das pesagens das últimas 4 semanas) e recalcula o gasto real e as metas.
  - **Registro por refeição** (café, almoço, lanche, jantar) com os 591 alimentos da **Tabela TACO** (Unicamp) e itens comuns de academia (whey, creatina, pasta de amendoim…), porções caseiras (colher de sopa de arroz, concha de feijão, unidade de ovo…), recentes, favoritos, “repetir de ontem”, alimentos próprios e navegação entre os dias.
  - **IA (Claude)**: descreva a refeição em texto, tire foto do prato ou do rótulo, e a IA estima os alimentos, as gramas e os macros (você confere antes de salvar). Usa a mesma chave da Anthropic do assistente de treino.
  - **Código de barras** pelo Open Food Facts (a câmera lê o código onde o navegador permite; no iPhone, digite os números ou use a foto do rótulo).
  - **Refeições prontas**: salve uma refeição que se repete (ex.: “café de sempre”) e adicione tudo com um toque.
  - **Micronutrientes** da TACO: fibras, sódio (com o limite de 2.000 mg), potássio, cálcio, ferro, magnésio e vitamina C, comparados com as referências diárias para adultos.
  - **Água**, resumo dos últimos 7 dias e um card na tela Hoje.
- **Corpo**: registre peso, composição corporal (gordura, massa muscular, água, gordura visceral, massa óssea, metabolismo basal) e medidas (pescoço, ombros, peito, braço, antebraço, cintura, abdômen, quadril, coxa, panturrilha). Mostra resumo com variação desde o início, gráficos por período (com meta de peso), massa magra, IMC, relação cintura/quadril, estimativa de gordura pelas medidas (método da Marinha dos EUA) e um mapa dos músculos treinados nos últimos 7 dias.
- **Fotos do progresso** (aba Corpo): fotos de frente, de lado e de costas por data, tiradas pela **câmera com guia** (mostra a foto anterior transparente por cima para você se posicionar igual, com grade e timer de 3 ou 10 s) ou escolhidas da galeria. Galeria por pose, **comparação antes/depois** deslizando ou lado a lado, com a variação de peso, gordura, cintura e braço entre as datas. As fotos ficam só no aparelho (IndexedDB); o backup pergunta se deve incluí-las.
- **Treinador IA (Claude)** (tela Hoje): **relatório da semana** que lê treinos, cargas, evolução do 1RM, séries por músculo, dieta, gasto e tendência do peso, e diz o que foi bem, o que precisa de atenção e as metas da próxima semana; e uma **conversa** para perguntar sobre o seu treino e a sua dieta com base nos seus registros. Usa a mesma chave da Anthropic.
- **Compartilhar nos Stories**: o resumo do treino vira uma imagem 1080 × 1920 (cartão, sobre uma foto sua ou adesivo com fundo transparente), com duração, séries, volume, melhor série de cada exercício, recordes e conquistas.
- **Conquistas**: 28 medalhas (bronze, prata, ouro e especiais) por número de treinos, toneladas levantadas, semanas seguidas, semana perfeita, recordes, força relativa ao peso (supino com o próprio peso, agachamento 1,5×, terra 2×), dieta, água, pesagens e fotos. Aparecem no fim do treino e no Histórico, com o progresso das que faltam, e também podem ser compartilhadas.
- **Nuvem e lembretes** (opcional, Ajustes → Nuvem e lembretes), com um servidor grátis seu na Cloudflare ([passo a passo](server/README.md)):
  - **Sincronização** entre iPhone, iPad e outros aparelhos com **criptografia de ponta a ponta** (o servidor não consegue ler). Um código de 20 caracteres liga os aparelhos; mudanças feitas em aparelhos diferentes são juntadas campo a campo.
  - **Lembretes por notificação** (iPhone com iOS 16.4+, app instalado na Tela de Início): treino do dia (pelos dias das fichas), água, refeições, pesagem semanal e backup, no fuso do aparelho. O que você já fez no dia não é lembrado.
- **Histórico**: mapa de frequência, volume, duração, recordes pessoais (PR) detectados automaticamente.
  - **Séries por músculo**: quantas séries cada grupo muscular fez na semana (esta semana, semana passada ou média de 4 semanas), comparadas com a faixa de 10 a 20 séries usada nos estudos de hipertrofia. Músculos secundários contam meia série.
- **Evolução por exercício**: gráfico de carga máxima, 1RM estimado ou volume, e a sugestão para o próximo treino.
- **Calculadoras** (Ajustes → Treino, menu do exercício ou página do exercício): anilhas por lado (barra de 20, 15, 10 kg ou sem barra, com as anilhas que a sua academia tem), aquecimento e 1RM com a tabela de cargas por repetição.
- **Backup e segurança dos dados**: exportar/importar um arquivo `.json` (vai para o app Arquivos/iCloud). O app pede ao navegador para não apagar os dados quando faltar espaço, lembra de fazer backup (na tela Hoje e no fim do treino, quando há 3 ou mais treinos sem backup há mais de uma semana) e guarda uma cópia antes de importar, para dar para desfazer.
- **Fotos offline**: cada foto fica salva depois que aparece uma vez; em **Ajustes → Baixar fotos para usar offline** dá para baixar todas de uma vez (~19 MB, use no Wi-Fi).

Tipos de registro: carga × reps, peso corporal (+kg opcional) × reps, tempo em segundos (prancha) e cardio (min / km).

## Publicar no GitHub Pages (grátis)

1. Crie um repositório público no GitHub, por exemplo `ficha`.
2. Envie os arquivos desta pasta para a raiz do repositório:
   ```bash
   cd ficha
   git init
   git add .
   git commit -m "Ficha: app de treinos"
   git branch -M main
   git remote add origin git@github.com:SEU_USUARIO/ficha.git
   git push -u origin main
   ```
3. No GitHub: **Settings → Pages → Build and deployment → Source: Deploy from a branch**, branch `main`, pasta `/ (root)`, **Save**.
4. Em 1–2 minutos o app fica em `https://SEU_USUARIO.github.io/ficha/`.

## Instalar no iPhone

1. Abra o endereço no **Safari** (tem que ser o Safari).
2. Toque em **Compartilhar** → **Adicionar à Tela de Início** → **Adicionar**.
3. Abra pelo ícone: ele roda em tela cheia e funciona sem internet.

> Os dados ficam salvos no iPhone, dentro do app instalado. Se apagar o ícone da Tela de Início, os dados vão junto — use **Ajustes → Exportar backup** de vez em quando, ou ative a **nuvem**.

## Testar no computador

```bash
cd ficha
python3 -m http.server 8000
# abra http://localhost:8000
```

No Chrome/Firefox use o modo de dispositivo móvel (F12 → ícone de celular) para ver como fica no iPhone.

## Atualizar o app

Edite os arquivos, aumente a versão em `sw.js` (`const CACHE = 'ficha-v21'`) e faça `git push`.
O app procura a versão nova sozinho sempre que é aberto (com internet) e recarrega uma vez quando encontra. Se quiser forçar, use **Ajustes → Procurar atualização**. Os dados não são apagados.

## Estrutura

| Arquivo | O que é |
|---|---|
| `index.html` | Página base, metatags do iOS e barra de abas |
| `styles.css` | Visual (tema escuro/claro automático, áreas seguras do iPhone) |
| `app.js` | Toda a lógica: telas, roteamento por hash, treino, histórico, gráficos |
| `exercises.js` | Catálogo de exercícios (nome, grupo, equipamento, tipo, foto, músculos) — edite à vontade |
| `templates.js` | Modelos de programas prontos — edite ou crie os seus |
| `body.js` | Aba Corpo (medidas, composição corporal, gráficos) e mapa muscular |
| `tools.js` | Progressão automática, séries por músculo e calculadoras (anilhas, aquecimento, 1RM) |
| `nutrition.js` | Aba Dieta: registro, metas, gasto adaptativo, água, IA e código de barras |
| `photos.js` | Fotos do progresso: armazenamento no aparelho, câmera com guia, galeria e comparação |
| `foods.js` | Banco de alimentos (TACO com micronutrientes + itens comuns de academia), valores por 100 g |
| `share.js` | Imagem do treino para os Stories e conquistas |
| `coach.js` | Treinador IA: relatório da semana e conversa |
| `cloud.js` | Sincronização criptografada entre aparelhos e lembretes por notificação |
| `glass.js` | Movimento do Liquid Glass: molas, lente da barra de abas, brilho no toque, botão que vira painel |
| `server/` | Servidor opcional (Cloudflare Worker + KV) da nuvem e dos lembretes, com o passo a passo |
| `assistant.js` | Assistente de treino: regras de montagem (padrões de movimento, restrições, séries por objetivo) e integração com o Claude |
| `vendor/anthropic-sdk.mjs` | SDK oficial da Anthropic empacotado para o navegador (carregado só quando a IA é usada) |
| `fonts/` | Fonte Google Sans Flex do estilo Material You (só o alfabeto latino) |
| `img/ex/`, `img/thumb/` | Fotos da execução (início e fim lado a lado) e miniaturas, em WebP |
| `sw.js` | Service worker (cache offline e notificações) |
| `manifest.webmanifest`, `icons/` | Instalação e ícones |

Sem frameworks nem build: é HTML, CSS e JavaScript puro.

## Créditos

Alimentos: Tabela Brasileira de Composição de Alimentos (TACO), 4ª edição revisada e ampliada, NEPA/UNICAMP, 2011 (reprodução permitida citando a fonte), organizada pelo projeto [taco-api](https://github.com/raulfdm/taco-api) (MIT). Produtos por código de barras: [Open Food Facts](https://world.openfoodfacts.org) (base aberta, ODbL).

A fonte do estilo Material You é a [Google Sans Flex](https://github.com/googlefonts/googlesans-flex) (SIL Open Font License 1.1; licença em `fonts/OFL.txt`).

O desenho do mapa muscular é adaptado do [react-body-highlighter](https://github.com/GV79/react-body-highlighter) (licença MIT, © 2020 GV79).

Fotos, músculos e a base do catálogo vêm do [free-exercise-db](https://github.com/yuhonas/free-exercise-db), em domínio público (Unlicense). Os nomes foram traduzidos para o português e as fotos redimensionadas.
