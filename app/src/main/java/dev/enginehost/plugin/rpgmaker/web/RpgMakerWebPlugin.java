package dev.enginehost.plugin.rpgmaker.web;

import android.annotation.SuppressLint;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import dev.enginehost.api.EngineControllerEvent;
import dev.enginehost.api.EnginePlugin;
import dev.enginehost.api.EnginePluginSession;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * RPG Maker MV and MZ games, run from their own folder in a confined browser.
 *
 * The game is served over a private https origin (see {@link GameServer}),
 * which is what its scripts expect of a web deploy. Saves never live in the
 * WebView: {@link LocalStorageBridge} replaces localStorage, which MV saves
 * into directly and MZ through localforage, with a store whose file sits in
 * the save folder Enginehost chose for this game.
 */
public final class RpgMakerWebPlugin implements EnginePlugin {
    private static final String TAG = "enginehost-rpgmaker";

    /**
     * The button names MV and MZ read, from Input.keyMapper and
     * Input.gamepadMapper (rpg_core.js, rmmz_core.js). Enginehost's MV/MZ
     * actions are these names with "mvmz_" in front.
     */
    private static final Set<String> BUTTONS = new HashSet<>(Arrays.asList(
        "up", "down", "left", "right", "ok", "cancel", "menu", "shift", "control",
        "escape", "tab", "debug", "pageup", "pagedown"));

    /** A stick is its four directions, negative half first, as the engine's own pad code makes it. */
    private static final Map<String, List<String>> STICKS = new HashMap<>();
    static {
        STICKS.put("left_x", Arrays.asList("left", "right"));
        STICKS.put("left_y", Arrays.asList("up", "down"));
        STICKS.put("right_x", Arrays.asList("left", "right"));
        STICKS.put("right_y", Arrays.asList("up", "down"));
    }

    private WebView webView;
    private GameServer server;
    private LocalStorageBridge storage;
    /** The button each action is holding down now, if any. */
    private final Map<String, String> held = new HashMap<>();
    /** The actions holding each button down: it is up again when the last lets go. */
    private final Map<String, Set<String>> holders = new HashMap<>();

    @Override
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public void onCreate(EnginePluginSession session) throws Exception {
        String context = session.engineContext();
        if (!"rpgmaker".equals(session.engine()) || !("mv".equals(context) || "mz".equals(context))) {
            throw new IOException("This runtime runs RPG Maker MV and MZ, not " + session.engine() + " " + context);
        }
        File gameRoot = new File(session.gamePath()).getCanonicalFile();
        if (!gameRoot.isDirectory()) throw new IOException("The game folder is not readable");
        JSONObject options = new JSONObject(session.optionsJson() == null ? "{}" : session.optionsJson());

        storage = new LocalStorageBridge(new File(session.host().saveDirectory(), "localStorage.json"),
            (priority, message, error) -> session.host().log(priority, TAG, message, error));
        server = new GameServer(gameRoot, session.execFile(), options);

        webView = new WebView(session.host().context());
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // sessionStorage; saves go through the bridge
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(options.optBoolean("mediaPlaybackRequiresGesture", false));
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        String userAgent = options.optString("userAgent", "");
        if (!userAgent.isBlank()) settings.setUserAgentString(userAgent);
        WebView.setWebContentsDebuggingEnabled(options.optBoolean("webContentsDebugging", false));
        webView.setBackgroundColor(0xFF000000);
        webView.addJavascriptInterface(storage, LocalStorageBridge.JS_NAME);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                int priority = message.messageLevel() == ConsoleMessage.MessageLevel.ERROR ? android.util.Log.ERROR : android.util.Log.DEBUG;
                session.host().log(priority, TAG, message.message() + " (" + message.sourceId() + ":" + message.lineNumber() + ")", null);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !server.serves(request.getUrl());
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                try {
                    return server.respond(request);
                } catch (IOException error) {
                    session.host().log(android.util.Log.WARN, TAG, "Could not serve " + request.getUrl(), error);
                    return GameServer.status(500, "Internal error");
                }
            }
        });
        session.display().addView(webView, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.loadUrl(server.entryUrl());
    }

    /**
     * An Enginehost action, held in the game's own Input under the name MV
     * and MZ give it. The names cannot go in as keys: the keyboard has no key
     * for cancel or menu, only Escape, which the engine reads as both
     * (Input._isEscapeCompatible), so the state the game's Input keeps is
     * where a pad's cancel, menu and escape stay three different buttons.
     * That is also where the engine's own gamepad code puts them
     * (Input._updateGamepadState), and Input.update makes a press, a
     * trigger and a repeat out of it exactly as for a key.
     */
    @Override public boolean onControllerEvent(EngineControllerEvent event) {
        if (webView == null) return false;
        String action = event.action();
        String wanted;
        List<String> stick = STICKS.get(action);
        if (stick != null) {
            // The engine's own threshold for a stick direction.
            float value = event.value();
            wanted = value < -0.5f ? stick.get(0) : value > 0.5f ? stick.get(1) : null;
        } else if (action.startsWith("mvmz_") && BUTTONS.contains(action.substring(5))) {
            wanted = event.pressed() ? action.substring(5) : null;
        } else {
            return false;
        }
        String before = held.get(action);
        if (before != null && before.equals(wanted)) return true;
        if (before != null) {
            held.remove(action);
            Set<String> holding = holders.get(before);
            if (holding != null && holding.remove(action) && holding.isEmpty()) setButton(before, false);
        }
        if (wanted != null) {
            held.put(action, wanted);
            Set<String> holding = holders.get(wanted);
            if (holding == null) holders.put(wanted, holding = new HashSet<>());
            if (holding.isEmpty()) setButton(wanted, true);
            holding.add(action);
        }
        return true;
    }

    /** [name] is one of {@link #BUTTONS}, so it is safe to write into the script. */
    private void setButton(String name, boolean down) {
        webView.evaluateJavascript("(function(){var i=window.Input;"
            + "if(i&&i._currentState)i._currentState['" + name + "']=" + down + ";})()", null);
    }

    @Override public void onResume() { if (webView != null) webView.onResume(); }

    @Override public void onPause() {
        if (webView != null) webView.onPause();
        if (storage != null) storage.flush();
    }

    @Override public void onDestroy() {
        if (storage != null) storage.flush();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
    }
}
