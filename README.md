# Ficha — registro de treinos (PWA)

App web instalável para montar fichas de treino e registrar cargas, séries e evolução.
Funciona no iPhone pela Tela de Início, offline, sem App Store, sem Mac e sem conta de desenvolvedor.

## Funcionalidades

- **Catálogo com ~900 exercícios** em 15 grupos (musculação, funcional/LPO, cardio, alongamento e mobilidade), com busca em português ou inglês e filtro por grupo; crie exercícios personalizados.
- **Execução de cada exercício**: foto animada da posição inicial → final (873 exercícios), músculos trabalhados, atalho para vídeos no YouTube e opção de salvar o link do seu próprio vídeo (ex.: enviado pelo personal). Toque na miniatura em qualquer lista, ficha ou no treino.
- **Programas e fichas**: um programa agrupa várias fichas (ex.: “Meu treino” → Push, Pull, Legs). Em cada ficha você escolhe dias da semana, séries, repetições, descanso e observações. Programas podem ser pausados; sem dias fixos, a tela Hoje sugere a próxima ficha na ordem (A → B → C…).
- **Modelos prontos**: 14 programas para adicionar e ajustar — ABC, ABCD, ABCDE, Push/Pull/Legs, Upper/Lower, PHUL, Arnold split, iniciante, foco em glúteos, só halteres, em casa sem equipamento, funcional, alongamento/mobilidade e abdômen.
- **Aparência**: tema automático, claro, escuro ou preto (OLED), 10 cores de destaque e efeito vidro no estilo Liquid Glass do iOS 26 (barra de abas flutuante e painéis translúcidos).
- **Hoje**: mostra a ficha do dia, a semana com dias planejados/treinados e estatísticas.
- **Modo treino**: marque cada série, veja o que fez no último treino, cargas pré-preenchidas, séries de aquecimento, cronômetro de descanso com aviso sonoro, adicionar/trocar/reordenar exercícios no meio do treino.
- **Histórico**: mapa de frequência, volume, duração, recordes pessoais (PR) detectados automaticamente.
- **Evolução por exercício**: gráfico de carga máxima, 1RM estimado ou volume.
- **Backup**: exportar/importar um arquivo `.json` (vai para o app Arquivos/iCloud).
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

> Os dados ficam salvos só no iPhone, dentro do app instalado. Se apagar o ícone da Tela de Início, os dados vão junto — use **Ajustes → Exportar backup** de vez em quando.

## Testar no computador

```bash
cd ficha
python3 -m http.server 8000
# abra http://localhost:8000
```

No Chrome/Firefox use o modo de dispositivo móvel (F12 → ícone de celular) para ver como fica no iPhone.

## Atualizar o app

Edite os arquivos, aumente a versão em `sw.js` (`const CACHE = 'ficha-v4'`) e faça `git push`.
O app procura a versão nova sozinho sempre que é aberto (com internet) e recarrega uma vez quando encontra. Se quiser forçar, use **Ajustes → Procurar atualização**. Os dados não são apagados.

## Estrutura

| Arquivo | O que é |
|---|---|
| `index.html` | Página base, metatags do iOS e barra de abas |
| `styles.css` | Visual (tema escuro/claro automático, áreas seguras do iPhone) |
| `app.js` | Toda a lógica: telas, roteamento por hash, treino, histórico, gráficos |
| `exercises.js` | Catálogo de exercícios (nome, grupo, equipamento, tipo, foto, músculos) — edite à vontade |
| `templates.js` | Modelos de programas prontos — edite ou crie os seus |
| `img/ex/`, `img/thumb/` | Fotos da execução (início e fim lado a lado) e miniaturas, em WebP |
| `sw.js` | Service worker (cache offline) |
| `manifest.webmanifest`, `icons/` | Instalação e ícones |

Sem frameworks nem build: é HTML, CSS e JavaScript puro.

## Créditos

Fotos, músculos e a base do catálogo vêm do [free-exercise-db](https://github.com/yuhonas/free-exercise-db), em domínio público (Unlicense). Os nomes foram traduzidos para o português e as fotos redimensionadas.
