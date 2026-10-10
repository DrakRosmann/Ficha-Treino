# Ficha para Android

Versão nativa do Ficha para Android, em **Kotlin + Jetpack Compose** com **Material 3 Expressive**.
Usa os mesmos dados do PWA: o backup `.json` exportado em um app é importado no outro.

## Abrir e rodar

1. No Android Studio: **File → Open** e escolha esta pasta (`android/`).
2. Espere o Gradle sincronizar e toque em **Run** (emulador ou celular com depuração USB).

Pela linha de comando (usando o JDK que vem com o Android Studio):

```bash
cd android
export JAVA_HOME=/opt/android-studio-canary/jbr   # ou o caminho do seu Android Studio
./gradlew installDebug        # instala a versão de desenvolvimento no aparelho conectado
./gradlew assembleRelease     # APK otimizado em app/build/outputs/apk/release/
```

O APK de release é assinado com a chave de debug, para instalar direto (`adb install -r …`) sem criar uma chave.
Para publicar na Play Store, crie uma chave própria e troque o `signingConfig` em `app/build.gradle.kts`.

Requer Android 8.0 ou mais novo. As cores do papel de parede (Material You) precisam do Android 12+,
e o descanso na barra de status (Live Update) aparece no Android 16+.

## Levar os dados do PWA

1. No PWA: **Ajustes → Exportar backup**.
2. No Android: **Ajustes → Importar backup** e escolha o arquivo.

Tudo vem junto: programas, fichas, histórico, exercícios personalizados, vídeos, dieta, medidas, perfil, conquistas,
conversa com o treinador e, se o backup foi exportado com elas, as fotos do progresso. O backup exportado pelo
Android abre no PWA do mesmo jeito.

Outra opção é a **sincronização** (Ajustes → Nuvem e lembretes): com o mesmo servidor e o mesmo código, o Android e o
PWA ficam sempre iguais.

## O que já existe

- **Hoje**: semana com dias planejados e treinados, ficha do dia ou próxima da sequência, estatísticas e último treino.
- **Fichas e programas**: criar, renomear, duplicar, pausar, reordenar, dias da semana (botões conectados do Expressive),
  séries/reps/descanso/observação por exercício e superséries/circuitos.
- **Modelos prontos**: os 14 programas do PWA.
- **Exercícios**: os 901 exercícios com busca (português ou inglês), filtro por grupo, foto animada da execução,
  mapa muscular, vídeos no YouTube, link do seu vídeo e exercícios personalizados.
- **Treino**: tabela de séries com a anterior e a sugestão da progressão automática, RIR, tipos de série (aquecimento,
  drop set, até a falha), aquecimento automático, superséries que levam ao próximo exercício, trocar/reordenar/remover
  exercícios, anotações e tela sempre ligada.
- **Descanso**: barra flutuante com indicador ondulado, notificação com contagem regressiva e botões −15 s / +15 s / Pular,
  alarme exato no fim (com som ou só vibração) e chip na barra de status no Android 16+.
- **Histórico**: mapa de frequência, séries por músculo, sessões por mês, recordes pessoais, conquistas e resumo com confete.
- **Evolução por exercício**: recordes, gráfico (carga máxima, 1RM estimado, volume…) e calculadoras de anilhas,
  aquecimento e 1RM.
- **Dieta**: metas calculadas (Mifflin-St Jeor ou Katch-McArdle) e ajustadas pelo gasto real, busca na tabela TACO,
  porções caseiras, favoritos, refeições prontas, alimentos próprios, código de barras (Open Food Facts), água,
  micronutrientes, últimos 7 dias e, com IA, descrever a refeição ou fotografar o prato ou o rótulo.
- **Corpo**: peso, % de gordura, medidas e IMC com gráficos, meta de peso e mapa dos músculos treinados.
- **Fotos do progresso**: frente, lado e costas; câmera com guia (a foto anterior aparece transparente por cima),
  timer, linha do tempo e antes/depois deslizando ou lado a lado. As fotos ficam só no aparelho.
- **Assistente de treino**: 3 opções de programa pelo seu perfil, sem internet; ou montadas pela IA, que entende
  observações em texto livre.
- **Treinador IA**: relatório da semana (treinos, cargas, volume, dieta e peso) e conversa com respostas em tempo real.
- **Conquistas**: 28 selos (bronze, prata, ouro e especiais), com o progresso de cada um.
- **Imagem para os Stories**: do treino ou da conquista, em cartão, sobre uma foto sua ou como adesivo transparente.
- **Nuvem**: sincronização criptografada de ponta a ponta com o mesmo servidor (Cloudflare Worker) do PWA.
- **Lembretes**: treino do dia, água, refeições, pesagem e backup, agendados no próprio aparelho (sem servidor).
- **Ajustes**: tema do sistema/claro/escuro/preto (OLED), 10 cores com paleta tonal, vibrante ou expressiva,
  cores do papel de parede, opções de treino, chave da IA e backup (exportar com ou sem fotos, compartilhar,
  importar e desfazer).

Os recursos de IA usam o Claude (`claude-opus-5-5`) com a **sua** chave da API da Anthropic, salva só no aparelho
(fica fora do backup do Android). Sem chave, tudo o mais funciona normalmente.

### Testar a sincronização com um servidor local

A versão de debug aceita `http://` para `localhost` e `10.0.2.2` (só nela; a de release exige `https://`).
Com o servidor rodando no computador na porta 8787:

```bash
adb reverse tcp:8787 tcp:8787   # o app acessa o servidor do computador como localhost
```

e use `http://localhost:8787` como endereço em Ajustes → Nuvem e lembretes.

## Estrutura

| Pasta / arquivo | O que é |
|---|---|
| `app/src/main/java/app/ficha/data/` | Modelo de dados (igual ao do PWA), catálogo embutido e gravação no aparelho |
| `app/src/main/java/app/ficha/logic/` | Regras: progressão, volume, recordes, superséries, calculadoras, dieta, corpo, assistente e conquistas |
| `app/src/main/java/app/ficha/ui/` | Telas (Compose), componentes, tema Material 3 Expressive e navegação |
| `app/src/main/java/app/ficha/timer/` | Notificação do descanso, alarme e botões da notificação |
| `app/src/main/java/app/ficha/ai/` | Chamadas ao Claude (SDK Java da Anthropic) e os recursos de IA |
| `app/src/main/java/app/ficha/sync/` | Sincronização criptografada (igual ao `cloud.js`) e lembretes locais |
| `app/src/main/java/app/ficha/photos/`, `share/` | Fotos do progresso e imagens para os Stories |
| `app/src/debug/` | Só na build de debug: permite servidor de sincronização local por `http://` |
| `app/src/main/assets/data/` | Exercícios, modelos, mapa muscular e alimentos em JSON (gerados dos `.js` do PWA) |
| `tools/convert_data.py` | Gera os JSON a partir de `exercises.js`, `templates.js`, `body.js` e `foods.js` |

As fotos dos exercícios não são copiadas para cá: o build pega as da pasta `img/` do repositório.
Depois de mudar o catálogo, os modelos ou os alimentos no PWA, rode na raiz do repositório:

```bash
python3 android/tools/convert_data.py
```
