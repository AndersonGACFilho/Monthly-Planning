package com.planejamentomensal.shared.application;

/**
 * Which database the app works on: the live one in {@code data/}, or a copy of a backup opened to check it before
 * trusting it (IMP-58, POR-12). Changes made while a backup is open stay in the copy and are dropped when the live
 * data comes back; restoring a backup is what makes it the live data.
 *
 * @since 1.0
 */
public interface ActiveDatabase {

    /**
     * Database the app works on.
     *
     * @param backup file name of the backup opened, or {@code null} for the live data
     */
    record State(String backup) {

        /**
         * Tells whether the app works on the live data.
         *
         * @return {@code true} when no backup is open
         */
        public boolean live() {
            return backup == null;
        }
    }

    /**
     * Returns the database the app works on.
     *
     * @return the state
     */
    State state();

    /**
     * Opens a copy of a backup of {@code data/backups/}, migrated to the current schema; the backup file itself is
     * never changed.
     *
     * @param fileName name of the backup file, as listed by {@link Backups#list()}
     * @return the new state
     * @throws com.planejamentomensal.shared.domain.NotFoundException for a name that is not a backup
     */
    State openBackup(String fileName);

    /**
     * Goes back to the live data and drops the copy of the backup that was open.
     *
     * @return the new state
     */
    State openLive();

    /**
     * Makes a backup the live data: the live data is backed up first ({@code before-restore}), then replaced by the
     * backup migrated to the current schema. The app works on the live data afterwards.
     *
     * @param fileName name of the backup file, as listed by {@link Backups#list()}
     * @return the new state
     * @throws com.planejamentomensal.shared.domain.NotFoundException for a name that is not a backup
     */
    State restore(String fileName);
}
