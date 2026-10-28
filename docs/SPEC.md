# Planejamento Mensal — Especificação de Requisitos

Oct 26, 2026 · @Anderson Gonçalves

## 1. Objetivo e escopo

O Planejamento Mensal controla as finanças mensais de uma casa: rendas, despesas fixas e variáveis e, principalmente, as faturas de cartão importadas do Itaú, com divisão de gastos por conta (pessoa ou centro de custo).

O mês de referência de tudo é a **competência**, definida pelo vencimento da fatura. Um seletor global de mês/ano no topo controla todas as abas.

| Aba | Finalidade |
| --- | --- |
| 1. Visão geral | Resumo do mês: % da renda gasta, gastos por categoria, saldo restante |
| 2. Rendas e contas | Cadastro de contas (pessoas) e rendas; despesa total por conta |
| 3. Despesas | Despesas fixas e dinâmicas; total por cartão |
| 4. Lançamentos | Lançamentos de cartão com alias de descrição, categoria e total rápido |
| 5. Importação | Importar faturas Itaú (PDF e XLSX) com validação de duplicidade e parcelas |

Fora do escopo da primeira versão: integração bancária automática (Open Finance), investimentos e múltiplos usuários com login separado.

## 2. Glossário e convenções

| Termo | Definição |
| --- | --- |
| Conta | Pessoa ou divisão de custo (ex.: Anderson, Cônjuge, Casa). Toda renda, despesa e lançamento pertence a uma conta |
| Cartão | Cartão de crédito com dia de fechamento e dia de vencimento |
| Fatura | Conjunto de lançamentos de um cartão com o mesmo vencimento |
| Competência | Mês/ano do vencimento da fatura. Ex.: vencimento em 10/11/2026 → competência 11/2026 |
| Data da compra | Data em que a compra foi feita no estabelecimento |
| Data do lançamento | Data em que o lançamento aparece na fatura (para parcelas, a data da parcela) |
| Parcela | Notação `n/N` (ex.: `03/10`) |
| Compra única | Notação `-` |
| Assinatura | Notação `*`; cobrança recorrente mensal |
| Descrição original | Texto exatamente como veio do cartão; nunca é alterado |
| Alias | Nome amigável dado pelo usuário a uma descrição original |
| Despesa fixa | Valor recorrente e previsível fora do cartão (aluguel, internet) |
| Despesa dinâmica | Valor variável, lançado manualmente no mês (feira, combustível em dinheiro/PIX) |

## 3. Cadastros base

Três cadastros sustentam todas as abas: contas, cartões e categorias.

**Contas (pessoas/divisões)**

- RF-01: Cadastrar, editar e inativar contas com nome, cor e indicador de ativa. Conta com lançamentos não pode ser excluída, apenas inativada.
- RF-02: Definir uma conta padrão por cartão, usada quando o lançamento não informa outra.

**Cartões**

- RF-03: Cadastrar cartões com apelido, banco (Itaú), finais do cartão (4 dígitos cada; um cartão pode ter final físico, virtual e de reposição na mesma fatura), titular/adicional (nome do portador como aparece na fatura), conta padrão, dia de fechamento e dia de vencimento.
- RF-04: Permitir alterar dia de fechamento/vencimento com vigência a partir de uma competência, sem recalcular faturas passadas.
- RF-05: Cartões adicionais (mesma fatura, final diferente) são agrupados sob o cartão titular para fins de fatura, mas identificáveis pelo final.

**Categorias**

- RF-06: Cadastrar categorias (ex.: Mercado, Transporte, Saúde, Lazer, Assinaturas) com cor e ícone; suportar subcategoria opcional.
- RF-07: Toda despesa e lançamento tem categoria. Sem categoria, recebe "Não classificado" e aparece destacado.
- RF-08: Regras de categorização automática: a categoria escolhida para uma descrição original é sugerida nos próximos lançamentos com a mesma descrição (ver alias, seção 7).

## 4. Aba 1 — Visão geral mensal

A visão geral responde, para a competência selecionada: quanto entrou, quanto saiu, quanto sobra e onde o dinheiro foi.

- RF-10: Indicadores no topo: renda total, despesa total, saldo restante (renda − despesa) e % da renda gasta (despesa ÷ renda × 100).
- RF-11: Despesa total = despesas fixas + despesas dinâmicas + total das faturas de cartão da competência.
- RF-12: Resumo por categoria: valor, % do total gasto e variação em relação ao mês anterior, em gráfico (rosca ou barras) e tabela.
- RF-13: Resumo por conta (pessoa): renda, despesa, saldo e % da renda gasta de cada conta.
- RF-14: Resumo por cartão: total da fatura da competência e quanto dele já está comprometido em parcelas futuras.
- RF-15: Filtro opcional por conta, para ver a visão geral de uma só pessoa.
- RF-16: Alerta visual quando % da renda gasta passar de um limite configurável (ex.: 80% amarelo, 100% vermelho).
- RF-17: Bloco "comprometido nos próximos meses": soma das parcelas já projetadas para as 3 competências seguintes.

## 5. Aba 2 — Rendas e contas

Rendas são parametrizadas: cadastra-se uma vez o modelo (fonte, valor, recorrência) e o sistema gera a renda em cada competência, permitindo ajustar o valor do mês.

- RF-20: Cadastro de contas acessível nesta aba (mesmo cadastro da seção 3).
- RF-21: Cadastro de renda parametrizada: descrição, conta, tipo (salário, freelance, bolsa, aluguel recebido, outro), valor padrão, recorrência (mensal, única, a cada N meses), dia previsto e vigência (início e fim opcional).
- RF-22: Para cada competência, gerar as rendas previstas a partir dos parâmetros; o usuário pode alterar o valor só daquele mês sem mudar o padrão.
- RF-23: Lançar renda avulsa (única) na competência.
- RF-24: Marcar renda como recebida/prevista.
- RF-25: Tabela de despesa total por conta: renda, despesas fixas, dinâmicas, cartão e saldo de cada conta.
- RF-26: Divisão de um lançamento entre contas: por padrão 100% para uma conta; opcionalmente dividir por percentual ou valor (ex.: compra da casa 50/50).
- RF-27: Mostrar "quem deve a quem" quando uma conta pagou despesas atribuídas a outra (ex.: cartão do Anderson com compras da conta Casa).

## 6. Aba 3 — Despesas mensais

A aba reúne tudo o que sai no mês fora do detalhe de cartão, mais o total consolidado de cada fatura.

**Despesas fixas**

- RF-30: Cadastro parametrizado igual às rendas: descrição, conta, categoria, valor padrão, recorrência, dia de vencimento, vigência e forma de pagamento (boleto, débito, PIX, cartão).
- RF-31: Geração automática por competência, com ajuste do valor só do mês.
- RF-32: Marcar como paga/pendente, com data de pagamento.
- RF-33: Despesa fixa paga no cartão não é somada duas vezes: ela é vinculada automaticamente, pela descrição (alias ou RN-16) e valor próximo, ao lançamento da fatura (ou a uma assinatura `*`).

**Despesas dinâmicas**

- RF-34: Lançamento manual com data, descrição, conta, categoria, valor e forma de pagamento.
- RF-35: Lista filtrável por conta e categoria, com total do filtro.

**Total por cartão**

- RF-36: Card por cartão com total da fatura da competência, data de fechamento, data de vencimento e status (aberta, fechada, paga).
- RF-37: Detalhar o total do cartão em compras à vista, parcelas e assinaturas.
- RF-38: Clicar no card leva à Aba 4 já filtrada por aquele cartão e competência.

## 7. Aba 4 — Lançamentos por cartão

Cada linha é uma parcela, compra única ou cobrança de assinatura numa fatura; a descrição original nunca muda e o usuário enxerga o alias.

**Campos do lançamento**

| Campo | Regra |
| --- | --- |
| Cartão | Obrigatório; inclui o final quando for adicional |
| Competência | Calculada pelo vencimento da fatura (seção 9) |
| Data do lançamento | Data na fatura |
| Data da compra | Data original da compra; igual em todas as parcelas |
| Conta | Conta padrão do cartão, editável; aceita divisão (RF-26) |
| Parcela | `n/N`, `-` (única) ou `*` (assinatura) |
| Descrição original | Somente leitura, vinda do cartão |
| Descrição exibida | Alias, se houver; senão a original |
| Valor | Valor da parcela em R$; negativo para estornos e créditos |
| Categoria | Obrigatória (padrão "Não classificado") |

**Requisitos**

- RF-40: Listar lançamentos da competência com filtros por cartão, conta, categoria, tipo (`-`, `n/N`, `*`) e texto.
- RF-41: Ordenar por qualquer coluna; padrão por data do lançamento.
- RF-42: Total rápido fixo na página: soma dos lançamentos filtrados e soma dos selecionados (checkbox), com contagem de itens.
- RF-43: Edição em linha de categoria, conta e alias.
- RF-44: Lançamento manual (compra não importada), com as mesmas regras de parcela.
- RF-45: Ao lançar compra parcelada manualmente, gerar as parcelas futuras nas competências seguintes (seção 9).

**Alias de descrição**

- RF-46: Renomear cria ou atualiza um alias ligado à **descrição original normalizada** (maiúsculas, sem espaços duplicados, sem o sufixo de parcela). A descrição original fica intacta.
- RF-47: Renomear de novo substitui o alias; o novo nome continua apontando para a descrição original, nunca para o alias anterior. Não existe alias de alias.
- RF-48: Ao renomear, o padrão é aplicar a todos os lançamentos com a mesma descrição original (passados e futuros importados); a opção "só este lançamento" cria um AliasOverride.
- RF-49: O alias pode carregar uma categoria padrão, aplicada automaticamente nas próximas importações.
- RF-50: Botão "restaurar original" remove o alias e volta a exibir a descrição do cartão.
- RF-51: Busca por texto encontra tanto pela descrição original quanto pelo alias.

