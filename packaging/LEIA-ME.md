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

1. Feche o app.
2. Substitua os executáveis (`PlanejamentoMensal.exe`, `PlanejamentoMensal.AppImage`, `start-linux.sh`) e a pasta
   `backend/` pelos da versão nova. **Não apague `data/`.**
3. Abra o app: ele faz uma cópia do banco e atualiza a estrutura sozinho.

---

# Planejamento Mensal (English)

Portable household finance app. Everything, including your data (`data/`), lives in this folder, which opens on
Windows and Linux.

- **Open:** `PlanejamentoMensal.exe` on Windows; `PlanejamentoMensal.AppImage` on Linux, or `sh start-linux.sh` when the
  AppImage cannot run (no FUSE, or a drive mounted `noexec`).
- **Backups:** one copy per start in `data/backups/` (latest 10 kept); more in *Settings → Backups and data*. To restore,
  close the app and replace `data/planejamento-mensal.db` with a copy.
- **Removing the drive:** close the app first.
- **Updating:** close the app, replace the executables and `backend/`, keep `data/`, open the app again; it backs up and
  migrates the database by itself.
