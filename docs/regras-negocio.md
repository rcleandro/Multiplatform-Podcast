# Regras de Negócio — Podcast KMP

Este documento detalha a lógica de negócio, fluxos de trabalho e validações que regem o ecossistema do aplicativo de Podcast KMP.

## 1. Visão Geral
O projeto é uma plataforma de consumo de podcasts multiplataforma (Android, iOS, Desktop e Web). O objetivo principal é oferecer uma experiência unificada de descoberta, gerenciamento e reprodução de episódios, garantindo a integridade dos dados de progresso e a eficiência no consumo de feeds RSS.

## 2. Fluxo de Trabalho e Casos de Uso

### 2.1. Ingestão de Conteúdo (Assinatura)
1.  **Entrada:** O usuário fornece a URL de um feed RSS.
2.  **Validação:** O sistema verifica se o podcast já existe na biblioteca local.
3.  **Processamento:** O sistema realiza o fetch do XML, realiza o parsing dos metadados (incluindo extensões iTunes) e converte para entidades de domínio.
4.  **Persistência:** O podcast e seus episódios são salvos no banco de dados local.

### 2.2. Gerenciamento de Reprodução
1.  **Sincronização de Progresso:** Durante a reprodução, a posição atual (em milissegundos) é persistida periodicamente no banco de dados.
2.  **Marcação de Conclusão:** Ao atingir o final do áudio (ou uma porcentagem crítica), o episódio é marcado automaticamente como "Ouvido".
3.  **Fila de Reprodução (Queue):** O sistema gerencia uma lista ordenada de episódios a serem reproduzidos sequencialmente.

## 3. Tabela de Regras e Validações

| Componente / Comando | Regra / Restrição | Comportamento em Falha |
| :--- | :--- | :--- |
| `AddPodcastFromUrlUseCase` | **Unicidade de URL:** Não é permitido adicionar o mesmo feed mais de uma vez. | Retorna `PodcastError.AlreadyExists`. |
| `AddPodcastFromUrlUseCase` | **Formato RSS:** O XML deve conter tags obrigatórias (`<title>`, `<item>`, `<enclosure>`). | Retorna `PodcastError.ParseFailed`. |
| `AudioPlayer` | **Estado de Rede:** Reprodução de streaming requer conexão ativa. | Lança exceção de rede ou exibe alerta de conectividade. |
| `PlaybackStateDao` | **Persistência de Progresso:** O progresso deve ser salvo apenas se > 0 e < duração total. | Ignora a atualização ou zera o progresso se concluído. |
| `EpisodeDownloader` | **Integridade de Arquivo:** O download deve ser validado após a conclusão. | Exclui arquivo corrompido e permite nova tentativa. |

## 4. Tratamento de Exceções e Casos de Borda

> ⚠️ **Nota:** A aplicação utiliza o padrão `Result<T>` ou `sealed class PodcastError` para garantir que falhas de infraestrutura não causem crashes na camada de interface.

*   **Feed RSS Malformado:** Caso o parser encontre campos ausentes essenciais, o processo de importação é abortado para evitar dados inconsistentes na UI.
*   **Interrupção de Download:** O sistema suporta a retomada de downloads (via Ktor) ou descarta o fragmento parcial em caso de erro crítico de I/O.
*   **Conflito de Versão do Banco de Dados:** O Room 3 gerencia migrações automáticas; falhas graves de migração resultam na recriação do banco (destrutivo) em ambientes de desenvolvimento, mas são protegidas por testes de migração em produção.