## 8. Aba 5 — Importação Itaú

A importação lê a fatura, mostra uma prévia classificada linha a linha e só grava depois da confirmação do usuário.

**Entrada**

- RF-60: Seletor de cartão, mês e ano (competência de destino), preenchido com a competência atual.
- RF-61: Upload de arquivo nos formatos Itaú: PDF da fatura e XLSX exportado do internet banking. Detectar o formato pela extensão e validar a estrutura antes de processar.
- RF-62: Se o arquivo indicar vencimento ou cartão diferente do selecionado, avisar e pedir confirmação.

**Leitura dos formatos**

Os dois formatos foram mapeados com uma fatura fechada em PDF (vencimento em outubro de 2026) e uma fatura aberta em XLSX (vencimento em novembro de 2026) do mesmo cartão. Eles diferem em quase tudo, inclusive no texto da mesma compra.

| Aspecto | XLSX (fatura aberta, exportada do app) | PDF (fatura fechada) |
| --- | --- | --- |
| Situação | Aberta e parcial ("Fatura Aberta - \<mês>/\<ano>", "Valor (parcial)") | Fechada, com total final |
| Competência | Data na célula abaixo de "Vencimento" | "Vencimento: dd/mm/aaaa" na página 1 |
| Data da compra | Data do Excel, com ano; em parcelas, a data original da compra | Só dd/mm, sem ano; em parcelas, também a data original |
| Descrição | Loja + cidade + país colados, em caixa mista ("Posto Agronomiaporto Alegrebra") | Loja + cidade colados, em maiúsculas e truncados; a linha de baixo traz categoria do Itaú e cidade ("outros PORTO ALEGRE") |
| Parcela | Coluna "Parcelamento": "Parcela N de M" ou vazia | Sufixo `NN/NN` na descrição |
| Cartão | Colunas "Número do cartão" (`****4102`, `****4090`…) e "Tipo do cartão" (Físico, Virtual recorrente) | Não separa por final; agrupa por nome do portador |
| Assinatura | Não marcada ("Virtual recorrente" não significa assinatura) | Não marcada |
| Internacional | Valor já em reais; IOF numa linha própria "Iof Internacional - \<descrição>" | Seção própria com valor em dólar, valor em reais, cotação e "Repasse de IOF" como total |
| Tarifas e créditos | Linhas "Mensalidade - Plano Do Cartão" e "Redução Mensalidade" (negativa) | Seção "Lançamentos: produtos e serviços" |
| Pagamentos | Linha negativa "Pagamento Pix" | Seção "Pagamentos efetuados" |
| Totais para conferência | Subtotal com fórmula quebrada (`==SUBTOTAL`), não confiável | Total por seção e "Total dos lançamentos atuais" |
| Parcelas futuras | Não traz | Seção "Compras parceladas - próximas faturas" |
| Datas do ciclo | Só vencimento | Vencimento, emissão e "Previsão próx. Fechamento" |

**Parser XLSX (`ItauXlsxParser`)**

- RF-63: Ler a primeira planilha; achar a competência na célula "Fatura Aberta - \<mês>/\<ano>" e o vencimento abaixo de "Vencimento"; achar o cabeçalho pela célula "Data" seguida de "Lançamento", nunca por número de linha.
- RF-63a: Ler as colunas pelo nome do cabeçalho: Data, Lançamento, Parcelamento, Valor, Titularidade, Nome, Tipo do cartão, Número do cartão. Colunas sem cabeçalho são ignoradas.
- RF-63b: Ler até a primeira linha vazia; ignorar "Subtotal", "Importante saber" e o texto legal. O total é calculado pelo app, nunca lido da fórmula.
- RF-63c: "Parcela N de M" vira parcela `N/M`; vazio vira compra única.
- RF-63d: Linha negativa que começa com "Pagamento" é pagamento da fatura anterior e não vira lançamento; os demais negativos (como "Redução Mensalidade") são créditos.
- RF-63e: A linha "Iof Internacional - \<descrição>" vira um lançamento de IOF ligado à compra de mesma descrição e data.
- RF-63f: Fatura aberta gera um lote **parcial**; reimportar a mesma fatura aberta atualiza esse lote, e a fatura fechada depois concilia tudo (RN-21).

**Parser PDF (`ItauPdfParser`)**

- RF-64: Na página 1, ler o final do cartão (padrão `XXXX.XXXX.<4 dígitos>`), vencimento, emissão, previsão do próximo fechamento e "Total desta fatura".
- RF-64a: As páginas de lançamentos têm duas colunas. Cada coluna é extraída por área (PDFBox `PDFTextStripperByArea`); extrair a página inteira ordenada por posição mistura as colunas.
- RF-64b: Cada lançamento ocupa duas linhas: `dd/mm  descrição [NN/NN]  valor` e, abaixo, `<categoria do Itaú> <cidade>`. A categoria do Itaú é guardada como sugestão de categoria.
- RF-64c: Seções lidas: "Lançamentos: compras e saques" (por portador), "Lançamentos internacionais" e "Lançamentos: produtos e serviços". Ignoradas: "Pagamentos efetuados", encargos, limites, boleto, textos legais e "continua...".
- RF-64d: Internacional: usa o valor em reais da primeira linha; a linha de dólar e cotação vira observação; o "Repasse de IOF" vira um lançamento de IOF da fatura.
- RF-64e: PIX parcelado (`Principal (R$ x) + Juros (R$ y)`) guarda principal e juros na observação do lançamento.
- RF-64f: Ano da data: para parcela `n/N`, o ano que deixa a data mais perto de (fechamento − (n − 1) meses) sem passar do fechamento; para compra única, o ano que deixa a data até o fechamento. Ex.: `14/10 … 12/12` na fatura de outubro de 2026 é 14/10/2025.
- RF-64g: "Compras parceladas - próximas faturas" não cria lançamentos; serve para conferir as parcelas projetadas (RN-20).
- RF-64h: A soma das seções tem de bater com "Total dos lançamentos atuais"; diferença bloqueia a confirmação até o usuário revisar.

**Comum aos dois**

- RF-65: Extrair a parcela, removê-la da descrição e normalizar (RN-15).
- RF-66: Mapear o portador (PDF: título da seção; XLSX: coluna Nome) para a cota padrão, e cada final ao cartão cadastrado (RF-03); final desconhecido vai para a fila de validação.
- RF-67: Estornos e créditos entram com valor negativo. Dados pessoais do arquivo (CPF, endereço, agência, conta, código de barras) não são lidos nem guardados.

**Prévia e confirmação**

- RF-68: Mostrar cada linha com status: Novo, Duplicado (já existe), Parcela conciliada (bate com parcela projetada), Divergente (parece igual mas valor ou data diferem).
- RF-69: Por padrão: Novo e Conciliado são importados; Duplicado é ignorado; Divergente exige decisão do usuário.
- RF-70: Aplicar alias e categoria automáticos na prévia, permitindo editar antes de confirmar.
- RF-71: Conferir o total importado contra o total da fatura lido do arquivo e mostrar a diferença.
- RF-72: Registrar o lote de importação (arquivo, data, cartão, competência, quantidades por status) e permitir desfazer o lote inteiro.
- RF-73: Reimportar o mesmo arquivo não cria nada novo (idempotência pelo hash do arquivo e pela chave de cada linha).

> Amostras recebidas em 26/10/2026: uma fatura PDF e uma XLSX. Os fixtures de teste usam cópias com nome, CPF, endereço, agência e conta trocados por dados fictícios.

**Fila de validação**

Tudo o que entra como novo numa importação vai para uma fila de pendentes, onde o usuário confere e completa alias, cota, rateio e categoria antes de o lançamento ser dado como validado.

- RF-74: Lançamentos de compras novas (status NEW) entram com situação de revisão **pendente** (`reviewStatus = PENDING_REVIEW`). Parcelas conciliadas (MATCHED) de compras já validadas herdam alias, cota e categoria (RN-08) e não voltam para a fila.
- RF-75: Na fila, cada item já vem com sugestões preenchidas: alias e categoria do alias existente para a mesma descrição original, e cota padrão do cartão. Sugestão não valida o item.
- RF-76: Campos editáveis em cada item: alias (com o escopo do RF-48), cota ou rateio entre cotas, categoria e observação. Validar exige cota (ou rateio fechando 100%) e, quando a cota é a principal, categoria; lançamentos 100% de cotas de terceiros podem ficar sem categoria, como no modelo da planilha (seção 18).
- RF-77: Ações em lote: selecionar vários itens e aplicar a mesma cota, categoria ou alias; "validar selecionados".
- RF-78: Fluxo pelo teclado: Enter valida o item e vai para o próximo; Tab passa pelos campos; Esc pula sem validar.
- RF-79: Filtros por cartão, competência e lote de importação; contador de pendentes no menu lateral, visível em todas as abas.
- RF-79a: Pendentes entram nos totais (o dinheiro já foi gasto), mas a visão geral mostra quantos são e quanto somam, e as telas os destacam.
- RF-79b: Exportar o extrato de uma cota com pendentes na competência avisa e lista os itens; o usuário pode validar antes ou enviar assim mesmo.
- RF-79c: Lançamento manual já nasce validado; editar um item validado não o devolve para a fila; desfazer o lote (RF-72) remove também os pendentes dele.
- RF-79d: Opção em Ajustes, desligada por padrão: validar automaticamente itens cuja descrição já tem alias com categoria e cota definidas.

No código: enum `ReviewStatus` (PENDING\_REVIEW, REVIEWED), caso de uso `ValidateEntriesUseCase` (um item ou lote) e testes `ValidateEntriesUseCaseTest` e `review-queue.spec.ts`.

## 9. Regras de negócio

Uma compra parcelada é um único registro de compra com N parcelas, cada parcela presa a uma competência calculada pelo fechamento e vencimento do cartão.

**Competência**

