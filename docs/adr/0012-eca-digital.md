# ADR 0012 — Enquadramento no ECA Digital

- **Status:** aceita
- **Data:** 2026-10-19
- **Fase:** 26.1 do [roadmap](../ROADMAP_MELHORIAS.md)

## Contexto

O Estatuto Digital da Criança e do Adolescente (Lei 15.211/2025) está em vigor desde 17/03/2026, regulamentado pelo
Decreto 12.880/2026 e fiscalizado pela ANPD. Vale para quem usa o app no Brasil, onde quer que o app seja feito.

**Fontes lidas em 19/10/2026:**
- O texto da lei no Planalto, já com a vigência dada pela Lei 15.352/2026.
- O Decreto 12.880, de 18/03/2026.
- A página da ANPD sobre o ECA Digital. As orientações de aferição de idade continuam **preliminares**: a de março
  de 2026 e a proposta de atualização de maio, cuja consulta terminou em 09/07/2026. O guia de escopo também segue em
  consulta. Nenhum guia definitivo saiu até essa data.
- A política do Google Play para o ECA Digital e as notas da Play Age Signals API: sinal para o Brasil desde
  17/03/2026; a versão 0.0.4 tem `requestAgeSignalsAccess()` e `checkAgeSignals()`.
- O anúncio da Apple de 24/02/2026 sobre a Declared Age Range no Brasil: em beta, e a faixa só vem quando a pessoa,
  ou o responsável, aceita compartilhar.

**A fiscalização da ANPD tem três etapas:**
1. **Etapa I (março de 2026):** lojas e sistemas operacionais.
2. **Etapa II (desde agosto de 2026):** orientações definitivas e adaptação até novembro.
3. **Etapa III (janeiro de 2027):** fiscalização ativa, com foco em aferição de idade.

O decreto (art. 40) só pune o descumprimento recorrente.

**O app hoje:**
- Não tem contas, publicidade, compras nem conteúdo de usuários.
- Busca no diretório da Apple, mostra populares e sugestões (19.6), e toca o que os feeds publicam.
- O único marcador de conteúdo adulto é o `itunes:explicit` do feed, que "Mostrar conteúdo explícito" (18.5) já
  filtra na busca.
- A telemetria (16.4) vem ligada.
- A fila (18.6) passa sozinha para o próximo episódio.
- As notificações de episódio novo (18.18) vêm ligadas.

Não há parecer jurídico: o projeto não tem advogado. As decisões abaixo são do dono do projeto, tomadas em
19/10/2026.

## Decisão

### O app está no escopo

O art. 1º alcança produtos "de acesso provável" por crianças e adolescentes. O parágrafo único define acesso provável
por atratividade, facilidade de acesso e risco. Um app de podcasts genérico, gratuito e nas lojas atende os dois
primeiros critérios. Ele também não é "serviço com controle editorial" (art. 2º, IX), que dispensaria parte das
obrigações (art. 39, § 1º): o app mostra o que diretórios e feeds de terceiros publicam, por busca e rankings.

### O que vale e o que não vale

| Obrigação | Vale? | Onde entra |
|---|---|---|
| Impedir o acesso de menores a conteúdo impróprio, sem autodeclaração (lei, art. 9º; decreto, art. 17) | Sim | 26.3 |
| Receber o sinal de idade e ter os próprios cuidados (lei, art. 14; decreto, arts. 25 e 26) | Sim | 26.2 |
| Configuração mais protetora por padrão, com explicação para mudar (lei, arts. 7º e 8º, IV) | Sim | 26.4 |
| Evitar uso compulsivo: reprodução automática, notificações (lei, arts. 8º, IV e 17, § 4º, II; decreto, art. 9º) | Sim | 26.5 |
| Ferramentas de supervisão parental e limite de tempo de uso (lei, arts. 17 e 18) | Sim, no que couber a um app sem contas | 26.6 |
| Canal de notificação de violações (lei, art. 28; decreto, art. 41) e retirada sem ordem judicial (lei, art. 29) | Sim. "Retirar" é esconder do app, porque o conteúdo é do feed | 26.7 |
| Informações sobre riscos e medidas, em português (lei, art. 16) | Sim | 26.8 |
| Contas vinculadas a responsáveis (art. 24), redes sociais (art. 25), loot boxes (art. 20), publicidade (arts. 22 e 26) | Não: o app não tem nada disso | — |
| Relatório semestral (art. 31) | Não: só acima de 1 milhão de usuários registrados, e o app não tem cadastro | — |
| Comunicar exploração e abuso detectados (art. 27) | Só se o app receber uma notificação disso, pelo canal da 26.7 | 26.7 |
| Representante legal no país (art. 40) | Fora do código; o dono do projeto está no Brasil | — |

