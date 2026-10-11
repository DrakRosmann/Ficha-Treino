# Publicar o Ficha na Play Store

Tudo o que a Play Console pede está nesta pasta. Siga a ordem: as etapas 1 a 3 são feitas uma vez só.

| Arquivo | Onde usar na Play Console |
|---|---|
| `icon-512.png` | Página da loja → Ícone do app (512 × 512) |
| `feature-graphic.png` | Página da loja → Recurso gráfico (1024 × 500) |
| `screenshots/phone/*.png` | Página da loja → Capturas de tela do telefone (na ordem dos números) |
| `screenshots/tablet/*.png` | Página da loja → Tablet de 7 e 10 polegadas |
| `listing.md` | Página da loja → nome, descrição breve e descrição completa |
| `data-safety.md` | Conteúdo do app → Segurança dos dados (respostas prontas) |
| `../../privacidade.html` | Política de privacidade (endereço público, veja a etapa 2) |

## 1. Conta e app

1. Crie a conta de desenvolvedor em [play.google.com/console](https://play.google.com/console) (taxa única de US$ 25).
   Contas pessoais novas precisam de um **teste fechado com pelo menos 12 testadores por 14 dias** antes de liberar
   a produção — comece por ele.
2. **Criar app**: nome "Ficha", idioma padrão Português (Brasil), App, Gratuito.
3. O pacote é **`io.github.drakrosmann.ficha`** e é definido pelo primeiro arquivo enviado. Não dá para mudar depois.

## 2. Política de privacidade

O arquivo `privacidade.html` fica na raiz do repositório. Quando ele estiver na branch `main`, o GitHub Pages publica em:

**https://drakrosmann.github.io/Ficha-Treino/privacidade.html**

Use esse endereço em *Conteúdo do app → Política de privacidade* e no AdMob.

## 3. Assinaturas, anúncios e servidor

Veja `android/README.md` → *Para publicar* (assinaturas `ficha_apoio` e `ficha_ia`, AdMob, IDs em
`local.properties`) e `server/README.md` → *IA para assinantes*. **Sem os IDs reais do AdMob, a release sai com
anúncios de teste** — serve para o teste fechado, mas não para a produção.

## 4. Gerar o arquivo para enviar (.aab)

```bash
cd android
./gradlew bundleRelease
```

O arquivo sai em `app/build/outputs/bundle/release/app-release.aab`, assinado com a **chave de envio**.

**Antes de cada nova versão**, aumente `versionCode` (1, 2, 3…) e, se quiser, `versionName` em `app/build.gradle.kts`:
a Play recusa um arquivo com o mesmo `versionCode` de um já enviado.

### A chave de envio

- Fica em `~/.android-keys/ficha-upload.jks`, com as senhas em `~/.android-keys/keystore.properties`
  (e uma cópia em `android/keystore.properties`, que o git ignora). **Faça backup dessa pasta** num lugar seguro
  (gerenciador de senhas, pendrive) e nunca a coloque no GitHub.
- Na primeira publicação, aceite o **Play App Signing**: o Google guarda a chave definitiva do app, e a sua chave só
  serve para enviar. Se ela se perder, dá para pedir uma nova ao suporte da Play.

## 5. Formulários de "Conteúdo do app"

| Formulário | Resposta |
|---|---|
| Acesso ao app | Todas as funções estão disponíveis sem login. A IA exige a assinatura Ficha IA (informe uma conta de teste de licença, se pedirem). |
| Anúncios | **Sim, o app contém anúncios** |
| Classificação de conteúdo | Questionário IARC: categoria "Referência, saúde e fitness"/utilitário, sem violência, sem conteúdo sexual, sem apostas, sem compartilhamento de localização. Resulta em "Livre". |
| Público-alvo | 18 anos ou mais (evita as exigências de apps para crianças, por causa dos anúncios) |
| Apps de saúde | Marque "Fitness e exercícios" e "Nutrição e controle de peso". Não é dispositivo médico. |
| Segurança dos dados | Veja `data-safety.md` |
| ID de publicidade | **Sim**, usado para publicidade (AdMob) |
| Alarmes exatos (`USE_EXACT_ALARM`) | Função principal: **cronômetro/timer** — o descanso entre as séries precisa tocar na hora exata, com a tela apagada |
| Notificações promovidas (Live Updates) | Contagem do descanso em andamento, iniciada pelo usuário |

## 6. Enviar

1. *Testar → Teste fechado → Criar versão* → envie o `.aab` → notas da versão (ex.: "Primeira versão").
2. Adicione os testadores (lista de e-mails) e mande o link de participação.
3. Depois dos 14 dias com 12 testadores, peça o acesso à **produção** e promova a versão.

As compras só funcionam em versões instaladas pela Play Store. Cadastre o seu e-mail em
*Configuração → Testes de licença* para comprar sem ser cobrado.