- RN-01: Se a data da compra for até o dia de fechamento, a 1ª parcela cai na fatura que fecha naquele mês; se for depois, cai na fatura seguinte.
- RN-02: A competência da fatura é o mês/ano do vencimento. Ex.: fechamento dia 3, vencimento dia 10; compra em 05/10/2026 → fatura que fecha em 03/11 e vence em 10/11 → competência 11/2026.
- RN-03: Na importação, a competência selecionada pelo usuário prevalece para os lançamentos do arquivo; o cálculo do RN-01 é usado para compras manuais e para projetar parcelas.

**Rotação de parcelas**

- RN-04: Ao registrar a parcela `n/N`, o sistema cria (se não existirem) as parcelas `n+1 … N` como **projetadas** nas competências seguintes, uma por mês, com o mesmo valor.
- RN-05: Se a primeira parcela importada for `03/10`, as parcelas 01 e 02 não são criadas retroativamente (apenas registradas como "anteriores ao sistema").
- RN-06: Quando a fatura do mês seguinte é importada, a linha `n+1/N` concilia com a parcela projetada: muda o status para **confirmada** e ajusta valor/data se diferirem (diferença de centavos em arredondamento é aceita).
- RN-07: Parcela projetada que não aparece na fatura importada fica marcada como **não confirmada** para revisão (compra cancelada, antecipação de parcelas).
- RN-08: Alias, categoria e conta definidos em uma parcela valem para todas as parcelas da mesma compra.
- RN-09: Assinaturas `*` são projetadas em todas as competências seguintes, até deixarem de aparecer numa fatura fechada importada ou serem removidas pelo usuário; a projeção cobre o horizonte exibido (ex.: 12 meses) e é renovada a cada mês.

**Deduplicação**

- RN-10: Chave de uma parcela = cartão + data da compra + descrição original normalizada + valor total da compra (ou valor da parcela × N) + número da parcela.
- RN-11: Chave de compra (agrupa parcelas) = mesma chave sem o número da parcela.
- RN-12: Duas compras idênticas legítimas no mesmo dia (ex.: dois cafés iguais) se distinguem por um sequencial de ocorrência dentro do arquivo; a prévia sinaliza o caso.
- RN-13: Ordem de classificação de cada linha importada:
  1. Chave de parcela já existe como confirmada → Duplicado.
  2. Chave de parcela existe como projetada → Parcela conciliada.
  3. Mesmo cartão, descrição e parcela, mas valor ou data com diferença → Divergente.
  4. Nenhum dos casos → Novo (e, se parcelado, projeta as próximas).
- RN-14: Lançamento editado manualmente pelo usuário (alias, categoria, conta) nunca é sobrescrito por reimportação; só valor e data podem ser atualizados, com aviso.

**Correspondência entre PDF e XLSX**

A mesma compra aparece com texto, data e centavos diferentes nos dois formatos, então a comparação não pode ser exata.

| Mesma parcela no PDF | No XLSX |
| --- | --- |
| `20/09 BMB *Equato 02/02 276,77` | `20/09/2026 Bmb *Equatorialporto Alegrebra · Parcela 2 de 2 · 276,77` |
| `05/09 nuuvem *N 02/02 135,35` | `04/09/2026 Nuuvem *Nuuvemrio De Janeirbra · Parcela 2 de 2 · 135,35` |
| `31/08 MERCADOLIVRE*M 02/02 48,01` | `30/08/2026 Mercadolivre*mercadoljaragua Do Sbra · Parcela 2 de 2 · 48,01` |

- RN-15: Normalização da descrição: sem acentos, maiúsculas, só letras e números ("Posto Agronomiaporto Alegrebra" → `POSTOAGRONOMIAPORTOALEGREBRA`).
- RN-16: Duas descrições são da mesma loja quando uma normalizada é prefixo da outra, com pelo menos 8 caracteres (`BMBEQUATO` é prefixo de `BMBEQUATORIALPORTOALEGREBRA`).
- RN-17: A data da compra pode diferir em 1 dia entre os formatos (3 das 16 parcelas da amostra); a tolerância é de ±1 dia.
- RN-18: Parcelas da mesma compra podem diferir em até R$ 0,02 (48,02 e 48,01); a última parcela absorve o arredondamento.
- RN-19: A chave de compra (RN-11) passa a ser: cartão + data da compra (±1 dia) + quantidade de parcelas + valor da parcela (±0,02) + descrição por prefixo (RN-16). Isso substitui a comparação exata de descrição do RN-10.
- RN-20: As parcelas projetadas são conferidas com "Compras parceladas - próximas faturas" do PDF; se diferirem, vale o valor do banco.
- RN-21: Fatura aberta (XLSX) e depois fechada (PDF): lançamentos do lote parcial que batem pelo RN-19 viram CONFIRMED; os que não aparecem na fechada vão para a fatura seguinte se a data passar do fechamento, ou ficam UNCONFIRMED para revisão.
- RN-22: O fechamento varia de mês a mês. O app usa a data de fechamento lida dos arquivos (`CardBill.closingDate`, da "Previsão próx. Fechamento") e só recorre ao dia típico do cartão (RN-01) quando não tiver essa data.
- RN-23: Assinaturas não vêm marcadas. A fila de validação sugere `*` quando a mesma loja (RN-16), sem parcela, aparece em 2 competências seguidas com valor parecido; o usuário confirma.
- RN-24: Um alias agrupa várias descrições normalizadas: a do PDF e a do XLSX da mesma loja, ou variações como `ECPARAMOUNTPLUS…` e `MPPARAMOUNTPLUS…`. A fila de validação sugere juntar quando o RN-16 indicar a mesma loja.
- RN-25: Duas linhas iguais no mesmo dia são compras distintas (a amostra tem duas de R$ 12,00 na mesma loja em 03/09) e seguem o RN-12.

**Estornos**

- RN-26: Linha negativa que corresponde a uma compra anterior (mesma loja pelo RN-16 e valor igual ou menor) vira um lançamento de **estorno** ligado a ela, e a compra original é marcada como **estornada**. O estorno herda cota e categoria da original, então os totais se anulam.
- RN-27: Estorno parcial (valor menor que a compra) marca a original como parcialmente estornada e mostra o saldo restante.
- RN-28: Negativo sem compra correspondente (ex.: "Redução Mensalidade") é crédito avulso; se o app não decidir sozinho, o item vai para a fila de validação para o usuário ligar a uma compra ou manter como crédito.
- RN-29: Compra parcelada estornada cancela as parcelas projetadas restantes.
- RN-30: Se uma despesa fixa vinculada automaticamente (RF-33) tiver mais de um lançamento candidato, o vínculo vai para a fila de validação.

## 10. Modelo de dados sugerido

O ponto central é separar **Compra** (o fato) de **Lançamento** (cada parcela numa fatura) e **Alias** (o nome dado a uma descrição original).

| Entidade | Campos principais |
| --- | --- |
| `Member` | id, code (sigla, ex.: AB), name, kind (MAIN, THIRD\_PARTY), color, active, email, whatsappPhone, pixKey, preferredLanguage |
| `Card` | id, nickname, bank, lastDigits (lista), holderName, primaryCardId, defaultMemberId, active |
| `CardCycle` | id, cardId, closingDay, dueDay, validFrom |
| `Category` | id, name, parentId, color, icon |
| `CardBill` | id, cardId, billingPeriod, closingDate, dueDate, fileTotal, status (OPEN, CLOSED, PAID) |
| `Purchase` | id, cardId, purchaseDate, originalDescription, normalizedDescription, totalAmount, installmentCount, type (SINGLE, INSTALLMENT, SUBSCRIPTION), bankCategory, cardLastDigits, purchaseKey |
| `BillEntry` | id, purchaseId, cardBillId, installmentNumber, entryDate, amount, status (PROJECTED, CONFIRMED, UNCONFIRMED), installmentKey, refundOfEntryId, refundStatus (NONE, PARTIAL, FULL), reviewStatus (PENDING\_REVIEW, REVIEWED), manuallyEdited, version |
| `EntrySplit` | billEntryId, memberId, percentage ou amount |
| `Alias` | id, displayName, defaultCategoryId, patterns (descrições normalizadas, cada uma única) |
| `AliasOverride` | billEntryId, displayName (renomear só um lançamento) |
| `RecurrenceRule` | id, kind (INCOME, FIXED\_EXPENSE), description, memberId, categoryId, defaultAmount, recurrence, day, validFrom, validUntil |
| `Occurrence` | id, recurrenceRuleId, billingPeriod, amount, status, effectiveDate |
| `VariableExpense` | id, date, description, memberId, categoryId, amount, paymentMethod |
| `ImportBatch` | id, cardId, billingPeriod, format (PDF, XLSX), fileHash, importedAt, newCount, duplicateCount, matchedCount, divergentCount, undone |
| `Template` / `TemplateVersion` | id, name, isDefault / id, templateId, version, settings, texts por idioma, createdAt |
| `StatementDelivery` | id, memberId, billingPeriod, channel, language, templateVersionId, sentAt, status (SENT, AWAITING\_USER, FAILED) |

Renda e despesa fixa compartilham `RecurrenceRule` e `Occurrence`, diferenciadas pelo campo `kind` (DRY).

Exibição da descrição: AliasOverride do lançamento → Alias da descrição normalizada → descrição original.

## 11. Requisitos não funcionais

