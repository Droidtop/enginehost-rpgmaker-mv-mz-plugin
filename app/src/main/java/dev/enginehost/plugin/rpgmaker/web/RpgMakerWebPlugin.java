package dev.enginehost.plugin.rpgmaker.web;

import android.webkit.WebView;
import dev.enginehost.api.EngineControllerEvent;
import dev.enginehost.api.EnginePlugin;
import dev.enginehost.api.EnginePluginSession;
import dev.enginehost.web.GameServer;
import dev.enginehost.web.GameWebView;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RPG Maker MV and MZ games, run from their own folder in a confined browser.
 *
 * The browser, the private https origin the game is served from (what its
 * scripts expect of a web deploy) and the save-folder localStorage, which MV
 * saves into directly and MZ through localforage, are Enginehost's plugin-web
 * ({@link GameWebView}), shared with the HTML core. What is this core's own:
 * the engine it accepts, where a deploy keeps its page, the audio stand-in,
 * the localforage hook, and the pad's buttons in the game's own Input.
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

    private static final GameServer.Site SITE = new GameServer.Site() {
        // A Windows deploy keeps the game under www; a web deploy is flat.
        @Override public String[] entryPages() { return new String[] {"www/index.html", "index.html"}; }

        @Override public File standIn(File missing) { return audioSibling(missing); }

        // RPG Maker MZ saves through localforage, which prefers IndexedDB;
        // point it at localStorage the moment the library appears so MZ
        // saves land in the same file as MV's.
        @Override public String pageScript() {
            return "(function(){var forage;\n"
                + "try{Object.defineProperty(window,'localforage',{configurable:true,\n"
                + " get:function(){return forage;},\n"
                + " set:function(v){forage=v;try{if(v&&v.config&&v.LOCALSTORAGE)v.config({driver:[v.LOCALSTORAGE]});}catch(e){}}});}catch(e){}\n"
                + "})();\n";
        }
    };

    private GameWebView game;
    /** The button each action is holding down now, if any. */
    private final Map<String, String> held = new HashMap<>();
    /** The actions holding each button down: it is up again when the last lets go. */
    private final Map<String, Set<String>> holders = new HashMap<>();

    @Override
    public void onCreate(EnginePluginSession session) throws Exception {
        String context = session.engineContext();
        if (!"rpgmaker".equals(session.engine()) || !("mv".equals(context) || "mz".equals(context))) {
            throw new IOException("This runtime runs RPG Maker MV and MZ, not " + session.engine() + " " + context);
        }
        game = GameWebView.open(session, TAG, SITE);
    }

    /**
     * RPG Maker MV's AudioManager.audioFileExt asks a mobile browser for
     * ".m4a" because iOS Safari could not decode Vorbis, while a Windows
     * deploy ships only the .ogg files; every sound then failed to load and
     * the game stopped at its title screen. The WebView decodes Vorbis, and
     * decodeAudioData reads the container from the bytes, so the sibling in
     * the other format is served when the requested one does not exist.
     */
    private static File audioSibling(File wanted) {
        String name = wanted.getName();
        String sibling;
        if (name.endsWith(".m4a")) sibling = name.substring(0, name.length() - 4) + ".ogg";
        else if (name.endsWith(".ogg")) sibling = name.substring(0, name.length() - 4) + ".m4a";
        else return null;
        File file = new File(wanted.getParentFile(), sibling);
        return file.isFile() ? file : null;
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
        if (game == null || game.view() == null) return false;
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
        WebView view = game.view();
        if (view == null) return;
        view.evaluateJavascript("(function(){var i=window.Input;"
            + "if(i&&i._currentState)i._currentState['" + name + "']=" + down + ";})()", null);
    }

    @Override public void onResume() { if (game != null) game.onResume(); }

    @Override public void onPause() { if (game != null) game.onPause(); }

    @Override public void onDestroy() { if (game != null) game.onDestroy(); }
}
