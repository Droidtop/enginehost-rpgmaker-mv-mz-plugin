package dev.enginehost.api;

import android.content.Context;
import java.io.File;

public interface EngineHost {
    Context context();
    File saveDirectory();
    File cacheDirectory();
    EngineFileSystem fileSystem();
    void log(int priority, String tag, String message, Throwable error);
    boolean rumbleController(int deviceId, long durationMs, int amplitude);
    void finish();
    /**
     * A link the game asks to open, offered to the person by Enginehost and never opened by the
     * core; returns at once, true when Enginehost took it. Added after the first hosts shipped:
     * an older host throws IncompatibleClassChangeError.
     */
    default boolean openLink(String url) { return false; }
}