- RNF-01: Valores monetários em centavos (inteiro) ou decimal exato; nunca ponto flutuante.
- RNF-02: Números e datas formatados conforme o idioma escolhido (seção 17); valores sempre em reais (BRL).
- RNF-03: Importação de uma fatura com até 300 linhas em menos de 5 segundos.
- RNF-04: Arquivos importados não são guardados após o processamento, apenas o hash e o resumo do lote; números completos de cartão nunca são armazenados (só o final).
- RNF-05: Toda alteração em lançamento confirmado gera registro de auditoria (antes, depois, data).
- RNF-06: Interface que se adapta ao tamanho da janela, de 800 px de largura a telas largas (seção 14); funciona 100% offline, exceto o envio de e-mail.
- RNF-07: Backup/exportação dos dados em CSV ou JSON.
- RNF-08: Parsers isolados por formato (interface comum), para adicionar outros bancos depois sem mexer nas regras de negócio.
- RNF-09: Documentação Javadoc e testes automatizados por funcionalidade, conforme a seção 15.
- RNF-10: Sem senha e sem login: o app abre direto na visão geral e um único usuário vê todas as contas. O arquivo de dados não é cifrado; quem tiver o SSD tem acesso aos dados.

## 12. Arquitetura e stack

Um app desktop nativo, portátil e multiplataforma (Windows e Linux): uma janela Tauri 2 com a interface em React inicia um backend Spring Boot local com JRE embutido e grava tudo num arquivo SQLite na própria pasta, que pode ficar num SSD externo.

&#91;embedded content: arquitetura · app, API em camadas e banco\]

As dependências apontam para dentro: `api` chama `application`, que usa `domain` e acessa banco e arquivos só por interfaces implementadas em `infra`.

| Camada | Tecnologia | Responsabilidade |
| --- | --- | --- |
| Shell desktop | Tauri 2 | Janela nativa (WebView2 no Windows, WebKitGTK no Linux), um executável portátil por sistema; inicia e encerra o backend |
| Interface | React 18 + Vite, TypeScript | Telas dentro da janela |
| Navegação | React Router | Menu lateral com as abas |
| Estilo | Tailwind CSS | Layout que se adapta ao tamanho da janela |
| Estado do servidor | TanStack Query | Cache, recarga e invalidação por competência |
| Estado de UI | Zustand | Competência selecionada, filtros, seleção do total rápido |
| Formulários | React Hook Form + Zod | Validação no cliente com esquemas gerados da API |
| Cliente da API | Orval (hooks, tipos, Zod e mocks MSW a partir do OpenAPI) | Contrato único entre interface e backend |
| Gráficos | Recharts | Gráficos da visão geral |
| Backend | Spring Boot 3, Java 21, Spring HATEOAS (HAL), executado como sidecar do Tauri | API local só em 127.0.0.1, porta livre escolhida ao abrir |
| Runtime Java | JRE mínimo gerado com `jlink`, um por sistema operacional, dentro da pasta do app | Não exige Java instalado no computador |
| Persistência | Spring Data JPA, SQLite (dialeto do `hibernate-community-dialects`), Flyway | Um único arquivo `data/planejamento-mensal.db` |
| Documentação | Javadoc + springdoc-openapi | Javadoc por funcionalidade (seção 15); OpenAPI gera o contrato |
| Importação | Apache PDFBox (PDF), Apache POI (XLSX) | Só leitura de arquivo; sem regra de negócio |
| Exportação | Thymeleaf, OpenHTMLtoPDF, Spring Mail | Extrato por cota (seção 16) |
| Segurança | Token aleatório por sessão entre janela e backend | Nenhum outro processo ou máquina acessa a API |

**Por que Tauri e não Electron ou Docker**

| Opção | Tamanho aproximado | A favor | Contra |
| --- | --- | --- | --- |
| Tauri 2 + backend Java | \~120 MB | Usa a WebView do sistema, abre rápido, pasta portátil | Build exige Rust (só na máquina de quem compila) |
| Electron + backend Java | \~280 MB | Tudo em JavaScript no shell, Chromium igual em todo PC | Carrega um Chromium inteiro, mais memória e disco |
| Docker Compose | Depende do Docker instalado | Ambiente idêntico | Exige Docker e permissão de admin; não roda de um SSD em qualquer PC |

Tamanhos por sistema; a pasta com Windows e Linux juntos fica em torno de 250 MB. Estimativas a confirmar no primeiro build. O Electron renderiza igual em todo sistema, enquanto o Tauri usa motores diferentes (WebView2 e WebKitGTK); por isso a interface é testada nos dois motores (seção 15).

**Organização do backend (pacote por módulo)**

Cada módulo tem as mesmas quatro camadas; um módulo só acessa outro pela camada `application`.

| Camada | Contém | Não pode |
| --- | --- | --- |
| `api` | Controller, DTO de entrada/saída, mapper | Ter regra de negócio ou acessar repositório |
| `application` | Um caso de uso por classe (`PreviewImportUseCase`) | Conhecer HTTP ou formato de arquivo |
| `domain` | Entidades, value objects, políticas (regras RN) | Depender de Spring; anotações JPA são a única exceção (SOL-05) |
| `infra` | Repositórios JPA, parsers Itaú | Decidir regra de negócio |

Módulos (pacotes sob `com.planejamentomensal`): `member`, `card`, `category`, `recurrence` (rendas e despesas fixas), `expense`, `entry`, `alias`, `imports` (`import` é palavra reservada do Java), `summary`, `statement` (exportação por cota), `i18n` e `shared` (`Money`, `BillingPeriod`, auditoria).

### API RESTful com HATEOAS

A API segue REST nível 3: cada resposta em HAL (`application/hal+json`) traz links para os próximos passos possíveis, e a interface mostra uma ação só quando o link dela vem na resposta.

**Regras REST**

- API-01: Recursos são substantivos em inglês, no plural e em kebab-case (`/api/bill-entries`), sem verbos na URL; ações de negócio que não são CRUD viram sub-recursos (`POST /api/imports/{id}/confirmation`).
- API-02: Verbos com a semântica HTTP: `GET` lê, `POST` cria, `PUT` substitui (idempotente), `PATCH` altera parte, `DELETE` remove.
- API-03: Status: `200` leitura, `201` criação com cabeçalho `Location`, `204` sem corpo, `400` validação, `404` inexistente, `409` conflito de regra (ex.: lote já desfeito), `412` versão desatualizada; erros sempre em `ProblemDetail` (RFC 9457), no idioma do `Accept-Language`.
- API-04: Controle de concorrência com `ETag` e `If-Match` (versão JPA `@Version`), evitando que duas telas sobrescrevam a mesma edição.
- API-05: Listas paginadas como `PagedModel`, com `page`, `size`, `sort` e links `first`, `prev`, `next`, `last`.
- API-06: Filtros por query string no recurso da lista (`/api/bill-entries?billingPeriod=2026-11&card=3&category=5`).

**Regras HATEOAS**

- API-07: `GET /api` é o ponto de entrada e devolve links para todos os recursos de primeiro nível; a interface só conhece essa URL e navega pelos links.
- API-08: Todo recurso traz `self` e links para os recursos relacionados (`card`, `category`, `purchase`, `bill`).
- API-09: Links de ação aparecem conforme o estado, decidido no backend. A interface não repete essas regras (DRY).
- API-10: Relações próprias em inglês e kebab-case (`rename`, `restore-original`, `confirm`, `undo`, `export`, `send`), descritas num perfil HAL em `/api/profile`.
- API-11: Uma classe `RepresentationModelAssembler` por recurso monta links e representação (SRP); controllers só chamam o caso de uso e o assembler.
- API-12: O OpenAPI (springdoc com suporte a HATEOAS) descreve os modelos HAL, e o Orval gera os tipos com `_links` e `_embedded`.
- API-13: Testes de contrato com `WebTestClient`/MockMvc e Spring HATEOAS conferem, para cada estado, quais links estão presentes ou ausentes.

**Recursos e links de ação**

| Recurso | URL | Links de ação condicionais |
| --- | --- | --- |
| Entrada | `/api` | `members`, `cards`, `categories`, `bill-entries`, `imports`, `summaries`, `statements`, `templates` |
| Lançamento | `/api/bill-entries/{id}` | `rename` sempre; `restore-original` só com alias; `confirm` só se PROJECTED |
| Compra | `/api/purchases/{id}` | `installments` (lançamentos da compra) |
| Fatura | `/api/card-bills/{id}` | `import` se aberta; `mark-paid` se fechada e não paga |
| Importação | `/api/imports/{id}` | `confirm` e `cancel` se em prévia; `undo` se confirmada e não desfeita |
| Resumo mensal | `/api/summaries/{billingPeriod}` | `previous`, `next`, `by-member`, `by-category` |
| Extrato da cota | `/api/statements/{billingPeriod}/{memberId}` | `pdf`, `png`, `xlsx`, `send-email`, `send-whatsapp` (este só se a cota tiver telefone) |
| Template | `/api/templates/{id}` | `preview`, `versions`, `make-default` se não for o padrão |
| Fila de validação | `/api/review-queue` | `validate-selected` quando há pendentes; cada item traz `validate` só se PENDING\_REVIEW |

Exemplo de um lançamento com alias, na competência atual:

```json
{
  "id": 482,
  "displayDescription": "Mercado do bairro",
  "originalDescription": "SUPERMERC JOAO LTDA",
  "installment": "03/10",
  "amount": 89.90,
  "status": "CONFIRMED",
  "_links": {
    "self": { "href": "/api/bill-entries/482" },
    "purchase": { "href": "/api/purchases/97" },
    "bill": { "href": "/api/card-bills/31" },
    "category": { "href": "/api/categories/4" },
    "rename": { "href": "/api/bill-entries/482/alias" },
    "restore-original": { "href": "/api/bill-entries/482/alias" }
  }
}
```

No front, um hook único `useAction(resource, 'rename')` lê o link e faz a chamada; o botão some quando o link não vem. Os valores de dados do usuário (como `displayDescription`) não são traduzidos.

### Execução portátil (SSD, Windows e Linux)

O app é uma pasta copiada para o SSD que abre com duplo clique no Windows ou no Linux, sem instalar nada e sem administrador; os dois sistemas usam os mesmos dados.

