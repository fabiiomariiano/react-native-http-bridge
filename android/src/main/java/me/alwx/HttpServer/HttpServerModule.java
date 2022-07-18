package me.alwx.HttpServer;

import android.content.Context;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactMethod;

import java.io.File;
import java.io.IOException;

import android.util.Log;

public class HttpServerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    ReactApplicationContext reactContext;

    private File www_root = null;
    private static final String MODULE_NAME = "HttpServer";

    private String localPath;
    private static int port;
    private static Server server = null;

    public HttpServerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void start(int port, String serviceName, String root) {
        Log.d(MODULE_NAME, "Initializing server...");
        this.port = port;

        if (root != null) {
            if ((root.startsWith("/") || root.startsWith("file:///"))) {
                www_root = new File(root);
                localPath = www_root.getAbsolutePath();
            } else {
                www_root = new File(this.reactContext.getFilesDir(), root);
                localPath = www_root.getAbsolutePath();
            }
        }

        startServer(www_root);
    }

    @ReactMethod
    public void stop() {
        Log.d(MODULE_NAME, "Stopping server...");

        stopServer();
    }

    @ReactMethod
    public void respond(String requestId, int code, String type, String body) {
        if (server != null) {
            server.respond(requestId, code, type, body);
        }
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        stopServer();
    }

    private void startServer(File www_root) {
        if (this.port == 0) {
            return;
        }

        if (server == null) {
            server = new Server(reactContext, port, www_root);
        }
        try {
            server.start();
        } catch (IOException e) {
            Log.e(MODULE_NAME, e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
            port = 0;
        }
    }
}
