package dev.enginehost.plugin.rpgmaker.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/** Hosts a deployed RPG Maker MV/MZ web game directly from its existing folder. */
public final class RunActivity extends Activity {
    private WebView webView;
    private File gameRoot;
    private boolean allowNetwork;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        String context = getIntent().getStringExtra("engineContext");
        if (!"mv".equals(context) && !"mz".equals(context)) {
            fail("Unsupported RPG Maker engineContext: " + context);
            return;
        }

        String path = getIntent().getStringExtra("path");
        if (path == null || !(gameRoot = new File(path)).isDirectory()) {
            fail("enginehost did not provide a valid game folder");
            return;
        }

        JSONObject options;
        try {
            String raw = getIntent().getStringExtra("options");
            options = raw == null || raw.isBlank() ? new JSONObject() : new JSONObject(raw);
        } catch (JSONException error) {
            fail("RPG Maker options must be a JSON object");
            return;
        }

        String entry = getIntent().getStringExtra("execFile");
        if (entry == null || entry.isBlank()) entry = options.optString("entryPoint", "index.html");
        File entryFile;
        try {
            gameRoot = gameRoot.getCanonicalFile();
            entryFile = new File(gameRoot, entry).getCanonicalFile();
            String rootPrefix = gameRoot.getPath() + File.separator;
            if (!entryFile.getPath().startsWith(rootPrefix) || !entryFile.isFile()) {
                fail("RPG Maker entry point is missing or outside the game folder");
                return;
            }
        } catch (IOException error) {
            fail("Unable to resolve the RPG Maker game folder");
            return;
        }

        allowNetwork = options.optBoolean("allowNetwork", false);
        WebView.setWebContentsDebuggingEnabled(options.optBoolean("webContentsDebugging", false));
        webView = new WebView(this);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onConsoleMessage(ConsoleMessage message) {
                android.util.Log.d("enginehost-rpgmaker", message.message());
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme())) return !isInsideGame(uri);
                return !allowNetwork || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()));
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(options.optBoolean("domStorage", true));
        webView.getSettings().setDatabaseEnabled(options.optBoolean("database", true));
        webView.getSettings().setMediaPlaybackRequiresUserGesture(
            options.optBoolean("mediaPlaybackRequiresUserGesture", false));
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(allowNetwork);
        String userAgent = options.optString("userAgent", "");
        if (!userAgent.isBlank()) webView.getSettings().setUserAgentString(userAgent);

        setContentView(webView);
        webView.loadUrl(Uri.fromFile(entryFile).toString());
    }

    private boolean isInsideGame(Uri uri) {
        try {
            File candidate = new File(uri.getPath()).getCanonicalFile();
            return candidate.equals(gameRoot) || candidate.getPath().startsWith(gameRoot.getPath() + File.separator);
        } catch (IOException | NullPointerException ignored) {
            return false;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    private void fail(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
