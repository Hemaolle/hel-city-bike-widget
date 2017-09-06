package com.leppaaho.oskari.citybikewidget;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Modified from the Android documentation example:
 * https://developer.android.com/training/volley/request-custom.html
 * took inspiration from Volley JsonRequest and JsonObjectRequest to add support for the POST method:
 * https://github.com/google/volley/blob/master/src/main/java/com/android/volley/toolbox/JsonRequest.java,
 * https://github.com/google/volley/blob/master/src/main/java/com/android/volley/toolbox/JsonObjectRequest.java
 */
public class GsonRequest<T> extends Request<T> {
    private final Gson gson = new Gson();
    private final Class<T> clazz;
    private final Response.Listener<T> listener;
    private final String mRequestBody;

    /** Default charset for JSON request. */
    protected static final String PROTOCOL_CHARSET = "utf-8";

    /** Content type for request. */
    private static final String PROTOCOL_CONTENT_TYPE =
            String.format("application/json; charset=%s", PROTOCOL_CHARSET);

    public GsonRequest(int method, String url, JSONObject jsonRequest, Class<T> clazz,
                       Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.clazz = clazz;
        this.mRequestBody = jsonRequest.toString();
        this.listener = listener;
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() {
        try {
            return mRequestBody == null ? null : mRequestBody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s",
                    mRequestBody, PROTOCOL_CHARSET);
            return null;
        }
    }

    @Override
    protected void deliverResponse(T response) {
        listener.onResponse(response);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(
                    response.data,
                    PROTOCOL_CHARSET);
            return Response.success(
                    gson.fromJson(json, clazz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }
}