# ADR 0010 — Vídeo dos episódios

- **Status:** aceita
- **Data:** 2026-10-16
- **Fase:** 19.10 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

Alguns feeds publicam episódios em vídeo (`<enclosure type="video/mp4">`). Os players já tocavam o som desses
episódios, mas a tela mostrava só a capa. O tipo do enclosure não era guardado (`AudioMimeType.kt` adivinhava pelo
endereço para o Chromecast).

As features não podem depender do `:core:player` (ADR 0003), e é ele quem tem o player nativo de cada plataforma.

## Decisão

- **Tipo guardado:** `Episode.mediaType` (coluna `mediaType`, banco v27) vem do `type` do enclosure; `isVideo` é
  `video/*`. Episódios salvos antes ficam sem tipo até o feed ser lido de novo.
- **Slot no player:** `PlayerScreen` recebe um `VideoSurface?` (`@Composable (Modifier) -> Unit`). O `:shared`, que
  conhece o `:core:player`, passa o de cada plataforma (`platformVideoSurface`). Num episódio de vídeo, o vídeo ocupa
  o lugar da capa, em 16:9, em todos os layouts que mostram a capa (a tela de fora do flip continua só com controles).
- **Android:** `ContentFrame` do `media3-ui-compose` (nova dependência, `@UnstableApi` como o Cast) sobre o
  `MediaController`, que entrega a superfície ao ExoPlayer do serviço. Enquanto o vídeo toca a tela fica ligada, e
  sair do app (`onUserLeaveHint`) entra em picture-in-picture; em PiP o player mostra só o vídeo
  (`LocalPictureInPicture`). A activity trata as mudanças de tamanho (`configChanges`) para não recomeçar na
  transição. Num Chromecast, o item leva o tipo `video/*` e o receptor mostra o vídeo.
- **Web:** o `<audio>` virou `<video>` (toca os dois). Fica escondido na página e o `HtmlElementView` o move para
  dentro do player; ao sair volta para o lugar escondido na mesma tarefa, porque um elemento tirado da página pausa.
  O PiP é o do navegador (menu de contexto ou o automático do Chrome ao trocar de aba).
- **Desktop:** `MediaView` do JavaFX num `JFXPanel` (nova dependência `javafx-swing`, exposta pelo `:core:player`
  como `api`) dentro de um `SwingPanel`. `Platform.setImplicitExit(false)`: fechar o painel, única janela JavaFX, não
  pode encerrar o toolkit e o episódio. Sem PiP: a janela do app já se redimensiona.
- **iOS:** fica com o som e a capa; `AVPlayerLayer` e o PiP do AVKit vêm com o player em SwiftUI (fase 25).

Alternativas:
- **Vídeo numa tela própria:** mais uma rota, e o player perderia os controles que já tem (velocidade, capítulos,
  timer). O slot reaproveita tudo.
- **Desktop com VLCJ:** toca mais formatos, mas exige o VLC instalado; o JavaFX já está no app.

## Consequências

- `PlayerSurface` usa `SurfaceView`: o `clip` do Compose não arredonda o vídeo, por isso ele não tem cantos.
- O `SwingPanel` é pesado: nada do Compose é desenhado por cima do vídeo no Desktop.
- O PiP do Android usa 16:9 fixo; um vídeo vertical aparece com faixas. Se aparecer, usar `player.videoSize`
  (limitado à faixa que o Android aceita, de 1:2,39 a 2,39:1).
- Remover o `media3-ui-compose` ou o `javafx-swing` pede outra forma de mostrar o vídeo na plataforma.
