package com.planejamentomensal.shared.api;

import org.springframework.hateoas.RepresentationModel;

/**
 * Debug mode and where the log is written (RNF-11).
 *
 * @since 1.0
 */
public class DiagnosticsModel extends RepresentationModel<DiagnosticsModel> {

    /** {@code true} while the debug mode is on. */
    public final boolean debugMode;
    /** Folder of the backend log, to open from the settings. */
    public final String logFolder;

    /**
     * Creates the model.
     *
     * @param debugMode current mode
     * @param logFolder folder of the backend log
     */
    public DiagnosticsModel(boolean debugMode, String logFolder) {
        this.debugMode = debugMode;
        this.logFolder = logFolder;
    }
}
