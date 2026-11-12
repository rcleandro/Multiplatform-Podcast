# Política de privacidade e termos de uso

Última atualização: 10 de novembro de 2026.

Este é um app de podcasts para Android, iOS, computador e navegador. Ele não tem contas, cadastro, publicidade,
compras nem conteúdo publicado por usuários. Esta página diz o que o app guarda, o que ele envia e para quem, e o que
muda para crianças e adolescentes no Brasil.

## Quem é o responsável

O app é feito e mantido por **Leandro Carvalho**, desenvolvedor independente (pessoa física), que é o controlador
dos dados pessoais tratados pelo app, nos termos da Lei Geral de Proteção de Dados (LGPD, Lei 13.709/2018). Como
agente de tratamento de pequeno porte (Resolução CD/ANPD nº 2/2022), não há encarregado nomeado; o canal para falar
sobre os seus dados está em [Contato](#contato).

## O que fica só no seu aparelho

Tudo o que você faz no app fica no aparelho e não vai para nenhum servidor nosso, porque não temos servidor:

- os podcasts que você assina, os episódios, o progresso, a fila, os marcadores, as filas inteligentes e as
  estatísticas de escuta;
- os episódios baixados;
- as suas escolhas nas Configurações;
- o PIN dos responsáveis, guardado só como um código embaralhado (hash com sal), nunca os dígitos;
- os podcasts e episódios que você escondeu depois de uma denúncia.

Os endereços de feeds privados podem ter uma senha embutida. Eles ficam no aparelho e nunca são enviados para análise
de uso nem relatórios de erro; nos relatórios de erro, desses endereços aparece só o nome do servidor (por exemplo,
`feeds.exemplo.com`), para saber qual servidor falhou.

## O que o app envia e para quem

- **Aos servidores dos podcasts:** para atualizar um feed e tocar ou baixar um episódio, o app pede o arquivo ao
  servidor de quem publica o podcast, como um navegador faria. Esse servidor vê o endereço IP do aparelho.
- **À Apple (diretório de podcasts):** a busca, os mais ouvidos, as categorias e as sugestões vêm da busca pública do
  iTunes. A Apple recebe o termo buscado ou a categoria e o país, sem nenhuma identificação sua.
- **Ao Google Firebase (análise de uso e relatórios de erro), só no Android e no iOS e só se ligado:** veja a seção
  seguinte.
- **Ao serviço de sincronização que você escolher:** se você ligar "Sincronizar por pasta", o app grava a biblioteca,
  o progresso e a fila, com os endereços dos feeds, numa pasta que você escolhe (iCloud Drive, Dropbox, Syncthing ou
  outra). Quem guarda essa pasta é o serviço que você usa.
- **Ao seu app de e-mail:** uma denúncia abre o seu app de e-mail com o motivo, os títulos do podcast e do episódio e
  o nome do servidor do feed. Ela só é enviada se você mandar o e-mail.
- **Backup:** "Exportar backup" cria um arquivo que fica onde você salvar. O app não o envia a lugar nenhum.
- **Backup do Android:** se o backup estiver ligado no aparelho, o Android copia a biblioteca, o progresso e as
  configurações (não os episódios baixados) para a sua conta Google, só quando esse backup é criptografado de ponta a
  ponta com o bloqueio de tela, de modo que nem o Google consegue lê-lo. Sem essa criptografia, o app fica fora do
  backup. Em Android 8, que não oferece essa opção, o backup segue as regras do sistema.

## Análise de uso e relatórios de erro (Google Firebase)

Só no Android e no iOS, e só com a opção "Enviar relatórios de uso e de falhas" ligada em Configurações › Privacidade.
No Brasil, ela começa desligada até alguém escolher, e para menores fica sempre desligada.

- **Análise de uso (Firebase Analytics):** o nome das ações que você faz no app, como tocar um episódio, buscar ou
  mudar a velocidade, às vezes com uma contagem (por exemplo, quantos resultados a busca trouxe). Nunca qual podcast
  ou episódio, o título, o termo buscado, a categoria escolhida nem o endereço do feed. O próprio Firebase junta a isso
  um identificador aleatório da instalação do app, o modelo e o sistema do aparelho, a versão do app, o idioma e o
  país ou a região, deduzidos do endereço IP.
- **Relatórios de erro (Firebase Crashlytics):** quando o app trava ou encontra um erro, o tipo do erro, a linha do
  código onde aconteceu, o estado do aparelho (memória, espaço livre, orientação da tela), um identificador aleatório
  da instalação e as últimas mensagens internas do app, que podem trazer o nome do servidor de um feed que falhou.
- **O que não vai:** nome, e-mail, conta, localização precisa, contatos, a sua biblioteca ou o histórico do que você
  ouviu. O app não usa identificador de publicidade e não rastreia você em outros apps.

Ao desligar a opção, o app apaga os relatórios ainda não enviados e troca o identificador da análise de uso, para
que nada do que vier depois se ligue ao que já foi enviado. O que já tinha sido enviado expira no prazo abaixo.

## Por que o app trata esses dados (bases legais)

- **Análise de uso e relatórios de erro:** o seu consentimento (LGPD, art. 7º, I), dado ao ligar a opção e revogável
  a qualquer momento ao desligá-la. Servem para saber quais funções são usadas e para corrigir falhas.
- **Pedidos aos servidores dos podcasts e à busca da Apple:** são feitos porque você pediu (adicionar, atualizar,
  tocar, baixar ou buscar), para o app funcionar (art. 7º, V).
- **Faixa de idade e país, no Brasil:** para cumprir o ECA Digital (art. 7º, II, e art. 14), só para decidir as
  proteções descritas abaixo.
- **Denúncias e pedidos sobre os seus dados por e-mail:** para cumprir o ECA Digital e a LGPD (art. 7º, II) e
  responder a você.

## Por quanto tempo

- **No seu aparelho:** até você apagar, remover o app ou limpar os dados dele.
- **Análise de uso:** 2 meses no Google Analytics, contados do envio, depois apagada pelo Google.
- **Relatórios de erro:** 90 dias no Firebase Crashlytics.
- **E-mails de denúncia e de pedidos sobre dados:** o tempo necessário para responder e para cumprir a lei.

## Transferência para fora do Brasil

O Google (Firebase) e a Apple (busca do diretório) guardam os dados em servidores fora do Brasil, principalmente nos
Estados Unidos. No caso do Firebase, a transferência segue os Termos de Tratamento de Dados do Google Analytics e os
Termos de Processamento e Segurança de Dados do Firebase, aceitos pelo responsável (LGPD, art. 33, II). Os dados não
são compartilhados com outros produtos do Google nem usados para publicidade. A busca do diretório é feita direto do seu aparelho para a Apple, que
trata esses dados sob a política de privacidade dela.

## Os seus direitos

A LGPD (art. 18) garante a você: saber se os seus dados são tratados e ter acesso a eles; corrigi-los; pedir a
anonimização, o bloqueio ou a eliminação do que for desnecessário ou excessivo; a portabilidade; saber com quem são
compartilhados; ser informado sobre a possibilidade de não consentir e o que isso implica; revogar o consentimento; e
pedir a eliminação dos dados tratados com o seu consentimento.

Como quase tudo fica no seu aparelho, a maior parte disso você faz direto no app:

- **acesso e portabilidade:** Configurações › "Exportar backup" ou "Exportar OPML";
- **correção e eliminação:** edite ou remova os podcasts, ou limpe os dados do app, o que apaga tudo;
- **revogar o consentimento:** desligue a análise de uso em Configurações › Privacidade.

Para o resto, ou para qualquer dúvida, use o contato abaixo. Os dados do Firebase não trazem o seu nome nem um
contato, só um identificador aleatório que o app troca quando você desliga a opção; por isso, a partir de um e-mail,
não é possível localizá-los nem dizer quais são seus. Você também pode reclamar à Autoridade Nacional de Proteção de
Dados (ANPD), em gov.br/anpd.

## Crianças e adolescentes no Brasil (ECA Digital)

O app segue o Estatuto Digital da Criança e do Adolescente (Lei 15.211/2025) para quem está no Brasil. Fora do Brasil,
o app funciona como descrito acima.

**Que dado de idade o app lê e para quê.** No Android, o app pede ao Google Play a faixa de idade da conta (Play Age
Signals); no iOS, pede à App Store (Declared Age Range). O Google Play ou a App Store perguntam antes se você aceita
compartilhar. O app recebe só a faixa, nunca a data de nascimento nem a identidade, e a usa só para decidir as
proteções abaixo. A faixa fica na memória enquanto o app está aberto: não é gravada, nem enviada à análise de uso, ao
backup ou à sincronização. Para saber se você está no Brasil, o app olha o país da rede ou do chip, o da conta da loja
e o do idioma do aparelho.

**O que muda para quem tem menos de 18 anos, ou sem a faixa de idade, no Brasil:**

- podcasts e episódios marcados como explícitos pelos próprios feeds não aparecem e não tocam, e a opção "Mostrar
  conteúdo explícito" fica travada. Sem a faixa de idade (no computador, no navegador ou se você não compartilhou),
  vale o mesmo, porque a lei não aceita uma simples declaração de idade para liberar;
- o que ninguém escolheu ainda começa desligado: análise de uso, notificações de episódio novo e a fila tocando em
  sequência.

**Só para menores, no Brasil:**

- análise de uso e relatórios de erro ficam sempre desligados;
- não há sugestões "Para você", que dependem do que a pessoa assina;
- a fila não passa sozinha para o próximo episódio, e não há notificações de episódio novo, a menos que os
  responsáveis permitam;
- depois de um tempo ouvindo (1 hora, ou o que os responsáveis escolherem), o app sugere uma pausa.

**Como os responsáveis agem.** Em Configurações › Supervisão dos responsáveis, os responsáveis criam um PIN e, com
ele, permitem a fila em sequência e as notificações, escolhem o intervalo do lembrete de pausa e veem o tempo de
escuta. O conteúdo explícito não pode ser liberado para menores. O Family Link (Android) e o Tempo de Uso (iOS)
também limitam o tempo e os apps de um menor em todo o aparelho. Esquecido o PIN, só é possível apagá-lo limpando os
dados do app, o que apaga também a biblioteca.

**Denúncias.** No Brasil, o botão "Denunciar" nas telas do podcast e do episódio envia por e-mail uma denúncia de
conteúdo que viole direitos de crianças e adolescentes e pode esconder o conteúdo no aparelho na hora. Em caso de
crime, denuncie também ao Disque 100, ao Comunica PF (gov.br/pf) ou à ANPD (gov.br/anpd).

## Termos de uso

O app é gratuito e é oferecido como está, sem garantia. Os podcasts são de quem os publica: o app só lê os feeds
públicos ou privados que você adiciona e não controla, não edita nem hospeda esse conteúdo. A marca "explícito" vem
dos próprios feeds, e um podcast pode estar marcado de forma errada.

## Contato

Pedidos sobre os seus dados e denúncias: **sj.leandro@gmail.com**, também em Configurações › Privacidade › "Pedir
algo sobre os seus dados". A resposta vem em até 15 dias.

Dúvidas gerais sobre o app, que não envolvam os seus dados, também podem ir à página do projeto:
https://github.com/rcleandro/Multiplatform-Podcast/issues
