package dev.quark.ton.core.contract;

/**
 * Port of ton-core/src/contract/ComputeError.ts
 */
public class ComputeError extends RuntimeException {

    private final int exitCode;
    private final String debugLogs; // nullable
    private final String logs;      // nullable

    public ComputeError(String message, int exitCode) {
        this(message, exitCode, null, null);
    }

    public ComputeError(String message, int exitCode, String debugLogs, String logs) {
        super(message);
        this.exitCode = exitCode;
        this.debugLogs = (debugLogs != null && !debugLogs.isEmpty()) ? debugLogs : null;
        this.logs = (logs != null && !logs.isEmpty()) ? logs : null;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getDebugLogs() {
        return debugLogs;
    }

    public String getLogs() {
        return logs;
    }
}