### Conteúdo impróprio

**Todo episódio ou podcast com `itunes:explicit` conta como impróprio para menores.** O art. 9º, § 2º da lei fala de
material pornográfico e do que a lei proíbe, e a tag cobre também o simples palavrão. Mesmo assim, ela é o único
sinal que o app tem, e tratar tudo como impróprio é o caminho protetor.

### Quem está no Brasil

O app trata o usuário como estando no Brasil se **qualquer uma** das fontes do aparelho apontar para o Brasil, que é
a leitura protetora:
- **Android:** o país da rede ou do chip (`TelephonyManager`) e, sem eles, o do idioma do sistema.
- **iOS:** o país da conta da App Store (`Storefront.countryCode`) e o do idioma do sistema.
- **Desktop e Web:** o país do idioma do sistema ou do navegador (`deviceCountry()`).

Fora do Brasil, o app continua como é.

### As três faixas: adulto, menor, desconhecido

Um `AgeSignal` do domínio tem três valores:

| Faixa | De onde vem | O que muda no Brasil |
|---|---|---|
| **Adulto** | Play Age Signals ou Declared Age Range informam 18 anos ou mais | Nada: o app é o de hoje |
| **Menor** | O sinal informa menos de 18 anos | 1. Explícito escondido em todo lugar e sem tocar, com a opção travada (26.3).<br>2. Telemetria e relatórios de erro desligados e fora da tela (26.4).<br>3. A fila não passa sozinha para o próximo episódio.<br>4. Notificações de episódio novo desligadas.<br>5. Aviso de tempo de uso (26.5).<br>Os itens 3 e 4 só mudam pelas salvaguardas dos responsáveis (26.6). |
| **Desconhecido** | Sem sinal: Desktop, Web, aparelho sem conta, sinal recusado ou indisponível | Explícito escondido e travado, como para menor: o decreto (art. 17) não aceita autodeclaração para liberar. Telemetria, notificações e a fila automática **começam desligadas** (art. 7º), mas um adulto pode ligar nas Configurações, com a explicação do que muda |

- **Conflito de sinais:** se o sinal da loja e o que o app sabe divergirem, vale o mais protetor (decreto, art. 25,
  § 4º).
- **Quando o sinal é lido:** ao abrir o app e quando ele volta ao primeiro plano.
- **Uso da faixa:** fica só em memória e serve só para isso (lei, art. 13). Não vai para a telemetria, o backup nem a
  sincronização.
- **Instalações antigas:** quem já usa o app no Brasil e cai em "desconhecido" mantém as escolhas que já fez. O
  padrão protetor vale para instalações novas e para tudo o que ainda não foi escolhido. O explícito é a exceção:
  fica travado mesmo para quem já tinha ligado.

### Alternativas

- **Sem sinal = menor:** o mais protetor, mas todo usuário de Desktop e Web no Brasil ficaria sem telemetria, sem
  notificações e sem fila automática, sem como mudar.
- **Sem sinal = adulto:** o app ficaria como é, contrariando o padrão protetor (art. 7º) e o art. 17 do decreto.
- **Verificação de idade própria** (documento, selfie, terceiro certificado): desproporcional para um app sem contas
  (decreto, art. 24, I), e contrária à minimização de dados. Fica para o caso de a ANPD exigir de apps de mídia.

## Consequências

- **No Brasil sem sinal, quem quer conteúdo explícito não tem como liberar.** Isso vale para todo Desktop e toda Web,
  e para quem não compartilha a faixa na loja. Reavaliar quando houver um mecanismo de verificação aceito pela ANPD
  e disponível nessas plataformas.
- **Padrões protetores para adultos no Brasil também:** o que ninguém escolheu começa desligado assim que o app abre
  no Brasil, antes de o sinal de idade chegar. Um adulto no Brasil começa com telemetria e notificações desligadas e
  pode ligar nas Configurações (26.4).
- **A Play Age Signals está indo para o mundo todo até o fim de 2026.** O sinal deixa de indicar Brasil, por isso o
  país sai das fontes do aparelho, não do sinal.
- **A Declared Age Range no Brasil está em beta e depende de consentimento.** Até o iOS ganhar a interface em SwiftUI
  (fase 25), o sinal entra pela ponte Swift do app.
- **Orientações definitivas da ANPD:** quando saírem os guias de escopo e de aferição de idade, revisar esta ADR,
  principalmente o tratamento de "desconhecido" e se o `itunes:explicit` basta.
- **Parecer jurídico:** se o app for publicado para outras pessoas, um parecer deve confirmar o enquadramento antes
  de janeiro de 2027 (etapa III da fiscalização).
