package com.odysseus.mobile;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String endpoint = getPreferences(MODE_PRIVATE).getString("endpoint", "http://10.0.2.2:7000");
        showEndpointScreen(endpoint);
    }

    private void showEndpointScreen(String endpoint) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 24, 32, 24);
        root.setBackgroundColor(Color.rgb(40, 44, 52));

        TextView title = new TextView(this);
        title.setText("Odysseus Mobile");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("Enter the address of your Odysseus server");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(15);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.setMargins(0, 12, 0, 16);
        root.addView(hint, hintParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(endpoint);
        input.setHint("http://192.168.1.20:7000");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        root.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button connect = new Button(this);
        connect.setText("Connect");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        buttonParams.setMargins(0, 20, 0, 0);
        root.addView(connect, buttonParams);
        connect.setOnClickListener(view -> {
            String value = input.getText().toString().trim();
            if (!value.startsWith("http://") && !value.startsWith("https://")) value = "http://" + value;
            getPreferences(MODE_PRIVATE).edit().putString("endpoint", value).apply();
            showWebView(value);
        });
        setContentView(root);
    }

    private void showWebView(String endpoint) {
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        webView.loadUrl(endpoint);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
