package com.pisco.deydempro3;

import android.util.Log;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class VolleyMultipartRequest extends Request<String> {

    private final Response.Listener<String> mListener;
    private final Map<String, DataPart> mByteData;
    private final Map<String, String> mParams;
    private final String BOUNDARY = "Boundary-" + System.currentTimeMillis();
    private final String LINE_FEED = "\r\n";

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<String> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mByteData = new HashMap<>();
        mParams = new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        return headers;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }

    public Map<String, DataPart> getByteData() {
        return mByteData;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + BOUNDARY;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Ajouter les paramètres textuels
            for (Map.Entry<String, String> entry : mParams.entrySet()) {
                buildTextPart(dos, entry.getKey(), entry.getValue());
            }

            // Ajouter les fichiers
            for (Map.Entry<String, DataPart> entry : mByteData.entrySet()) {
                DataPart dataPart = entry.getValue();
                buildDataPart(dos, dataPart, entry.getKey());
            }

            // Fin de la requête
            dos.writeBytes("--" + BOUNDARY + "--" + LINE_FEED);

            return bos.toByteArray();

        } catch (IOException e) {
            VolleyLog.e("Error building multipart request: " + e.toString());
            return null;
        }
    }

    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
        dataOutputStream.writeBytes("--" + BOUNDARY + LINE_FEED);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + LINE_FEED);
        dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + LINE_FEED);
        dataOutputStream.writeBytes(LINE_FEED);
        dataOutputStream.writeBytes(parameterValue + LINE_FEED);
    }

    private void buildDataPart(DataOutputStream dataOutputStream, DataPart dataFile, String inputName) throws IOException {
        dataOutputStream.writeBytes("--" + BOUNDARY + LINE_FEED);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + inputName + "\"; filename=\"" + dataFile.getFileName() + "\"" + LINE_FEED);
        dataOutputStream.writeBytes("Content-Type: " + dataFile.getType() + LINE_FEED);
        dataOutputStream.writeBytes("Content-Transfer-Encoding: binary" + LINE_FEED);
        dataOutputStream.writeBytes(LINE_FEED);

        dataOutputStream.write(dataFile.getContent());

        dataOutputStream.writeBytes(LINE_FEED);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            return Response.success(jsonString, null);
        } catch (UnsupportedEncodingException e) {
            return Response.success(new String(response.data), null);
        }
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    public static class DataPart {
        private String fileName;
        private byte[] content;
        private String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }
    }
}