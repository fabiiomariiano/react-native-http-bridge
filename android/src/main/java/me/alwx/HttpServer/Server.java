package me.alwx.HttpServer;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Random;

import androidx.annotation.Nullable;
import android.util.Log;

public class Server extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private static final String SERVER_EVENT_ID = "httpServerResponseReceived";

    private ReactContext reactContext;
    private Map<String, Response> responses;
    private File root;

    public Server(ReactContext context, int port, File wwwroot) {
        super(port);
        reactContext = context;
        responses = new HashMap<>();
        this.root = wwwroot;

        Log.d(TAG, "Server started");
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Request received!");

        Map<String, String> headers = session.getHeaders();
        if (this.root != null) {
            String uri = session.getUri();
            File f = new File(this.root, uri);
            if (f.exists()) {
                return serveFile(uri, headers, f);
            }
        }
        return serveNoFile(session);
    }

    private Response serveNoFile(IHTTPSession session) {
        Random rand = new Random();
        String requestId = String.format("%d:%d", System.currentTimeMillis(), rand.nextInt(1000000));

        WritableMap request;
        try {
            request = fillRequestMap(session, requestId);
        } catch (Exception e) {
            return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage()
            );
        }

        this.sendEvent(reactContext, SERVER_EVENT_ID, request);

        while (responses.get(requestId) == null) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                Log.d(TAG, "Exception while waiting: " + e);
            }
        }
        Response response = responses.get(requestId);
        responses.remove(requestId);
        return response;
    }

    public void respond(String requestId, int code, String type, String body) {
        responses.put(requestId, newFixedLengthResponse(Status.lookup(code), type, body));
    }

    private WritableMap fillRequestMap(IHTTPSession session, String requestId) throws Exception {
        Method method = session.getMethod();
        WritableMap request = Arguments.createMap();
        request.putString("url", session.getUri());
        request.putString("params", session.getQueryParameterString());
        request.putString("type", method.name());
        request.putString("requestId", requestId);
        
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        if (files.size() > 0) {
          request.putString("postData", files.get("postData"));
        }

        return request;
    }

    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    private Response serveFile(String uri, Map<String, String> header, File file) {
        Response res;
        String mime = getMimeTypeForFile(uri);
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() +
                    file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = newChunkedResponse(Response.Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                            endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), file.length());
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = newFixedLengthResponse(Status.FORBIDDEN, "", "Forbidden: Reading file failed");
        }

        return (res == null) ? newFixedLengthResponse(Status.NOT_FOUND, "", "Error 404: File not found") : res;
    }
}
