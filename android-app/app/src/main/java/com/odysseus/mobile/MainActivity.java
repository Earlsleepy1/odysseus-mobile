package com.odysseus.mobile;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.SSLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String DEFAULT_ENDPOINT = "http://10.0.2.2:7000";
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private String endpoint;
    private String sessionId;
    private LinearLayout messageList;
    private EditText messageInput;
    private Button sendButton;
    private TextView status;
    private ScrollView transcript;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        endpoint = getPreferences(MODE_PRIVATE).getString("endpoint", DEFAULT_ENDPOINT);
        showConnectionScreen();
    }

    private void showConnectionScreen() {
        LinearLayout root = page(Color.rgb(40, 44, 52));
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 24, 32, 24);

        TextView title = text("Odysseus Mobile", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWrap());

        TextView hint = text("Native Android client — local servers usually use http://", 15, Color.LTGRAY);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = fullWrap();
        hintParams.setMargins(0, 10, 0, 26);
        root.addView(hint, hintParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(endpoint);
        input.setHint("http://192.168.1.20:7000");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(input, fullWrap());

        Button connect = button("Connect");
        LinearLayout.LayoutParams connectParams = wrap();
        connectParams.gravity = Gravity.CENTER_HORIZONTAL;
        connectParams.setMargins(0, 20, 0, 0);
        root.addView(connect, connectParams);
        connect.setOnClickListener(view -> {
            String value = normalizeEndpoint(input.getText().toString());
            if (value.isEmpty()) {
                input.setError("Enter your Odysseus server address");
                return;
            }
            endpoint = value;
            getPreferences(MODE_PRIVATE).edit().putString("endpoint", endpoint).apply();
            loadNativeApp();
        });
        setContentView(root);
    }

    private void loadNativeApp() {
        showLoading("Connecting to Odysseus…");
        request("GET", "/api/auth/status", "", result -> {
            try {
                JSONObject auth = new JSONObject(result);
                if (auth.optBoolean("authenticated", false) || !auth.optBoolean("configured", true)) {
                    loadModelsAndSessions();
                } else {
                    showLoginScreen(auth.optBoolean("configured", true));
                }
            } catch (Exception error) {
                showError("Could not read server response", error);
            }
        });
    }

    private void showLoginScreen(boolean configured) {
        LinearLayout root = page(Color.rgb(40, 44, 52));
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 24, 32, 24);
        TextView title = text(configured ? "Sign in to Odysseus" : "Set up Odysseus", 25, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWrap());

        EditText username = field("Username");
        root.addView(username, fullWrap());
        EditText password = field("Password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams passwordParams = fullWrap();
        passwordParams.setMargins(0, 12, 0, 0);
        root.addView(password, passwordParams);

        Button action = button(configured ? "Sign in" : "Create admin account");
        LinearLayout.LayoutParams actionParams = wrap();
        actionParams.gravity = Gravity.CENTER_HORIZONTAL;
        actionParams.setMargins(0, 20, 0, 0);
        root.addView(action, actionParams);
        action.setOnClickListener(view -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString();
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            String path = configured ? "/api/auth/login" : "/api/auth/setup";
            String body = "{\"username\":\"" + jsonEscape(user) + "\",\"password\":\"" + jsonEscape(pass) + "\"}";
            showLoading(configured ? "Signing in…" : "Creating account…");
            request("POST", path, body, result -> {
                try {
                    JSONObject response = new JSONObject(result);
                    if (response.optBoolean("requires_totp", false)) {
                        showTotpScreen(user, pass);
                    } else if (response.optBoolean("ok", false)) {
                        loadNativeApp();
                    } else {
                        showError(response.optString("message", "Authentication failed"), null);
                    }
                } catch (Exception error) {
                    showError("Authentication failed", error);
                }
            });
        });
        setContentView(root);
    }

    private void showTotpScreen(String username, String password) {
        LinearLayout root = page(Color.rgb(40, 44, 52));
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 24, 32, 24);
        TextView title = text("Two-factor code", 24, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWrap());
        EditText code = field("6-digit code");
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(code, fullWrap());
        Button action = button("Verify");
        LinearLayout.LayoutParams actionParams = wrap();
        actionParams.gravity = Gravity.CENTER_HORIZONTAL;
        actionParams.setMargins(0, 20, 0, 0);
        root.addView(action, actionParams);
        action.setOnClickListener(view -> {
            String body = "{\"username\":\"" + jsonEscape(username) + "\",\"password\":\"" + jsonEscape(password) + "\",\"totp_code\":\"" + jsonEscape(code.getText().toString().trim()) + "\"}";
            showLoading("Verifying…");
            request("POST", "/api/auth/login", body, result -> {
                try {
                    JSONObject response = new JSONObject(result);
                    if (response.optBoolean("ok", false)) loadNativeApp();
                    else showError("Invalid two-factor code", null);
                } catch (Exception error) { showError("Verification failed", error); }
            });
        });
        setContentView(root);
    }

    private void loadModelsAndSessions() {
        request("GET", "/api/models?background=false", "", modelsResult -> request("GET", "/api/sessions", "", sessionsResult -> {
            try {
                JSONArray sessions = new JSONArray(sessionsResult);
                if (sessions.length() > 0) sessionId = sessions.getJSONObject(0).optString("id", "");
                JSONObject models = new JSONObject(modelsResult);
                if (sessionId.isEmpty() && models.optJSONArray("items") != null && models.optJSONArray("items").length() > 0) {
                    JSONObject model = models.optJSONArray("items").getJSONObject(0);
                    createSession(model.optString("base_url", ""), model.optString("model", model.optString("id", "")));
                } else {
                    showChatScreen();
                }
            } catch (Exception error) { showError("Could not load chats", error); }
        }));
    }

    private void createSession(String endpointUrl, String model) {
        String form = "name=" + encoded("New Chat") + "&endpoint_url=" + encoded(endpointUrl) + "&model=" + encoded(model) + "&skip_validation=true";
        requestForm("POST", "/api/session", form, result -> {
            try { sessionId = new JSONObject(result).optString("id", ""); } catch (Exception ignored) { }
            showChatScreen();
        });
    }

    private void showChatScreen() {
        LinearLayout root = page(Color.rgb(40, 44, 52));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(18, 12, 18, 12);
        TextView title = text("Odysseus", 21, Color.WHITE);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        status = text("Connected", 13, Color.rgb(170, 180, 190));
        header.addView(status, wrap());
        root.addView(header, fullWrap());

        transcript = new ScrollView(this);
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(16, 8, 16, 16);
        transcript.addView(messageList, fullWrap());
        root.addView(transcript, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setPadding(10, 8, 10, 10);
        composer.setGravity(Gravity.BOTTOM);
        messageInput = new EditText(this);
        messageInput.setHint("Message Odysseus…");
        messageInput.setTextColor(Color.WHITE);
        messageInput.setHintTextColor(Color.GRAY);
        messageInput.setGravity(Gravity.TOP);
        messageInput.setMinLines(1);
        messageInput.setMaxLines(5);
        composer.addView(messageInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sendButton = button("Send");
        composer.addView(sendButton, wrap());
        sendButton.setOnClickListener(view -> sendMessage());
        root.addView(composer, fullWrap());
        setContentView(root);
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty() || sessionId == null || sessionId.isEmpty()) return;
        messageInput.setText("");
        addMessage("You", message, Color.rgb(145, 190, 255));
        sendButton.setEnabled(false);
        status.setText("Thinking…");
        String body = "message=" + encoded(message) + "&session=" + encoded(sessionId) + "&mode=chat";
        requestForm("POST", "/api/chat_stream", body, result -> {
            main.post(() -> {
                sendButton.setEnabled(true);
                status.setText("Connected");
                String answer = parseStream(result);
                addMessage("Odysseus", answer.isEmpty() ? result : answer, Color.WHITE);
            });
        });
    }

    private String parseStream(String body) {
        StringBuilder answer = new StringBuilder();
        for (String line : body.split("\\n")) {
            if (!line.startsWith("data:")) continue;
            try {
                JSONObject event = new JSONObject(line.substring(5).trim());
                String type = event.optString("type", "");
                if (type.equals("token") || type.equals("text") || type.equals("content")) answer.append(event.optString("content", event.optString("text", "")));
            } catch (Exception ignored) { }
        }
        return answer.toString().trim();
    }

    private void addMessage(String speaker, String content, int color) {
        TextView view = text(speaker + "\n" + content, 16, color);
        view.setPadding(14, 12, 14, 12);
        view.setBackgroundColor(Color.rgb(50, 55, 65));
        LinearLayout.LayoutParams params = fullWrap();
        params.setMargins(0, 6, 0, 6);
        messageList.addView(view, params);
        transcript.post(() -> transcript.fullScroll(View.FOCUS_DOWN));
    }

    private void showLoading(String message) {
        LinearLayout root = page(Color.rgb(40, 44, 52));
        root.setGravity(Gravity.CENTER);
        ProgressBar progress = new ProgressBar(this);
        root.addView(progress, wrap());
        TextView label = text(message, 16, Color.WHITE);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = fullWrap();
        params.setMargins(0, 16, 0, 0);
        root.addView(label, params);
        setContentView(root);
    }

    private void showError(String message, Exception error) {
        main.post(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            showConnectionScreen();
        });
    }

    private void request(String method, String path, String body, OnResponse callback) {
        network.execute(() -> {
            try {
                HttpURLConnection connection = open(method, path);
                if (!body.isEmpty()) {
                    connection.setRequestProperty("Content-Type", "application/json");
                    write(connection, body);
                }
                int code = connection.getResponseCode();
                String response = read(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
                main.post(() -> callback.onResult(response));
            } catch (Exception error) {
                if (retryWithoutTls(error, method, path, body, callback, false)) return;
                showError(connectionError(error), error);
            }
        });
    }

    private void requestForm(String method, String path, String body, OnResponse callback) {
        network.execute(() -> {
            try {
                HttpURLConnection connection = open(method, path);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                write(connection, body);
                int code = connection.getResponseCode();
                String response = read(code >= 400 ? connection.getErrorStream() : connection.getInputStream());
                main.post(() -> callback.onResult(response));
            } catch (Exception error) {
                if (retryWithoutTls(error, method, path, body, callback, true)) return;
                showError(connectionError(error), error);
            }
        });
    }

    private boolean retryWithoutTls(Exception error, String method, String path, String body, OnResponse callback, boolean form) {
        String failedEndpoint = endpoint;
        if (!failedEndpoint.startsWith("https://")) return false;
        String fallback = "http://" + failedEndpoint.substring(8);
        if (!(error instanceof SSLException)) return false;
        endpoint = fallback;
        getPreferences(MODE_PRIVATE).edit().putString("endpoint", endpoint).apply();
        if (form) requestForm(method, path, body, callback);
        else request(method, path, body, callback);
        return true;
    }

    private String connectionError(Exception error) {
        String detail = error.getMessage();
        if (detail == null || detail.trim().isEmpty()) detail = error.getClass().getSimpleName();
        return "Server connection failed: " + detail + "\nTry http://" + endpoint.replaceFirst("^https://", "");
    }

    private HttpURLConnection open(String method, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(300000);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        if (method.equals("POST")) connection.setDoOutput(true);
        return connection;
    }

    private void write(HttpURLConnection connection, String body) throws Exception {
        try (OutputStream output = connection.getOutputStream()) { output.write(body.getBytes(StandardCharsets.UTF_8)); }
    }

    private String read(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) output.append(line).append('\n');
        }
        return output.toString();
    }

    private LinearLayout page(int background) { LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(background); return root; }
    private TextView text(String value, float size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view; }
    private EditText field(String hint) { EditText view = new EditText(this); view.setHint(hint); view.setSingleLine(true); view.setTextColor(Color.WHITE); view.setHintTextColor(Color.GRAY); return view; }
    private Button button(String label) { Button view = new Button(this); view.setText(label); return view; }
    private LinearLayout.LayoutParams fullWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private String normalizeEndpoint(String value) { String result = value.trim(); if (!result.startsWith("http://") && !result.startsWith("https://") && !result.isEmpty()) result = "http://" + result; while (result.endsWith("/")) result = result.substring(0, result.length() - 1); return result; }
    private String encoded(String value) { try { return URLEncoder.encode(value, "UTF-8"); } catch (Exception error) { return ""; } }
    private String jsonEscape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
    @Override protected void onDestroy() { network.shutdownNow(); super.onDestroy(); }
    @Override public void onBackPressed() { showConnectionScreen(); }
    private interface OnResponse { void onResult(String result); }
}
