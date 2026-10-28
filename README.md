# Launcher do Leandro

Launcher próprio para Android TV / Google TV (TCL OLED e Xiaomi Mi Stick).
Leve: sem anúncios, sem serviços em segundo plano e só 2 bibliotecas (AndroidX Core e RecyclerView).

## O que ele mostra direto na tela inicial

- Saudação com seu nome (Bom dia / Boa tarde / Boa noite)
- Relógio com segundos e data completa: dia da semana, dia, mês e ano
- **Painel de clima** com temperatura atual, sensação térmica, umidade, vento, máxima/mínima de hoje,
  chance de chuva e previsão dos próximos 5 dias (dados do Open-Meteo, gratuito e sem chave de API;
  atualiza a cada 30 minutos e precisa de internet na TV)
- Atalhos do sistema em botões arredondados: Configurações, Wi-Fi, Bluetooth, Tela e **Trocar fundo**
- Fileira de **Favoritos** ★ (segure OK em qualquer app para favoritar ou desfavoritar)
- Fileira **Apps de TV** (com os banners originais dos apps)
- Fileira **Outros apps**

Os ícones seguem o estilo de TV moderno: cartões arredondados com brilho no topo e que crescem
e ganham sombra quando você passa o controle sobre eles, com o nome do app ficando mais forte.
- 6 fundos: OLED preto (ideal para a TCL), Noite, Aurora, Pôr do sol, Floresta e Grafite

A lista de apps se atualiza sozinha quando você instala ou remove algo.

## Como gerar o APK

1. Instale o **Android Studio** no computador.
2. Abra a pasta `LeandroLauncher` (File > Open) e espere o Gradle sincronizar.
3. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   Para a versão mais leve: **Build > Select Build Variant** e escolha `release`.
4. O APK fica em `app/build/outputs/apk/`.

## Como instalar nas TVs

- **Pelo celular/PC:** com o depuramento USB/rede ligado na TV: `adb connect IP_DA_TV` e depois `adb install app-release.apk`
- **Por pen drive:** copie o APK e abra com um gerenciador de arquivos na TV (ex.: "File Commander" ou "X-plore").
- **Por rede:** app "Send Files to TV" (celular e TV na mesma rede Wi-Fi).

Depois de instalado, ele aparece na lista de apps como **Launcher do Leandro**.

## Como deixar como tela inicial (botão Home)

1. Ao apertar Home, a TV pode perguntar qual tela inicial usar. Escolha o Launcher do Leandro.
2. Se não perguntar, o launcher original é fixo. A forma mais segura é desativá-lo por ADB (reversível):
   - Android TV: `adb shell pm disable-user --user 0 com.google.android.tvlauncher`
   - Google TV: `adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx`
   - Para voltar atrás: o mesmo comando trocando `disable-user` por `enable`.
   - Confira o nome do pacote com `adb shell pm list packages | grep launcher`.
3. Teste primeiro abrindo o launcher como um app normal, antes de desativar o original.

## Como personalizar

| O que mudar | Onde |
|---|---|
| Seu nome na saudação | `app/src/main/res/values/strings.xml` (`owner_name`) |
| Cidade do clima | `strings.xml` (`city_name`, `city_latitude`, `city_longitude`) |
| Formato da data | `res/layout/activity_main.xml` (`format24Hour` do TextClock `date`) |
| Cores dos fundos / novos fundos | `MainActivity.kt`, lista `themes` |
| Atalhos do topo | `MainActivity.kt`, função `buildShortcuts()` |
| Tamanho dos cartões (160x90) | `res/layout/item_app.xml` |
| Cores de destaque e brilho do foco | `res/drawable/card_bg.xml` e `chip_bg.xml` |
| Ícone e banner | `res/drawable/ic_launcher.xml` e `banner.xml` |

## Observação

O projeto foi escrito sem poder compilar no ambiente em que foi gerado (não há Android SDK lá).
Se o Android Studio apontar algum erro na primeira sincronização, é só copiar a mensagem que eu corrijo.
