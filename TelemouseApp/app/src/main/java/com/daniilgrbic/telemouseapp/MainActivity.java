package com.daniilgrbic.telemouseapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import tech.gusavila92.websocketclient.WebSocketClient;

@SuppressLint({"InflateParams", "ClickableViewAccessibility"})
public class MainActivity extends AppCompatActivity {
    WebSocketClient webSocketClient;
    View trackPad, scrollbar;
    static ImageView rightButton, leftButton;
    Activity context;
    SharedPreferences prefs;
    String IP, IPBuilder, LAN, rememberedCode, firstTime;
    Controller controller;
    boolean CONNECTED;
    boolean anyDialogShown;
    boolean utilBool;
    int utilVar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideNavbar();

        CONNECTED = false;
        IP = "ws://xxx.xxx.xxx.xxx:8080/";
        IPBuilder = "ws://xxx.xxx.xxx.";
        context = this;

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ownIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress()); // used deprecated method for android 6 support
        String[] ipParts = ownIp.split("\\.", 0);
        if(ownIp.equals("0.0.0.0")) {
            /* the phone is a hotspot; and is visible as 192.168.43.1
             (default for all Android devices, unless explicitly changed by the phone manufacturer, in which case the app won't work) */
            IPBuilder = "ws://192.168.43.";
            LAN = "192.168.43.";
        }
        else {
            IPBuilder = "ws://"+ipParts[0]+"."+ipParts[1]+"."+ipParts[2]+".";
            LAN = ipParts[0]+"."+ipParts[1]+"."+ipParts[2]+".";
        }

        /* The app tries to pull up the previously used code, and tries to connect to the server
         Otherwise the app prompts the user with the code input dialog*/
        prefs = this.getSharedPreferences("com.daniilgrbic.telemouse", Context.MODE_PRIVATE);
        firstTime = prefs.getString("first",null);
        rememberedCode = prefs.getString("code",null);

        if(firstTime == null) {
            showFirstTime();

        }
        else if(rememberedCode != null) {
            IP = IPBuilder + rememberedCode + ":8080/";
            createWebSocketClient();
            Toast.makeText(context, "Connecting to " + LAN + rememberedCode + "...", Toast.LENGTH_LONG).show();
        }
        else {
            showInputDialog();
        }

        anyDialogShown = false;
        controller = new Controller();

        scrollbar = findViewById(R.id.scrollbar);
        scrollbar.setOnTouchListener((view, motionEvent) -> {
            send(controller.scrolled(motionEvent));
            return true;
        });

        trackPad = findViewById(R.id.trackpad);
        trackPad.setOnTouchListener((view, motionEvent) -> {
            send(controller.movedCursor(motionEvent));
            return true;
        });

        leftButton = findViewById(R.id.leftButton);
        leftButton.setOnTouchListener((view, motionEvent) -> {
            send(controller.leftClicked(motionEvent));
            return true;
        });

        rightButton = findViewById(R.id.rightButton);
        rightButton.setOnTouchListener((view, motionEvent) -> {
            send(controller.rightClicked(motionEvent));
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater  = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.reload) {
            CONNECTED = false;
            webSocketClient.close();
            if(!anyDialogShown) showInputDialog();
        }
        else{
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/daniilgrbic/Telemouse"));
            startActivity(browserIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onDestroy() {
        super.onDestroy();
        if(CONNECTED) webSocketClient.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideNavbar();
    }

    private void hideNavbar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void send(String s) {
        if(s != null) webSocketClient.send(s);
    }

    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI(IP);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                Log.d("WEB-SOCKET", "Session is starting.");
                CONNECTED = true;
            }
            @Override
            public void onTextReceived(String MESSAGE) {
                Log.i("TEXT_RECEIVED", MESSAGE);
            }
            @Override
            public void onBinaryReceived(byte[] data) { }
            @Override
            public void onPingReceived(byte[] data) { }
            @Override
            public void onPongReceived(byte[] data) { }
            @Override
            public void onException(Exception e) {
                System.out.println(e.getMessage());
                Log.e("EXCP",e.getMessage());
                if(e.getMessage().equals("Connection reset")) {
                    context.runOnUiThread(() -> showConnectionLostDialog());
                }
            }
            @Override
            public void onCloseReceived() {
                Log.i("WEB-SOCKET", "Session closed.");
                CONNECTED = false;
            }
        };
        webSocketClient.setConnectTimeout(3000);
        webSocketClient.setReadTimeout(0);
        webSocketClient.enableAutomaticReconnection(100);
        webSocketClient.connect();

        // the app waits for approximately 3600 milliseconds before deciding that the server is impossible to reach
        utilVar = 0;
        utilBool = false;
        Handler handler = new Handler();
        Runnable connectRunnable = () -> {
            utilVar++;
            Log.i("handler","called");
            if(utilVar == 6 && !CONNECTED) {
                try {
                    if(!anyDialogShown) {
                        CONNECTED = false;
                        webSocketClient.close();
                        showFailedDialog();
                    }
                }
                catch(Exception e) {
                    Log.i("EXCEPTION","Cannot open failure dialog!");
                }
            }
            else if(CONNECTED && !utilBool) {
                utilBool = true;
                try {
                    if(!anyDialogShown) showSuccessDialog();
                }
                catch(Exception e) {
                    Log.i("EXCEPTION","Cannot open success dialog!");
                }
            }
        };
        for(int i =0; i<7 && !utilBool; i++) {
            try{
                handler.postDelayed(connectRunnable, 100 + 500 * i);
            }
            catch(Exception e) {
                Log.i("HANDLER_ERROR","Handler cannot be called");
            }
        }
    }

    // this dialog asks for a server provided access code (last number of the server's IPv4)
    // user has two options: proceed or quit the app
    protected void showInputDialog() {
        anyDialogShown = true;
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);
        final EditText editText = promptView.findViewById(R.id.edittext);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.OK, (dialog, id) -> {
                    hideNavbar();
                    if(editText.getText().toString().equals("") || Integer.parseInt(editText.getText().toString())<1 || Integer.parseInt(editText.getText().toString())>255) {
                        // the code can range from 1 -> 255
                        Toast.makeText(context, R.string.enterValidCode, Toast.LENGTH_SHORT).show();
                        showInputDialog();
                    }
                    else {
                        // save the new access code before connecting
                        rememberedCode = editText.getText().toString();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("code",rememberedCode);
                        editor.apply();
                        IP = IPBuilder + rememberedCode + ":8080/";
                        createWebSocketClient();
                        Toast.makeText(context, "Connecting to " + LAN + rememberedCode + "...", Toast.LENGTH_LONG).show();
                        anyDialogShown = false;
                    }
                })
                .setNegativeButton(R.string.quit,
                        (dialog, id) -> {
                            anyDialogShown = false;
                            dialog.cancel();
                            System.exit(0);
                        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        Objects.requireNonNull(alert.getWindow()).setBackgroundDrawable(new ColorDrawable(0xFF424242));
    }

    // this dialog is displayed once the connection attempt has resulted in failure
    // it let's the user try to connect again or to leave the app
    protected void showFailedDialog() {
        anyDialogShown = true;
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.failed_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.tryAgain, (dialog, id) -> {
                    webSocketClient.close();
                    CONNECTED = false;
                    showInputDialog();
                })
                .setNegativeButton(R.string.quit,
                        (dialog, id) -> {
                            anyDialogShown = false;
                            dialog.cancel();
                            System.exit(0);
                        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        Objects.requireNonNull(alert.getWindow()).setBackgroundDrawable(new ColorDrawable(0xFF424242));
    }

    // this dialog is displayed once the connection has been established successfully
    // the user can then proceed to using the app
    protected  void showSuccessDialog() {
        anyDialogShown = true;
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.connected_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
                .setNegativeButton(R.string.OK,
                        (dialog, id) -> {
                            anyDialogShown = false;
                            hideNavbar();
                            dialog.cancel();
                        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        Objects.requireNonNull(alert.getWindow()).setBackgroundDrawable(new ColorDrawable(0xFF424242));
    }

    // this is called when the server dies while the app is running
    // user can either reconnect or quit
    protected  void showConnectionLostDialog() {
        anyDialogShown = true;
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.connection_lost_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.tryAgain, (dialog, id) -> {
                    webSocketClient.close();
                    CONNECTED = false;
                    showInputDialog();
                })
                .setNegativeButton(R.string.quit,
                        (dialog, id) -> {
                            anyDialogShown = false;
                            dialog.cancel();
                            System.exit(0);
                        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        Objects.requireNonNull(alert.getWindow()).setBackgroundDrawable(new ColorDrawable(0xFF424242));
    }

    // this is called when the app is started for the first time to provide the user with instructions
    protected  void showFirstTime() {
        anyDialogShown = true;
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.first_time, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(R.string.instr, (dialog, id) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/daniilgrbic/Telemouse"));
                    startActivity(browserIntent);
                    showInputDialog();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("first","nope");
                    editor.apply();
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        Objects.requireNonNull(alert.getWindow()).setBackgroundDrawable(new ColorDrawable(0xFF424242));
    }
}
