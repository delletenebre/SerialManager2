package kg.delletenebre.serialmanager2.communication.impl;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import kg.delletenebre.serialmanager2.App;
import kg.delletenebre.serialmanager2.communication.BaseCommunicatorImpl;
import kg.delletenebre.serialmanager2.communication.CommunicatorType;
import kg.delletenebre.serialmanager2.utils.Utils;

public class WebServerCommunicator extends BaseCommunicatorImpl {
    private final AsyncHttpServer httpServer;
    private final List<WebSocket> sockets;

    public WebServerCommunicator(Context context) {
        super(context, CommunicatorType.WEB);
        httpServer = new AsyncHttpServer();
        sockets = new ArrayList<>();
    }

    @Override
    public void open() {
        int port = App.getInstance().getIntPreference("web_server_port",
                App.getInstance().getStringPreference("web_server_port"));

        httpServer.get("/", (request, response) -> {
            String webSocketInfo = String.format(
                    App.getInstance().getStringPreference("web_socket_info"),
                    Utils.getIpAddress(),
                    port);

            response.send("<!DOCTYPE html><head><title>"
                    + App.getInstance().getStringPreference("app_name")
                    + "</title><meta charset=\"utf-8\" /></head><body><h1>"
                    + App.getInstance().getStringPreference("app_name")
                    + "</h1><i>version: <b>"
                    + App.getInstance().getVersion()
                    + "</b></i><br><br>"
                    + webSocketInfo
                    + "<br><br><a href=\"/test-websocket\">WebSocket test</a></body></html>");
        });

        httpServer.get("/test-websocket", (request, response) -> {
            String html = "<h1>404 Not found</h1>";
            InputStream input;
            try {
                input = context.getAssets().open("websoket_test.html");
                int size = input.available();
                byte[] buffer = new byte[size];
                //noinspection ResultOfMethodCallIgnored
                input.read(buffer);
                input.close();

                html = new String(buffer);
                html = html.replace("{{address}}", Utils.getIpAddress() + ":" + port + "/ws");
            } catch (IOException e) {
                e.printStackTrace();
            }

            response.send(html);
        });

        httpServer.listen(port);

        httpServer.websocket("/ws", (webSocket, request) -> {
            App.log("New WebSocket client connected");
            webSocket.setClosedCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception e) {
                    try {
                        if (e != null) {
                            e.printStackTrace();
                        }
                    } finally {
                        localBroadcastManager.sendBroadcast(
                                new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                                        .putExtra("type", "web")
                                        .putExtra("name", webSocket.toString()));
                        sockets.remove(webSocket);
//                        updateNotificationText();
                    }
                }
            });
            webSocket.setStringCallback(message -> localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_COMMAND_RECEIVED)
                            .putExtra("from", "web")
                            .putExtra("command", message)));

            sockets.add(webSocket);

            localBroadcastManager.sendBroadcast(
                    new Intent(App.LOCAL_ACTION_CONNECTION_ESTABLISHED)
                            .putExtra("type", "web")
                            .putExtra("name", webSocket.toString()));

            if (isConnectionStateMessageUse()) {
                webSocket.send(App.ACTION_CONNECTION_ESTABLISHED);
            }
//            updateNotificationText();
        });


    }

    @Override
    public void close() {
        if (isConnectionStateMessageUse()) {
            write(App.ACTION_CONNECTION_LOST);
        }

        if (sockets != null) {
            for (WebSocket socket : sockets) {
                socket.close();
            }
//            sockets = null;
        }

        if (httpServer != null) {
            httpServer.stop();
//            httpServer = null;
        }
        localBroadcastManager.sendBroadcast(
                new Intent(App.LOCAL_ACTION_CONNECTION_CLOSED)
                        .putExtra("type", "web")
                        .putExtra("name", "web"));
    }

    @Override
    public void write(@NonNull String data) {
        if (sockets != null) {
            for (WebSocket socket : sockets) {
                socket.send(data);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return !sockets.isEmpty();
    }

    @Override
    public Integer getConnectionsCount() {
        return sockets.size();
    }
}
