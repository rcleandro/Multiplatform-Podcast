# ADR 0011 — Sincronização entre aparelhos por pasta

- **Status:** aceita
- **Data:** 2026-10-16
- **Fase:** 19.12 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

A 19.9b sincroniza celular e relógio pela Data Layer do Wear OS, com um merge puro (`planSync` sobre `SyncSnapshot`)
pensado para ser reaproveitado entre quaisquer aparelhos. Faltava o transporte entre celulares, tablets e o Desktop.
O app não tem backend nem contas, e não deve passar a ter só para isso. Os snapshots levam endereços de feeds
privados, com tokens.

## Decisão

**Uma pasta que o próprio usuário já sincroniza** (iCloud Drive, Google Drive, Dropbox, Syncthing, Nextcloud),
escolhida em Configurações (decisão do usuário em 16/10/2026):

- Cada aparelho grava só o próprio arquivo, `podcast-sync-<aleatório>.json`, com o seu `SyncSnapshot`, e aplica os
  arquivos dos outros com o merge da 19.9b (`FolderSyncUseCase`). Dois aparelhos nunca gravam o mesmo arquivo, então
  o app de sincronização nunca tem conflito para resolver. Um arquivo pela metade (ainda chegando) é ignorado até a
  próxima troca; arquivos que não são do app não são lidos.
- A troca acontece quando a pasta é escolhida, a cada 5 minutos e 10 s depois que a biblioteca para de mudar,
  enquanto o app está aberto. O arquivo só é regravado quando muda.
- A pasta é guardada como bookmark do FileKit: permissão persistente do SAF no Android (`takePersistableUriPermission`)
  e caminho no Desktop. Escolher de novo mantém o nome do arquivo deste aparelho; "Parar de sincronizar" esquece a
  pasta e deixa os arquivos lá.
- O mutex de aplicação passou do `WearableSync` para o `LibrarySyncUseCase`, porque no celular relógio e pasta podem
  aplicar ao mesmo tempo.
- **Android e Desktop.** A Web não tem pasta que persista com escrita pelo FileKit (o File System Access só existe nos
  navegadores Chromium e não tem bookmark). O iOS fica para a fase 25, com bookmarks com escopo de segurança e o
  download dos itens do iCloud que ainda não estão no aparelho. Em ambos a linha não aparece.

Alternativas:
- **gPodder (gpodder.net ou Nextcloud):** conversa com outros apps, mas pede conta e servidor (o gpodder.net é
  instável) e a API não tem fila nem remoções datadas, então o merge teria de ser adaptado.
- **Google Drive `appDataFolder`:** sincroniza sem o usuário escolher pasta, mas exige login OAuth, um projeto no
  Google Cloud e a verificação do escopo.
- **Backend próprio:** contas, autenticação, custo e manutenção para um app de um usuário só.

## Consequências

- **Feeds privados:** os tokens vão para a pasta em texto claro. Quem tiver acesso à pasta tem os feeds. Fica a cargo
  do serviço escolhido pelo usuário. A descrição em Configurações ainda não avisa disso; se o app passar a ter outros
  usuários, o aviso entra nela.
- **Nada em segundo plano:** com o app fechado nada é trocado. Se fizer falta, a troca entra no worker da 18.18.
- **Google Drive no Android:** o app do Drive não oferece pastas pelo seletor do sistema (não tem árvore no SAF). No
  Android funciona com Syncthing, Nextcloud, Dropsync, FolderSync e semelhantes, ou com pastas locais que outro app
  sincroniza.
- **Arquivos de aparelhos aposentados ficam na pasta.** Perdem para mudanças mais novas, como qualquer snapshot. Se a
  pasta crescer demais, um aparelho pode apagar os arquivos parados há meses.
- **Sem criptografia própria:** se um dia os snapshots forem cifrados, a chave terá de ir de um aparelho para outro
  fora da pasta (QR code, por exemplo).
