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

Programas, fichas, histórico, exercícios personalizados e vídeos são usados pelo app. Dieta, medidas e o resto
que ainda não foi portado ficam guardados e voltam no backup exportado pelo Android.

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
- **Histórico**: mapa de frequência, séries por músculo, sessões por mês, recordes pessoais e resumo com confete.
- **Evolução por exercício**: recordes, gráfico (carga máxima, 1RM estimado, volume…) e calculadoras de anilhas,
  aquecimento e 1RM.
- **Ajustes**: tema do sistema/claro/escuro/preto (OLED), 10 cores com paleta tonal, vibrante ou expressiva,
  cores do papel de parede, opções de treino e backup (exportar, compartilhar, importar e desfazer).

Ainda não portado: Dieta, Corpo (medidas e fotos), Assistente de treino, Treinador IA, Conquistas, imagem para os Stories
e Nuvem/lembretes.

## Estrutura

| Pasta / arquivo | O que é |
|---|---|
| `app/src/main/java/app/ficha/data/` | Modelo de dados (igual ao do PWA), catálogo embutido e gravação no aparelho |
| `app/src/main/java/app/ficha/logic/` | Regras: formatação, progressão, volume, recordes, superséries, calculadoras e ações |
| `app/src/main/java/app/ficha/ui/` | Telas (Compose), componentes, tema Material 3 Expressive e navegação |
| `app/src/main/java/app/ficha/timer/` | Notificação do descanso, alarme e botões da notificação |
| `app/src/main/assets/data/` | Exercícios, modelos, mapa muscular e alimentos em JSON (gerados dos `.js` do PWA) |
| `tools/convert_data.py` | Gera os JSON a partir de `exercises.js`, `templates.js`, `body.js` e `foods.js` |

As fotos dos exercícios não são copiadas para cá: o build pega as da pasta `img/` do repositório.
Depois de mudar o catálogo, os modelos ou os alimentos no PWA, rode na raiz do repositório:

```bash
python3 android/tools/convert_data.py
```
