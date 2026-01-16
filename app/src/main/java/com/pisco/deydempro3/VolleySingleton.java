package com.pisco.deydempro3;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class VolleySingleton extends Request<String> {

    private static VolleySingleton instance;
    private static RequestQueue requestQueue;
    private static Context ctx;

    private final Response.Listener<String> listener;
    private final String boundary = "apiclient-" + System.currentTimeMillis();

    /* ================= CONSTRUCTOR ================= */

    public VolleySingleton(int method, String url,
                           Response.Listener<String> listener,
                           Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
    }

    /* ================= SINGLETON ================= */

    public static synchronized RequestQueue getInstance(Context context) {
        if (requestQueue == null) {
            ctx = context.getApplicationContext();
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }

    /* ================= PARAMS ================= */

    protected Map<String, String> getParams() throws AuthFailureError {
        return null;
    }

    public Map<String, DataPart> getByteData() {
        return null;
    }

    /* ================= HEADERS ================= */

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "multipart/form-data;boundary=" + boundary);
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + boundary;
    }

    /* ================= BODY ================= */

    @Override
    public byte[] getBody() throws AuthFailureError {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // TEXT PARAMS
            Map<String, String> params = getParams();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    writeText(dos, entry.getKey(), entry.getValue());
                }
            }

            // FILE PARAMS
            Map<String, DataPart> data = getByteData();
            if (data != null) {
                for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                    writeFile(dos, entry.getKey(), entry.getValue());
                }
            }

            dos.writeBytes("--" + boundary + "--\r\n");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    private void writeText(DataOutputStream dos, String key, String value)
            throws Exception {

        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n");
        dos.writeBytes(value + "\r\n");
    }

    private void writeFile(DataOutputStream dos, String key, DataPart data)
            throws Exception {

        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key +
                "\"; filename=\"" + data.fileName + "\"\r\n");
        dos.writeBytes("Content-Type: " + data.type + "\r\n\r\n");
        dos.write(data.content);
        dos.writeBytes("\r\n");
    }

    /* ================= RESPONSE ================= */

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String res = new String(response.data);
        return Response.success(res, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(String response) {
        listener.onResponse(response);
    }

    /* ================= DATA PART ================= */

    public static class DataPart {
        public String fileName;
        public byte[] content;
        public String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }
    }
}