```text
PlanejamentoMensal/
├── PlanejamentoMensal.exe              ← Windows
├── PlanejamentoMensal.AppImage         ← Linux
├── start-linux.sh                  ← alternativa sem FUSE ou sem permissão de execução
├── backend/
│   ├── app.jar                     ← o mesmo para os dois sistemas
│   ├── runtime-windows/            ← JRE mínimo (jlink)
│   └── runtime-linux/
└── data/                           ← compartilhado entre os sistemas
    ├── planejamento-mensal.db                  ← SQLite
    ├── templates/                  ← templates, logos e fontes
    ├── exports/
    ├── backups/
    └── logs/
```

| Sistema | Versões suportadas | Executável | Depende de |
| --- | --- | --- | --- |
| Windows | 10 e 11, x64 | `PlanejamentoMensal.exe` | WebView2 Runtime (já presente no Windows atualizado) |
| Linux | x86\_64 com glibc 2.35 ou mais nova (Ubuntu 22.04+, Debian 12+, Fedora 38+, Mint 21+) | `PlanejamentoMensal.AppImage` | Nada além do sistema: o AppImage leva WebKitGTK e bibliotecas |

**Portabilidade**

- POR-01: Uma pasta serve aos dois sistemas: `app.jar` único, um JRE por sistema e `data/` compartilhado. O mesmo SSD abre no Windows hoje e no Linux amanhã, com os mesmos dados.
- POR-02: O SSD é formatado em exFAT, lido e gravado nativamente pelos dois sistemas.
- POR-03: Todo caminho é relativo à pasta do executável e montado com `java.nio.file.Path`; nada é gravado em AppData, no registro, em `~/.config` ou fora da pasta.
- POR-04: Nomes de arquivo gerados pelo app usam só letras, números, `-` e `_`, evitando caracteres proibidos no Windows e nomes que só diferem em maiúsculas.
- POR-05: Fontes do template ficam embutidas em `data/templates/`, para o PDF sair idêntico nos dois sistemas.
- POR-06: Integrações com o sistema só pelos plugins do Tauri (abrir arquivo e pasta, área de transferência, diálogo de arquivo, notificações), sem código específico de sistema na interface.
- POR-07: WhatsApp: no Windows abre o WhatsApp Desktop se instalado, senão o navegador; no Linux abre o WhatsApp Web no navegador padrão.

**Execução**

- POR-08: Ao abrir, o Tauri sobe o backend com o JRE do sistema atual, numa porta livre em 127.0.0.1 e com token de sessão; mostra tela de carregamento até `/actuator/health` responder. Ao fechar a janela, encerra o backend.
- POR-09: No Linux, se faltar FUSE ou o SSD estiver montado sem permissão de execução, `bash start-linux.sh` roda o AppImage extraído e copia o JRE para uma pasta temporária, removida ao fechar (única exceção ao POR-03).
- POR-10: Instância única: um arquivo de trava em `data/` com máquina, sistema e PID impede abrir o mesmo banco duas vezes; trava órfã (PID inexistente) é liberada com confirmação.
- POR-11: SQLite em modo WAL com `synchronous=FULL`; ao fechar, faz checkpoint para deixar um único arquivo consistente antes de o SSD ir para o outro computador.
- POR-12: Backup automático ao abrir (cópia em `data/backups/`, mantendo as 10 mais recentes) e botão "Fazer backup agora" com destino escolhido.
- POR-13: Falha de escrita (SSD removido com o app aberto) gera aviso claro; a interface orienta fechar o app antes de ejetar.

**Atualização e build**

- POR-14: Atualizar = substituir executáveis e `backend/`; `data/` fica intacto e o Flyway migra o banco ao abrir, com backup antes.
- POR-15: O CI compila o `app.jar` uma vez e roda em matriz `windows-latest` e `ubuntu-22.04` (a distro mais antiga suportada, para o AppImage funcionar nas mais novas): `jlink` e `tauri build` em cada sistema, juntando tudo num único `PlanejamentoMensal-<version>.zip`.
- POR-16: O app procura uma versão nova nos releases do GitHub ao abrir (em silêncio se estiver sem internet) e mostra um aviso com as novidades, sem nunca obrigar. Ao aceitar, baixa o zip, confere o SHA-256 publicado com ele e prepara os arquivos em `update/`, dentro da pasta do app; ao reiniciar, troca executáveis, scripts e `backend/` e volta para a versão anterior se a nova não abrir. `data/` não é alterado (o backup do POR-12 e a migração do POR-14 seguem iguais), e o `start-linux.sh` reinicia o app depois da troca.

## 13. DRY e SRP na prática

Cada regra de negócio tem exatamente um dono no código; todos os fluxos (lançamento manual, importação, projeção) chamam esse dono em vez de reimplementar a regra.

**SRP — uma responsabilidade por classe (backend)**

| Classe | Única responsabilidade | Regras |
| --- | --- | --- |
| `BillingPeriodPolicy` | Dada data da compra e ciclo do cartão, devolve a competência | RN-01 a RN-03 |
| `InstallmentProjector` | Gerar ou atualizar parcelas projetadas e assinaturas | RN-04 a RN-09 |
| `DescriptionNormalizer` | Normalizar texto e separar o sufixo de parcela | RF-46, RF-65 |
| `EntryKeyFactory` | Montar chave de compra e de parcela | RN-10 a RN-12 |
| `ImportClassifier` | Dar o status de cada linha (NEW, DUPLICATE, MATCHED, DIVERGENT) | RN-13 |
| `DisplayDescriptionResolver` | Resolver override → alias → original | RF-46 a RF-50 |
| `BillParser` (interface) | Ler arquivo e devolver `List<RawBillLine>` | RF-63, RF-64 |
| `ItauPdfParser`, `ItauXlsxParser` | Uma implementação por formato, só leitura | RF-63, RF-64 |
| `PreviewImportUseCase` | Orquestrar parse → normalizar → classificar → prévia, sem gravar | RF-68 a RF-71 |
| `ConfirmImportUseCase` | Gravar o lote confirmado e projetar parcelas | RF-72 |
| `UndoImportBatchUseCase` | Reverter um lote inteiro | RF-72 |
| `MonthlySummaryService` | Calcular indicadores da visão geral | RF-10 a RF-17 |
| `SplitPolicy` | Dividir um valor entre cotas sem perder centavos | RF-26 |

**DRY — o que é escrito uma vez só**

- `Money` e `BillingPeriod` (value objects em `shared`): arredondamento, soma, formatação e cálculo de mês seguinte em um lugar só.
- Rendas e despesas fixas usam o mesmo `RecurrenceRule` e o mesmo `GenerateOccurrencesUseCase`; só muda o `kind` (RF-21, RF-30).
- Contrato da API: o OpenAPI do springdoc gera tipos, hooks e esquemas Zod no front via Orval; nenhum DTO é escrito à mão no TypeScript.
- Validação: Bean Validation no backend é a fonte; o Zod gerado espelha no front.
- Totais oficiais (fatura, categoria, cota, saldo) só são calculados no backend. O front soma apenas a seleção do total rápido, sobre valores já recebidos.
- Erros: um `@RestControllerAdvice` único devolve `ProblemDetail` (RFC 9457); um interceptor único no front mostra a mensagem.
- Auditoria: um listener JPA (`@EntityListeners`) registra alterações, em vez de código em cada serviço.
- Textos: um `MessageSource` no backend e um arquivo de tradução por idioma no front; nenhum texto de interface repetido em código (seção 17).

**DRY no front — componentes e hooks compartilhados**

| Peça | Usada em |
| --- | --- |
| `useBillingPeriod()` + `BillingPeriodSelector` | Cabeçalho global de todas as abas |
| `MoneyText`, `MoneyInput` | Todo valor em R$, formatado pelo idioma |
| `FilterableList` (lista + filtros + ordenação) | Lançamentos, despesas dinâmicas, rendas |
| `QuickTotalBar` | Lançamentos e despesas dinâmicas |
| `RecurrenceRuleForm` | Cadastro de renda e de despesa fixa |
| `CategoryPicker`, `MemberPicker` | Todo formulário com categoria ou cota |
| `SummaryCard` | Visão geral e totais por cartão |
| `useAction()` | Todo botão de ação vindo de link HATEOAS |

**SOLID**

- SOL-01 (S): Além das classes acima, `MonthlySummaryService` só orquestra calculadoras menores: `CategoryTotalsCalculator`, `MemberTotalsCalculator` e `CommittedInstallmentsCalculator`.
- SOL-02 (O): Formatos de fatura e canais de envio entram por registro de estratégias: o Spring injeta `Map<BillFormat, BillParser>` e `Map<Channel, DeliveryChannel>`. Um formato ou canal novo é uma classe nova com `@Component`, sem editar as existentes.
- SOL-03 (L): `DeliveryChannel.deliver()` devolve `DeliveryResult` com `SENT`, `AWAITING_USER` ou `FAILED`. O e-mail devolve `SENT`; WhatsApp, área de transferência e pasta devolvem `AWAITING_USER`, porque o usuário termina o envio; o histórico guarda esse estado e o usuário pode marcar como enviado.
- SOL-04 (I): Portas pequenas por uso, definidas em `application` (`BillEntryReader`, `BillEntryWriter`, `AliasRepository`, `ImportBatchRepository`…); cada caso de uso depende só das portas que usa.
- SOL-05 (D): As portas ficam em `application` e são implementadas em `infra`; `application` e `domain` não importam nada de `infra`. Exceção consciente: as entidades do `domain` usam anotações JPA (Jakarta Persistence), para não manter um segundo modelo só de persistência; nenhuma outra dependência de framework entra no `domain`.
- SOL-06: Um teste com ArchUnit (`LayerDependencyTest`) quebra o build se alguma regra de dependência entre camadas for violada.

## 14. Diretrizes de interface (desktop)

O uso principal é com teclado e mouse; a janela se adapta de 800 px de largura até telas largas.

