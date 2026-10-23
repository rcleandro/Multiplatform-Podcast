# ADR 0014 — Modelo de ameaças

- **Status:** aceita
- **Data:** 2026-10-23
- **Fase:** 22.1 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

O app não tem conta nem servidor próprio, mas lê feeds arbitrários da internet, guarda URLs de feeds pagos com token
de acesso, expõe um serviço de mídia a outros apps e, no Brasil, trava conteúdo atrás do PIN do responsável (fase 26).
Esta ADR lista o que proteger, de quem e o que já está feito, para que cada item da fase 22 aponte para uma ameaça.
Estado conferido no código em 23/10/2026, com a fase 20 no `main`.

## Ativos

| Id | Ativo | Onde fica | Por que importa |
|---|---|---|---|
| A1 | URL de feed privado (com token ou usuário e senha) | Banco; backup do Android (o banco vai junto); JSON de backup (19.11); arquivo da pasta sincronizada (19.12); Data Layer do relógio (19.9b) | Quem tem a URL ouve o conteúdo pago no lugar do assinante |
| A2 | Biblioteca e hábitos de escuta | Banco (assinaturas, histórico, estatísticas, favoritos) | Revela interesses; no caso de um menor, dado protegido pelo ECA Digital |
| A3 | Episódios baixados | `files/downloads/` | Espaço em disco; nome e conteúdo vêm do feed |
| A4 | PIN do responsável | Preferências, como `sal:sha256(sal:pin)` (26.6) | Destrava conteúdo explícito e as opções que o responsável fecha para um menor |
| A5 | Chave de assinatura do Android | Fora do repositório (`~/keys`) e nos secrets do GitHub (20.6) | Quem tem a chave publica atualizações como se fosse o app |

## Entradas não confiáveis

- **O feed:** XML, títulos, HTML das descrições, URLs de áudio, capa, link, capítulos e transcrição, `guid`, tamanhos
  declarados.
- **Links abertos de outros apps:** `podcast:`, `pcast:`, `itpc:`, `feed:`, `https` com RSS e Apple Podcasts (18.4).
- **Arquivos importados:** OPML (18.13), JSON de backup (19.11) e os arquivos da pasta sincronizada (19.12), que
  outro aparelho, ou quem tiver acesso à pasta, escreveu.
- **Outros apps no aparelho:** o `PodcastMediaService` exportado, os atalhos e os componentes do relógio.
- **A rede:** respostas de qualquer host, inclusive em `http` (ADR 0009).

## Atacantes considerados

1. **Feed malicioso ou comprometido:** controla tudo o que o feed traz.
2. **Rede:** Wi-Fi público ou operadora, que lê e altera o que trafega em `http`.
3. **Outro app instalado:** sem permissões especiais, fala com os componentes exportados.
4. **Quem tem acesso a uma cópia dos dados:** a conta de nuvem do backup, a pasta sincronizada ou o arquivo de backup
   enviado a alguém.
5. **O menor com o aparelho na mão:** tenta passar pelo PIN ou pelos controles da fase 26.

Fora do escopo: aparelho com root ou desbloqueado (quem tem root lê o banco), malware com acessibilidade ou
sobreposição de tela, e quem apaga os dados do app ou o desinstala. O controle dos pais que resiste a isso é o do
sistema (Family Link, Tempo de Uso); o do app é uma camada a mais.

## Ameaças

| Id | Ameaça | Atacante | Hoje | Item |
|---|---|---|---|---|
| T1 | Outro app navega na biblioteca (títulos de feeds pagos, histórico) e controla o player | 3 | Aberto: qualquer controlador recebe todos os comandos de biblioteca e de player (`sessionCommandsWith`, 20.7) | 22.2 |
| T2 | Feed enorme ou lento esgota memória ou trava a atualização | 1 | Aberto: `bodyAsText()` lê o corpo inteiro, sem limite; há só os timeouts do cliente (16.5) | 22.3 |
| T3 | Bomba de entidades ou entidade externa no XML | 1 | A conferir: o `KtXmlReader` não busca entidades externas e deixa as desconhecidas como texto; falta um fixture que prove que entidades declaradas no DTD não se expandem | 22.3 |
| T4 | URL de esquema perigoso (`file:`, `content:`, `javascript:`, `intent:`) no áudio, na capa, no link ou numa descrição | 1 | Aberto: as URLs são usadas como vêm; o `HtmlText` vira qualquer `href` em link | 22.3, 22.4 |
| T5 | Travessia de diretório ou arquivo enganoso no download | 1 | Fechado em parte: nome por hash (14.3); cota de disco (14.8); falta conferir tipo e tamanho declarados | 22.5 |
| T6 | Token de feed pago lido de uma cópia dos dados | 4 | Aberto: a URL completa vai para o banco, o backup do Android, o JSON de backup, a pasta sincronizada e o relógio | 22.6, 22.7 |
| T7 | Token de feed pago em log, telemetria ou denúncia | 4 | Fechado: URLs cortadas no host em log, Crashlytics e Analytics (16.1) e na denúncia (26) | — |
| T8 | Rede lê ou altera feed e áudio em `http` | 2 | Aceito em parte: `https` primeiro e `http` só se ele falhar (ADR 0009); sem pinning, porque os hosts são arbitrários | 22.8 |
| T9 | Release depurável, com logs de debug ou componente exportado à toa | 3 | Fechado em parte: R8 com ofuscação (20.1); logs a partir de info na release no Android e no iOS (16.2). Falta revisar cada componente exportado, e o Desktop e a Web logam debug sempre (`isDebugBuild = true`), inclusive nos pacotes da 20.6 | 22.9 |
| T10 | Dependência vulnerável ou segredo commitado | — | Fechado em parte: Dependabot (10.6) e `*.jks` no `.gitignore`; falta CodeQL e varredura de segredos | 22.10 |
| T11 | O menor descobre o PIN do responsável tentando | 5 | Aberto: PIN de 4 dígitos sem limite de tentativas; 10.000 combinações cabem numa tarde | 22.11 |
| T12 | Arquivo importado (OPML, backup, pasta sincronizada) com URLs hostis ou tamanho absurdo | 1, 4 | Aberto: o que vem do arquivo passa pelos mesmos caminhos do feed | 22.3 |

## Decisões

- **Sem pinning de certificado:** os hosts são os de cada podcast, arbitrários; pinning quebraria qualquer feed sem
  proteger nenhum ativo.
- **`http` continua como queda** (ADR 0009). A 22.8 revê se o áudio e as capas podem exigir `https` sem perder os
  podcasts antigos, mas não proíbe o texto claro.
- **O PIN não vira senha forte:** quatro dígitos são o que um responsável lembra. A defesa é o limite de tentativas
  com espera crescente (22.11), não um PIN maior.
- **Segredo fora do banco** (22.6): o token sai da URL guardada e vai para o Keystore (Android) e o Keychain (iOS). O
  banco, o backup, a sincronização e o relógio levam só a URL sem o segredo e uma referência; restaurar num aparelho
  novo pede o token de novo.

## Consequências

- Cada item da fase 22 aponta para uma linha da tabela de ameaças, e a fase termina quando nenhuma ameaça ficar
  "Aberto".
- Rever esta ADR quando o app ganhar conta, servidor ou proxy (ADR 0013), ou quando um ativo novo entrar.
