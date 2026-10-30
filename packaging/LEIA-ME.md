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

## Cópias de segurança

- A cada abertura o app copia o banco para `data/backups/` e guarda as 10 últimas.
- Em **Ajustes → Cópias de segurança e dados** dá para fazer uma cópia agora (na pasta ou em outro lugar) e
  exportar todos os dados em JSON ou CSV.
- Para restaurar: feche o app e substitua `data/planejamento-mensal.db` pela cópia desejada de `data/backups/`.

## Antes de remover o SSD

Feche o app e espere a janela sumir. Assim nenhuma gravação fica pela metade. Se o SSD sair com o app aberto, o app
avisa que não consegue gravar; reconecte e abra de novo.

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
- **Backups:** one copy per start in `data/backups/` (latest 10 kept); more in *Settings → Backups and data*. To restore,
  close the app and replace `data/planejamento-mensal.db` with a copy.
- **Removing the drive:** close the app first.
- **Updating:** when a new version exists the app shows a notice at the top of the window. *Update* downloads it and
  checks it; *Restart now* (or simply the next start) installs it, backs up and migrates the database, and goes back
  to the previous version if the new one does not open. It is never forced, and `data/` is never changed. The
  `update/` folder holds the download and, during the swap, the previous version (`update/previous/`). By hand
  (offline, or coming from 0.1.0 or 0.2.0): close the app, replace the executables and `backend/` with those of the new zip,
  keep `data/`.