| Largura da janela | Layout |
| --- | --- |
| 800–1199 px | Menu lateral recolhido (só ícones), tabelas com as colunas essenciais |
| ≥ 1200 px | Menu lateral expandido, todas as colunas, painel de detalhes à direita |

- UI-01: Atalhos: Ctrl+N novo lançamento, Ctrl+I importar, Ctrl+E exportar, Ctrl+F buscar, Alt+← / Alt+→ trocar competência.
- UI-02: Seletor de competência fixo no topo da janela.
- UI-03: Total rápido (RF-42) como barra fixa no rodapé.
- UI-04: Lançamentos em tabela com edição em linha; Ctrl+clique e Shift+clique selecionam para o total rápido.
- UI-05: Renomear (alias) e trocar categoria direto na linha ou no painel de detalhes.
- UI-06: Importação arrastando o PDF/XLSX para a janela ou pelo diálogo nativo de arquivo; a prévia expande só as linhas Divergentes.
- UI-07: Gráficos com Recharts.
- UI-08: Tema claro e escuro seguindo o sistema.
- UI-09: Listas longas com virtualização (TanStack Virtual).
- UI-10: O app lembra tamanho e posição da janela (configuração salva em `data/`).

## 15. Documentação e testes por funcionalidade

Uma funcionalidade só está pronta quando tem Javadoc, testes automatizados e sua linha na matriz de rastreabilidade; o CI barra o que faltar.

**Javadoc**

- DOC-01: Cada módulo tem um `package-info.java` que descreve a funcionalidade, lista as RF/RN que implementa e aponta as classes principais com `{@link}`.
- DOC-02: Toda classe e método público em `domain`, `application` e `api` tem Javadoc com descrição, `@param`, `@return`, `@throws` e `@see` para as classes relacionadas.
- DOC-03: O Javadoc cita a regra pelo id ("Implements RN-04") em vez de copiar seu texto; o texto completo vive só neste documento (DRY).
- DOC-04: `maven-javadoc-plugin` com doclint ativo; o build falha com Javadoc ausente ou inválido nessas camadas. O HTML gerado é publicado como artefato do CI.
- DOC-05: Os endpoints são descritos uma vez no Javadoc dos controllers; o springdoc lê esse Javadoc (therapi-runtime-javadoc) para montar o OpenAPI.
- DOC-06: No front, TSDoc no mesmo padrão (`@param`, `@returns`, `@see`) em hooks e componentes compartilhados.

Exemplo do padrão esperado:

```java
/**
 * Calculates the billing period (due month/year) of a purchase's first installment.
 *
 * <p>Implements RN-01 and RN-02: purchases up to the closing day go to the bill
 * that closes in the same month; purchases after the closing day go to the next bill.</p>
 *
 * @param purchaseDate date of the purchase at the merchant; must not be {@code null}
 * @param cycle        card cycle valid on the purchase date
 * @return the billing period of the first installment
 * @throws CycleNotValidException if {@code cycle} is not valid on {@code purchaseDate}
 * @see InstallmentProjector
 * @since 1.0
 */
public BillingPeriod calculate(LocalDate purchaseDate, CardCycle cycle) { ... }
```

**Testes**

| Nível | Ferramentas | O que cobre |
| --- | --- | --- |
| Domínio | JUnit 5, AssertJ, `@ParameterizedTest` | Cada RN com casos de borda: compra no dia do fechamento, virada de ano, fevereiro, parcela 1/1, estorno |
| Caso de uso | JUnit 5 + Mockito | Orquestração da `application`, sem banco |
| Parser | JUnit 5 + faturas mascaradas em `src/test/resources/fixtures/itau` | Cada formato (PDF, XLSX) e cada variação de layout |
| Integração | `@SpringBootTest`, MockMvc, SQLite em arquivo temporário | Endpoints, transações, migrações Flyway |
| Interface | Vitest, React Testing Library, MSW (mocks gerados pelo Orval) | Componentes compartilhados e telas |
| Ponta a ponta | Playwright com Chromium (equivale ao WebView2) e WebKit (equivale ao WebKitGTK), backend real, janelas de 800 × 600 e 1280 × 800 px | Importar fatura, renomear, exportar extrato, conferir visão geral |
| Pacote portátil | Smoke test no CI, em Windows e Linux | Abre o `.exe` a partir de uma pasta copiada, espera o health, fecha e confere que nada foi gravado fora da pasta |

- TST-01: Testes organizados por funcionalidade, espelhando o módulo (`imports/ImportClassifierTest`).
- TST-02: Nome em inglês pelo comportamento, com o id da regra: `@DisplayName("RN-13: projected installment becomes MATCHED")`.
- TST-03: Corpo no padrão Given / When / Then.
- TST-04: Cobertura mínima no JaCoCo: 90% de linhas em `domain`, 80% em `application`; abaixo disso o build falha.
- TST-05: Builders de teste compartilhados (`BillEntryBuilder`, `CardBuilder`) em vez de montar objetos em cada teste (DRY).
- TST-06: Todo bug corrigido ganha antes um teste que reproduz o erro.
- TST-07: O CI (GitHub Actions) roda testes, Javadoc e cobertura a cada push; PR só entra com tudo verde.

**Matriz de rastreabilidade**

| Funcionalidade | Requisitos | Classes principais | Testes |
| --- | --- | --- | --- |
| Competência | RN-01 a RN-03 | `BillingPeriodPolicy` | `BillingPeriodPolicyTest` |
| Rotação de parcelas | RN-04 a RN-09 | `InstallmentProjector` | `InstallmentProjectorTest`, `ConfirmImportIT` |
| Deduplicação | RN-10 a RN-14 | `EntryKeyFactory`, `ImportClassifier` | `EntryKeyFactoryTest`, `ImportClassifierTest` |
| Alias | RF-46 a RF-51 | `DisplayDescriptionResolver`, `RenameEntryUseCase` | `DisplayDescriptionResolverTest`, `BillEntryControllerIT` |
| Importação Itaú | RF-60 a RF-73 | `ItauPdfParser`, `ItauXlsxParser`, `PreviewImportUseCase` | `ItauPdfParserTest`, `ItauXlsxParserTest`, `import.spec.ts` |
| Visão geral | RF-10 a RF-17 | `MonthlySummaryService` | `MonthlySummaryServiceTest`, `monthly-summary.spec.ts` |
| Rendas e despesas fixas | RF-21 a RF-24, RF-30 a RF-32 | `GenerateOccurrencesUseCase` | `GenerateOccurrencesUseCaseTest` |
| Rateio entre cotas | RF-26 | `SplitPolicy` | `SplitPolicyTest` |
| Exportação por cota | RF-80 a RF-94, TPL-01 a TPL-07 | `MemberStatementBuilder`, `SendStatementUseCase` | `MemberStatementBuilderTest`, `SendStatementUseCaseTest` |
| Idioma | I18N-01 a I18N-14 | `MessageSource`, `locales/*.json` | `MessageKeysConsistencyTest`, `i18n.spec.ts` |

**Critério de pronto por funcionalidade**

- [ ] Javadoc completo no `package-info.java` e nas classes públicas
- [ ] Testes de domínio e de caso de uso cobrindo cada RN citada
- [ ] Teste de integração do endpoint
- [ ] Linha atualizada na matriz de rastreabilidade
- [ ] CI verde (testes, Javadoc, cobertura)

## 16. Exportação por cota

Para cada competência, o sistema gera um extrato por cota e o envia por e-mail, WhatsApp ou link, sempre no layout de um template visual que o usuário personaliza.

**Cadastro da cota**

- RF-80: A conta (cota) passa a ter e-mail, telefone com DDI para WhatsApp, chave PIX opcional para receber o valor e idioma preferido (seção 17).

**Conteúdo do extrato**

- RF-81: Selecionar a competência e uma ou mais cotas; o sistema gera um extrato por cota.
- RF-82: O extrato traz: cabeçalho (cota, competência, data de emissão); resumo (total devido, renda e saldo quando aplicável); lançamentos da cota agrupados por cartão (data, descrição exibida, parcela, valor e % do rateio); despesas fixas e dinâmicas da cota; parcelas comprometidas nos próximos meses; total geral; dados para pagamento (PIX) e uma observação livre.
- RF-83: Opções por exportação: mostrar ou ocultar categorias, usar alias ou descrição original, incluir ou não parcelas futuras.
- RF-84: Formatos: PDF (padrão), PNG (para colar no WhatsApp) e XLSX/CSV (para quem quer conferir em planilha).
- RF-85: Prévia do extrato antes de enviar ou baixar.

**Envio**

| Canal | Como funciona | Requisito |
| --- | --- | --- |
| E-mail | SMTP configurado na tela de ajustes (ex.: Gmail com senha de app), assunto e corpo do template, PDF anexo | RF-86 |
| WhatsApp | Abre o WhatsApp Desktop ou Web em `wa.me/<telefone>` com a mensagem resumida; o arquivo é salvo e copiado para a área de transferência para colar na conversa | RF-87 |
| Área de transferência | Copia o PNG do extrato para colar em qualquer app | RF-88 |
| Pasta | Salva em `data/exports/<billing-period>/` ou num local escolhido e abre a pasta | RF-89 |
| Impressão | Abre o PDF no leitor padrão do sistema | RF-90 |

- RF-91: Envio em lote: todas as cotas selecionadas de uma vez, com status por cota.
- RF-92: Histórico de envios: cota, competência, canal, data, status (enviado, aguardando o usuário, falhou), versão do template; permite reenviar.
- RF-93: Se a competência mudar depois do envio (nova importação, edição), o histórico marca o extrato como desatualizado.
- RF-94: Envio automático pela API oficial do WhatsApp Business fica fora da v1, por exigir conta Meta Business e ter custo por mensagem.

**Template visual personalizável**

