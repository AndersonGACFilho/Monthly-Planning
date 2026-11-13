# Planejamento Mensal

Aplicativo portátil de finanças da casa: rendas, despesas, faturas do Itaú e extratos por cota. Tudo fica nesta
pasta, inclusive os dados (`data/`). A mesma pasta abre no Windows e no Linux.

## Abrir

- **Windows:** dê dois cliques em `PlanejamentoMensal.exe`.
- **Linux:** dê dois cliques em `PlanejamentoMensal.AppImage` (ou `PlanejamentoMensal`). Se não abrir — sistema sem
  FUSE ou SSD montado com `noexec`, comum em exFAT — rode `sh start-linux.sh`: ele copia o programa para uma pasta
  temporária, abre de lá e apaga a cópia ao fechar.

Só uma janela do app pode usar a pasta de cada vez. Se o app foi fechado à força, ele pergunta se pode liberar a
pasta na próxima abertura.

## Encontrar o app na busca do computador

Como o app não se instala, a busca do Windows ou do Linux não o encontra. Para que encontre, abra o app neste
computador e use **Ajustes → Menu do sistema → Adicionar ao menu do sistema**. Ele só faz isso quando você pede, e
é a única coisa que o app grava fora desta pasta:

- **Windows:** o atalho `Planejamento Mensal.lnk` no menu Iniciar do seu usuário.
- **Linux:** `planejamento-mensal.desktop` e o ícone `planejamento-mensal.png` em `~/.local/share/applications/`.
  Se o app foi aberto pelo `start-linux.sh`, o atalho também abre por ele.

O atalho vale só para o computador onde foi criado e aponta para esta pasta:

- **SSD desconectado:** o atalho não abre (o Windows avisa que a unidade não está disponível ou que o item foi
  movido; se ele oferecer apagar o atalho, responda **Não**). Conecte o SSD e tente de novo.
- **SSD com outra letra (`E:` virou `F:`) ou montado em outro lugar:** Ajustes avisa que o atalho aponta para outra
  pasta; use **Corrigir atalho**.
- **Atualizações** mantêm os nomes dos programas, então o atalho continua funcionando.
- **Remover do menu do sistema** apaga tudo o que foi gravado. Faça isso antes de devolver um computador emprestado.

## Cópias de segurança

- A cada abertura o app copia o banco para `data/backups/` e guarda as 10 últimas.
- **Exportar banco**, no topo da janela (ou Ctrl+Shift+E), baixa o banco inteiro num arquivo
  `planejamento-mensal-<data>.db`: uma cópia completa e consistente, para guardar em outro lugar ou abrir em qualquer
  leitor de SQLite.
- **Importar banco (.db)**, em **Ajustes → Cópias de segurança e dados**, traz de volta um arquivo exportado (ou
  qualquer banco do app): ele é conferido, guardado em `data/backups/` como "Importado" (fora da limpeza automática) e
  aberto para conferir, com a faixa amarela. Nada muda nos dados reais até você clicar em **Restaurar**.
- Em **Ajustes → Cópias de segurança e dados** dá para fazer uma cópia agora (na pasta ou em outro lugar) e
  exportar todos os dados em JSON, CSV ou como o banco (.db).
- **Conferir uma cópia:** na lista de cópias, **Abrir para conferir** mostra aquela cópia em todo o app, com uma
  faixa amarela no topo. É uma cópia da cópia: o arquivo do backup não muda, e o que for alterado enquanto ela está
  aberta é descartado ao clicar em **Voltar aos dados reais**.
- **Restaurar:** **Restaurar** troca os dados reais pela cópia escolhida. Antes, o app guarda os dados atuais em
  `data/backups/` (motivo "Antes de restaurar"), então dá para desfazer restaurando essa cópia.
- Sem abrir o app, também dá para restaurar à mão: feche o app e substitua `data/planejamento-mensal.db` pela cópia
  desejada de `data/backups/`.

## Antes de remover o SSD

Feche o app e espere a janela sumir. Assim nenhuma gravação fica pela metade. Se o SSD sair com o app aberto, o app
avisa que não consegue gravar; reconecte e abra de novo.

## Quando algo der errado

Em **Ajustes → Diagnóstico**, ligue o **Modo debug** e repita o que deu erro. O aviso de erro passa a mostrar o código,
o número da requisição e o endereço, com o botão **Copiar detalhes**, e fica na tela até ser fechado. O app grava os
detalhes no log, `data/logs/backend.log`, que abre pelo botão **Abrir pasta do log** (passando de 5 MB, ele vira
`backend.1.log` na abertura seguinte e só os três anteriores ficam); o número da requisição do aviso
aparece nas linhas do log daquele erro. Mande o que foi copiado e esse arquivo para quem for ajudar, e desligue o modo
depois: ele continua ligado entre aberturas do app.

