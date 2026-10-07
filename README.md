# Ficha — registro de treinos (PWA)

App web instalável para montar fichas de treino e registrar cargas, séries e evolução.
Funciona no iPhone pela Tela de Início, offline, sem App Store, sem Mac e sem conta de desenvolvedor.

## Funcionalidades

- **Catálogo com ~110 exercícios** em 12 grupos musculares, com busca e filtro; crie exercícios personalizados.
- **Fichas**: monte quantas quiser (A, B, C…), escolha os dias da semana, séries, repetições, descanso e observações por exercício. Tem um modelo ABC pronto para começar.
- **Hoje**: mostra a ficha do dia, a semana com dias planejados/treinados e estatísticas.
- **Modo treino**: marque cada série, veja o que fez no último treino, cargas pré-preenchidas, séries de aquecimento, cronômetro de descanso com aviso sonoro, adicionar/trocar/reordenar exercícios no meio do treino.
- **Histórico**: mapa de frequência, volume, duração, recordes pessoais (PR) detectados automaticamente.
- **Evolução por exercício**: gráfico de carga máxima, 1RM estimado ou volume.
- **Backup**: exportar/importar um arquivo `.json` (vai para o app Arquivos/iCloud).

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

Edite os arquivos, aumente a versão em `sw.js` (`const CACHE = 'ficha-v2'`) e faça `git push`.
No iPhone, abra o app com internet; a nova versão carrega na próxima abertura. Os dados não são apagados.

## Estrutura

| Arquivo | O que é |
|---|---|
| `index.html` | Página base, metatags do iOS e barra de abas |
| `styles.css` | Visual (tema escuro/claro automático, áreas seguras do iPhone) |
| `app.js` | Toda a lógica: telas, roteamento por hash, treino, histórico, gráficos |
| `exercises.js` | Catálogo de exercícios — edite à vontade |
| `sw.js` | Service worker (cache offline) |
| `manifest.webmanifest`, `icons/` | Instalação e ícones |

Sem frameworks nem build: é HTML, CSS e JavaScript puro.