- TPL-01: Um único template HTML (Thymeleaf) gera o corpo do e-mail, o PDF (OpenHTMLtoPDF) e o PNG (página do PDF renderizada pelo PDFBox) — um layout, três saídas (DRY).
- TPL-02: Personalização sem código: logo (upload), cores primária e secundária, fonte de uma lista, texto de cabeçalho e rodapé, mensagem padrão, e quais blocos do RF-82 aparecem e em que ordem.
- TPL-03: Editor com pré-visualização ao vivo, usando dados reais da competência ou dados de exemplo, com seletor de idioma (pt-BR / en-US).
- TPL-04: Vários templates salvos, um marcado como padrão; cada cota pode ter um template próprio.
- TPL-05: Mensagens de texto (assunto do e-mail, texto do WhatsApp) com variáveis em inglês: `{{member.name}}`, `{{billingPeriod}}`, `{{total}}`, `{{dueDate}}`, `{{pixKey}}`; o texto em volta tem uma versão por idioma (I18N-11).
- TPL-06: Cada alteração gera uma nova versão do template; o envio guarda a versão e o idioma usados para reproduzir o documento depois.
- TPL-07: Modo avançado opcional para editar HTML/CSS, com sanitização e apenas as variáveis permitidas.

**Responsabilidades (SRP)**

| Classe | Única responsabilidade |
| --- | --- |
| `MemberStatementBuilder` | Montar os dados do extrato de uma cota numa competência |
| `TemplateRenderer` | Aplicar template + dados + `Locale` da cota e devolver HTML |
| `PdfGenerator` / `ImageGenerator` | Converter HTML em PDF e PDF em PNG |
| `DeliveryChannel` (interface) | Contrato de envio; implementações `EmailChannel`, `WhatsAppLinkChannel`, `ClipboardChannel`, `FolderChannel` |
| `SendStatementUseCase` | Orquestrar montar → renderizar → gerar → enviar → registrar histórico |

Link público de compartilhamento fica fora do escopo: o app roda offline, sem servidor acessível de fora. Novas entidades: `Template`, `TemplateVersion`, `StatementDelivery`. A configuração de SMTP fica na tela de ajustes, com a senha cifrada no banco. Cada extrato sai no idioma preferido da cota (I18N-08). Testes: `MemberStatementBuilderTest`, `TemplateRendererTest` (snapshot do HTML em pt-BR e en-US), `SendStatementUseCaseTest` e e-mail de integração com GreenMail.

## 17. Idioma e internacionalização

Todo o código é escrito em inglês; a interface e os documentos enviados existem em português (pt-BR) e inglês (en-US), e cada extrato sai no idioma da cota que vai recebê-lo.

**Código em inglês**

- I18N-01: Em inglês: pacotes, classes, métodos, variáveis, tabelas e colunas do banco, URLs e campos JSON da API, valores de enum, chaves de tradução, nomes de pastas e arquivos do app, Javadoc, nomes de testes e mensagens de commit.
- I18N-02: Este documento de requisitos continua em português; os termos de domínio seguem o glossário abaixo, e o código usa sempre o nome em inglês.

**Interface em dois idiomas**

- I18N-03: Idiomas disponíveis: português (pt-BR) e inglês (en-US). O padrão vem do idioma do sistema operacional; a troca fica em Ajustes, vale na hora e é salva em `data/`.
- I18N-04: Front com react-i18next e um arquivo por idioma (`locales/pt-BR.json`, `locales/en-US.json`); nenhum texto fixo em componente, verificado pela regra de lint `i18next/no-literal-string`.
- I18N-05: Números, datas e moeda formatados com `Intl` pelo idioma: R$ 1.234,56 e 26/10/2026 em pt-BR; R$1,234.56 e 10/26/2026 em en-US. A moeda é sempre BRL.
- I18N-06: Backend com `MessageSource` (`messages_pt_BR.properties`, `messages_en_US.properties`); mensagens de `ProblemDetail` e da Bean Validation seguem o `Accept-Language` enviado pela interface.
- I18N-07: Dados do usuário (descrições, aliases, nomes de categorias e contas) não são traduzidos. As categorias iniciais são criadas no idioma escolhido no primeiro uso.

**Documento de envio**

- I18N-08: Cada cota tem um idioma preferido (pt-BR ou en-US); o extrato, o PDF, o PNG, o assunto e o corpo do e-mail e a mensagem do WhatsApp saem nesse idioma, independentemente do idioma da interface.
- I18N-09: Na exportação, o usuário pode trocar o idioma só daquele envio.
- I18N-10: O template visual é um só; os rótulos fixos ("Total", "Vencimento", "Parcelas futuras") vêm do `MessageSource` com o `Locale` da cota, e números e datas seguem o I18N-05.
- I18N-11: Os textos personalizáveis do template (cabeçalho, rodapé, mensagem padrão) têm uma versão por idioma; se faltar a do idioma da cota, usa a outra e avisa na prévia.
- I18N-12: O histórico de envios guarda o idioma usado, junto com a versão do template.

**Testes**

- I18N-13: Teste no CI confere que as mesmas chaves existem nos dois idiomas, no front e no backend.
- I18N-14: Snapshot do extrato renderizado nos dois idiomas; Playwright roda os fluxos principais em pt-BR e en-US.

**Glossário de nomes no código**

| Termo no documento | Nome no código |
| --- | --- |
| Competência | `BillingPeriod` |
| Conta / cota | `Member` |
| Cartão / ciclo do cartão | `Card` / `CardCycle` |
| Fatura | `CardBill` |
| Compra | `Purchase` |
| Lançamento | `BillEntry` |
| Parcela (`n/N`) | `Installment` (`installmentNumber` / `installmentCount`) |
| Compra única / parcelada / assinatura | `SINGLE` / `INSTALLMENT` / `SUBSCRIPTION` |
| Categoria | `Category` |
| Alias / renomear só um lançamento | `Alias` / `AliasOverride` |
| Descrição original / normalizada / exibida | `originalDescription` / `normalizedDescription` / `displayDescription` |
| Renda / despesa fixa / despesa dinâmica | `Income` / `FixedExpense` / `VariableExpense` |
| Modelo recorrente / ocorrência do mês | `RecurrenceRule` / `Occurrence` |
| Rateio | `Split` |
| Importação / lote / prévia | `Import` / `ImportBatch` / `ImportPreview` |
| Novo / duplicado / conciliado / divergente | `NEW` / `DUPLICATE` / `MATCHED` / `DIVERGENT` |
| Projetada / confirmada / não confirmada | `PROJECTED` / `CONFIRMED` / `UNCONFIRMED` |
| Resumo mensal | `MonthlySummary` |
| Extrato da cota / envio | `MemberStatement` / `StatementDelivery` |
| Template / versão do template | `Template` / `TemplateVersion` |
| Fila de validação / pendente / validado | `ReviewQueue` / `PENDING_REVIEW` / `REVIEWED` |

## 18. Planilha mensal (XLSX)

Para a competência selecionada, o app gera um `.xlsx` com as mesmas abas, tabelas, colunas, fórmulas e visual do modelo enviado ("2026 - 11 - NOVEMBRO CJ"), já preenchido com os dados do mês.

| Aba | Conteúdo | Origem no app |
| --- | --- | --- |
| Resumo | % da renda gasta, renda total, despesas mensais, despesas de cartão, saldo e gastos por categoria com filtro de Cota | Fórmulas sobre as outras abas |
| Renda mensal | Tabela `Receita` (Item, Valor) e, ao lado, quanto cada cota de terceiro deve (Cota, Pessoa, Valor) | Rendas da competência e totais por cota |
| Despesas mensais | Tabela `Despesas` (Item, Valor) e total por cartão | Despesas fixas e dinâmicas pagas fora do cartão |
| Cartões | Tabela `CARTÕES` (Cartão, Vencimento, Cota, Parcela, Descrição, Valor, Categoria) agrupada por cartão, total geral e total por cota | Lançamentos da competência e boletos com cota |
| Uma aba por cota escolhida (ex.: "MÃE") | Blocos por mês com Cartão, Vencimento, Parcela, Descrição, Valor e total do mês | Lançamentos e parcelas projetadas da cota |
| Listas | Tabela `Categorias`, usada na lista suspensa da coluna Categoria | Cadastro de categorias |

**Conteúdo**

- XLS-01: Exportar a partir do seletor de competência; nome do arquivo `AAAA - MM - <MÊS> <sufixo>.xlsx` (ex.: `2026 - 11 - NOVEMBRO CJ.xlsx`), com sufixo configurável.
- XLS-02: Colunas da aba Cartões no padrão do modelo: Vencimento = dia de vencimento do cartão; Cota = sigla da cota (AB, T, M, AP); Parcela = `-` para compra única, `*` para assinatura, `NN/NN` para parcela; Descrição = descrição exibida (alias).
- XLS-03: Lançamentos agrupados por cartão, na ordem configurada, com uma linha em branco entre grupos. Despesas pagas por boleto ou débito que tenham cota entram no grupo "Boletos" da aba Cartões, como no modelo; as demais vão para Despesas mensais.
- XLS-04: Cotas têm sigla e tipo: **principal** (a casa, ex.: AB) ou **terceiro** (quem reembolsa, ex.: M, T, AP). O total de cada cota de terceiro entra na Renda mensal como reembolso, por fórmula que aponta para a aba Cartões. A visão geral do app (RF-10) segue a mesma regra.
- XLS-05: Categoria preenchida para a cota principal e vazia para cotas de terceiros, como no modelo.
- XLS-06: Aba por cota (como "MÃE"): gerada para as cotas marcadas em Ajustes, com blocos dos meses vizinhos (padrão: 2 anteriores, o atual e 1 seguinte, como no modelo) e o total de cada bloco.
- XLS-07: Lançamentos pendentes de validação entram destacados em amarelo, e o app avisa antes de exportar (como no RF-79b).

**Fórmulas e compatibilidade**

