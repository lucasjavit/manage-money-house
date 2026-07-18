# Money Ingest — app Android

Captura notificações de transação dos apps de banco e envia o **texto cru** para o backend
(`/api/ingest`). A IA no backend (Claude) extrai valor, estabelecimento e categoria — o app **não**
faz parsing por banco.

Privacidade: a notificação vai do celular direto para o Pi na sua rede; nada passa por terceiros.

## Como funciona

1. Uma notificação de banco chega no celular.
2. O `NotificationListenerService` captura o texto e abre a tela de classificação.
3. Você escolhe **"Meu gasto (Lucas)"** ou **"Gasto da casa"** (e, se casa, o tipo).
4. A transação vai para uma fila local (Room) e o `SyncWorker` envia ao backend quando há rede.
   Fora de casa, fica na fila e sincroniza ao reconectar ao Wi-Fi.

## Como obter o APK (sem PC)

O APK é compilado no **GitHub Actions** (o Pi não compila Android).

1. No GitHub, aba **Actions** → workflow **Build Android APK** → **Run workflow** (ou dê push em
   `android/**`).
2. Ao terminar, baixe o artifact **money-ingest-debug** — dentro há `app-debug.apk`.
   - Alternativa: crie uma tag `v0.1` → o APK é anexado a uma **Release** para baixar direto no
     navegador do celular.

## Instalar no celular

1. Baixe o `app-debug.apk` pelo navegador do celular.
2. Permita "instalar de fontes desconhecidas" quando pedir.
3. Abra o app → **Configuração**:
   - URL do backend: `http://192.168.1.20:8081` (já vem preenchido)
   - Token: cole o valor de `ingest.token` (veja em Configurações do sistema web, ou no banco:
     `SELECT value FROM configurations WHERE key='ingest.token'`)
   - **Salvar configuração**
4. Toque em **Habilitar leitura de notificações** e ative o app na lista do Android.
5. Desative a otimização de bateria para o app (senão o Android pode matar o listener).
6. Toque em **Notificação de teste** para validar o fluxo ponta a ponta sem gasto real.

## Notas

- `minSdk 26` (Android 8+). APK **debug** (assinado com a debug keystore) — suficiente para sideload.
- Apps monitorados (packageNames) têm um default editável em `SettingsStore.DEFAULT_PACKAGES`.
  Confirme os nomes reais dos apps do seu celular.
- Se a IA estiver sem chave/crédito no backend, a transação é gravada como `needs_review` (capturada,
  sem perder), só sem extração automática.