## Atualizar

O app procura uma versão nova toda vez que abre (sem internet, ele só segue sem avisar). Quando há uma, aparece um
aviso no topo da janela:

1. **Novidades** mostra o que mudou. **Atualizar** baixa a versão nova e confere se o arquivo chegou inteiro.
2. **Reiniciar agora** fecha o app, troca os programas e abre a versão nova. Se preferir, feche o app normalmente: a
   troca acontece na próxima vez que ele abrir.
3. Ao abrir, o app faz uma cópia do banco em `data/backups/` e atualiza a estrutura sozinho.

Nada é obrigatório: o **X** esconde o aviso até a próxima abertura, e **Ajustes → Atualizações** mostra a versão
instalada e procura de novo. A pasta `update/` guarda o download e, durante a troca, a versão anterior; ela some
quando a versão nova abre. **`data/` nunca é alterado pela atualização.**

Se a versão nova não abrir, o app avisa e volta sozinho para a anterior. Se nem isso der certo, feche o app, copie de
volta para esta pasta o que estiver em `update/previous/` e abra de novo; seus dados estão em `data/` e há cópias em
`data/backups/`.

**Atualizar à mão** (sem internet, ou vindo das versões 0.1.0 e 0.2.0, que ainda não se atualizam sozinhas): feche o app,
substitua os executáveis (`PlanejamentoMensal.exe`, `PlanejamentoMensal`, `PlanejamentoMensal.AppImage`,
`start-linux.sh`) e a pasta `backend/` pelos do zip da versão nova, em
<https://github.com/AndersonGACFilho/Monthly-Planning/releases>. **Não apague `data/`.**

---

# Planejamento Mensal (English)

Portable household finance app. Everything, including your data (`data/`), lives in this folder, which opens on
Windows and Linux.

- **Open:** `PlanejamentoMensal.exe` on Windows; `PlanejamentoMensal.AppImage` on Linux, or `sh start-linux.sh` when the
  AppImage cannot run (no FUSE, or a drive mounted `noexec`).
- **Export database:** the button at the top of the window (or Ctrl+Shift+E) downloads the whole database as one
  `planejamento-mensal-<date>.db` file, a full and consistent copy. *Import database (.db)* in *Settings → Backups
  and data* brings such a file back: it is checked, kept in `data/backups/` as "Imported" (out of the automatic
  cleanup) and opened to check; the live data change only when you click *Restore*.
- **Backups:** one copy per start in `data/backups/` (latest 10 kept); more in *Settings → Backups and data*. Each copy
  can be opened to check it (changes made there are dropped when you go back to the live data) or restored; restoring
  saves the current data as a new backup first. Without the app, close it and replace `data/planejamento-mensal.db`
  with a copy.
- **Finding the app when searching the computer:** *Settings → System menu → Add to the system menu* writes, only
  on request, the one thing the app keeps outside this folder: `Planejamento Mensal.lnk` in your Start Menu
  (Windows), or `planejamento-mensal.desktop` and its icon in `~/.local/share/applications/` (Linux). It belongs to
  that computer only and does not open while the drive is unplugged; if the drive gets another letter or mount
  path, Settings offers *Fix shortcut*. Updates keep it working; *Remove from the system menu* deletes all of it.
- **Removing the drive:** close the app first.
- **When something goes wrong:** turn on *Settings → Diagnostics → Debug mode* and repeat what failed. Error notices
  then show the code, the request number and the address with *Copy details*, and stay until closed. The details go
  to `data/logs/backend.log` (*Open log folder*), where the request number of the notice marks the lines of that
  error; over 5 MB it becomes `backend.1.log` at the next start, and only the three previous logs are kept. Turn
  the mode off afterwards; it stays on between starts.
- **Updating:** when a new version exists the app shows a notice at the top of the window. *Update* downloads it and
  checks it; *Restart now* (or simply the next start) installs it, backs up and migrates the database, and goes back
  to the previous version if the new one does not open. It is never forced, and `data/` is never changed. The
  `update/` folder holds the download and, during the swap, the previous version (`update/previous/`). By hand
  (offline, or coming from 0.1.0 or 0.2.0): close the app, replace the executables and `backend/` with those of the new zip,
  keep `data/`.