- XLS-08: Os totais são fórmulas, não valores: `=SUM` do total geral, `=SUMIF` por cota, % da renda gasta e saldo no Resumo, nomes definidos `TotalMensaldeRenda` e `TotalMensaldeDespesas`, e as tabelas nomeadas `Receita`, `Despesas`, `CARTÕES` e `Categorias`. Quem editar a planilha vê os totais se ajustarem.
- XLS-09: A planilha sai em um de dois **modos de fórmula**, escolhido na exportação. O padrão segue o sistema (Windows → Excel 365, Linux → Compatível) e pode ser trocado em Ajustes ou só naquele envio, por exemplo ao mandar a planilha para alguém que usa outro programa.
- XLS-10: Trocar o modo muda só as fórmulas e a tabela de categorias; dados, abas, visual e totais são os mesmos. A prévia da exportação mostra o modo escolhido.
- XLS-11: Listas suspensas: Categoria (tabela `Categorias`) e Cota (siglas cadastradas).
- XLS-12: O app corrige o que está fixo ou quebrado no modelo: o ano e o título ("ORÇAMENTO DE \<MÊS>") vêm da competência, os nomes dos meses são gerados (sem erros como "MOVEMBRO") e nomes definidos com `#REF!` não são copiados. Abas "DetalhesN", criadas pelo Excel ao clicar na tabela dinâmica, não fazem parte da exportação.

| Modo | Padrão em | Total por cartão (Despesas mensais) | Gastos por categoria (Resumo) |
| --- | --- | --- | --- |
| Excel 365 | Windows | `SORT`, `UNIQUE` e `FILTER`, igual ao modelo | Tabela dinâmica de verdade, com filtro por Cota e ordem decrescente |
| Compatível (LibreOffice e Excel antigo) | Linux | Lista de cartões já ordenada e um `SUMIF` por linha | Tabela de `SUMIFS` com a Cota em lista suspensa, ordenada na exportação |

Versões recentes do LibreOffice já leem parte das funções do Excel 365, mas o modo Compatível não depende da versão instalada.

**Visual personalizável**

- XLS-13: O visual vem de um arquivo-modelo `.xlsx` guardado em `data/templates/`: cores das abas (`#F38BA0`, `#EDF6E5`, `#B5EAEA`, `#FFBCBC`), títulos em faixa colorida, estilo de tabela "Orçamento mensal simples", formatos `R$ #,##0.00` e `0%` e larguras de coluna. O app copia o modelo e preenche os dados (Apache POI), em vez de desenhar o estilo em código.
- XLS-14: O usuário pode trocar o arquivo-modelo por outro, desde que mantenha as abas e tabelas nomeadas; o app valida isso e avisa o que falta.
- XLS-15: Idioma: pt-BR segue o modelo ao pé da letra; en-US traduz nomes das abas, títulos, cabeçalhos e meses, mantendo a estrutura (seção 17).

**Implementação**

- XLS-16: `MonthlyWorkbookExporter` orquestra; `WorkbookTemplate` carrega e valida o modelo; um `SheetWriter` por aba (`SummarySheetWriter`, `IncomeSheetWriter`, `ExpensesSheetWriter`, `CardsSheetWriter`, `MemberSheetWriter`, `ListsSheetWriter`). Uma aba nova = um `SheetWriter` novo, sem mexer nos outros. As fórmulas que mudam entre modos vêm de uma estratégia `FormulaDialect` (`Excel365Dialect`, `CompatibleDialect`) usada pelos `SheetWriter`, então um modo novo não exige mudar as abas.
- XLS-17: Na API, o resumo mensal ganha o link `export-xlsx` (`/api/summaries/{billingPeriod}/workbook`).
- XLS-18: Teste `MonthlyWorkbookExporterTest`: gera a planilha nos dois modos; no Compatível, recalcula com LibreOffice sem interface e confere os totais com o resumo do app; no Excel 365, confere fórmulas, tabelas nomeadas e a tabela dinâmica gerada.

## 19. Implementação com agente LLM

O agente que implementar o sistema (ex.: Claude Code) começa gerando um plano com tudo o que há para fazer, a partir deste documento, e trabalha marcando e atualizando esse plano a cada iteração.

**Arquivos no repositório**

| Arquivo | Conteúdo | Quem altera |
| --- | --- | --- |
| `docs/SPEC.md` | Esta especificação exportada em Markdown | Só o usuário |
| `docs/IMPLEMENTATION_PLAN.md` | Plano com cada requisito como item marcável, decisões, dúvidas e registro de iterações | O agente, a cada iteração |
| `CLAUDE.md` (ou `AGENTS.md`) | Regras de trabalho do agente: ler o plano ao começar, seguir esta seção, padrões de código das seções 13, 15 e 17 | O usuário |

**Plano inicial**

- AGT-01: A primeira entrega do agente é o plano completo, escrito de uma vez e antes de qualquer código: tudo o que será feito no projeto inteiro, tendo o `SPEC.md` como única base. Nada de planejar só o próximo marco.
- AGT-02: Cobertura total: todo id de requisito (RF, RN, RNF, POR, API, UI, DOC, TST, TPL, I18N, XLS, AGT) vira pelo menos um item, e também entram as tarefas que os requisitos implicam sem nomear (estrutura do repositório, migrações Flyway, fixtures mascaradas, configuração do CI, arquivos de tradução, arquivo-modelo da planilha).
- AGT-03: Cada item traz: id de origem, tarefa curta, módulo, critério de aceite (resumido do requisito, com a referência), testes previstos, dependências e estado. Ex.: `- [ ] RN-04 — Project remaining installments · entry · accept: n+1…N created as PROJECTED · InstallmentProjectorTest · after RN-01`.
- AGT-04: Itens agrupados em marcos e ordenados por dependência. Ordem sugerida: M0 base (repositório, CI, esqueleto Tauri + Spring Boot, pacote portátil); M1 domínio e cadastros (`Money`, `BillingPeriod`, cotas, cartões, categorias); M2 parsers Itaú com fixtures; M3 deduplicação, parcelas, estornos e fila de validação; M4 lançamentos e alias; M5 rendas, despesas e visão geral; M6 extrato por cota e planilha mensal; M7 idiomas, empacotamento final e testes ponta a ponta.
- AGT-05: Aprovação antes de começar: o agente entrega o plano, lista as dúvidas encontradas ao ler o `SPEC.md` e só começa o M0 depois que o usuário aprovar o plano.
- AGT-06: Depois de aprovado, o plano só ganha itens novos quando forem subtarefas de um item existente (com o id de origem) ou vierem de uma mudança aprovada no `SPEC.md` (AGT-11). Escopo novo sem origem na especificação não entra.
- AGT-07: O topo do plano mostra o progresso por marco (feitos / total), atualizado pelo agente a cada iteração.
- AGT-08: Um teste no CI confere que todo id do `SPEC.md` aparece no plano; requisito sem item quebra o build.

**Ciclo de cada iteração**

1. Ler o plano e o registro de iterações; retomar o item em andamento ou pegar o próximo item livre de dependências.
2. Marcar o item como em andamento (`[ ]` com a etiqueta `(in progress)`).
3. Escrever ou ajustar os testes do item e depois implementar.
4. Rodar o build completo: testes, Javadoc e cobertura (seção 15).
5. Com tudo verde, marcar `[x]`, anotar o hash do commit e registrar a iteração (data, itens, decisões, desvios).
6. Um item, ou um grupo pequeno de itens ligados, por commit.

**Dúvidas e desvios**

- AGT-09: Item pronto = critério de aceite do item e critério de pronto da seção 15 cumpridos. Marcar `[x]` sem isso não vale.
- AGT-10: Ambiguidade ou falta de informação: o agente não inventa regra. Registra a pergunta na seção "Open questions" do plano, marca o item com `(blocked: …)` e segue para outro item livre.
- AGT-11: Quando precisar se afastar da especificação (limitação de biblioteca, por exemplo), o agente registra na seção "Decisions" o que fez e por quê. O `SPEC.md` só muda quando o usuário aprova; aí o agente atualiza o texto aprovado, acrescenta ou reabre os itens afetados e registra a mudança na iteração.
- AGT-12: O plano é escrito em inglês, como o código (I18N-01); os ids de requisito ficam iguais aos deste documento.

Esqueleto do plano:

```markdown
# Implementation Plan

Progress: M0 4/6 · M1 0/18 · M2 0/14 · …

## M0 — Foundation
- [x] POR-15 — CI matrix Windows + Linux · build · smoke test (a1b2c3d)
- [ ] POR-08 — Start backend sidecar from Tauri (in progress) · shell · PortableStartIT
- [ ] API-07 — Root resource /api with links · api · RootControllerIT

## Open questions
- RF-64f: year inference when the bill spans New Year — blocks RF-64f

## Decisions
- 2026-11-02: SQLite dialect from hibernate-community-dialects (POR-11)

## Iteration log
- 2026-11-02 — POR-15 done; CI green on both runners
```

## 20. Pontos em aberto

As decisões abaixo mudam regras ou telas e precisam de resposta antes da implementação.

- [x] "Cota" no lançamento: interpretei como **conta** (pessoa/divisão). Confirmado: cota = conta; a exportação por cota (seção 16) usa esse cadastro.
- [x] Escopo padrão do renomear (RF-48): só este lançamento ou todos com a mesma descrição?
- [x] Assinaturas `*`: projetar só o próximo mês (RN-09) ou até cancelamento manual?
- [x] Estornos: abater do total da categoria original ou aparecer como categoria "Estornos"?
- [x] Despesa fixa paga no cartão (RF-33): vincular automaticamente pela descrição ou sempre manual?
- [x] Faturas de exemplo: 2–3 PDFs e 2–3 XLSX do Itaú, mascarados, para definir os parsers.
- [x] Além de Windows e Linux x86\_64, suportar macOS e Linux ARM (ex.: Raspberry Pi)?
