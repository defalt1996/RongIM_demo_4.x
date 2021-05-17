package io.rong.imkit.utils;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import io.rong.common.utils.SSLUtils;
import io.rong.imageloader.core.assist.ContentLengthInputStream;
import io.rong.imageloader.core.download.BaseImageDownloader;
import io.rong.imageloader.utils.IoUtils;
import io.rong.imlib.common.NetUtils;

public class RongAuthImageDownloader extends BaseImageDownloader {

    private static final String TAG = RongAuthImageDownloader.class.getSimpleName();

    public RongAuthImageDownloader(Context context) {
        super(context);
    }

    public RongAuthImageDownloader(Context context, int connectTimeout, int readTimeout) {
        super(context, connectTimeout, readTimeout);
    }

    @Override
    protected InputStream getStreamFromNetwork(String imageUri, Object extra) throws IOException {
        HttpURLConnection conn = null;
        InputStream imageStream;
        try {
            conn = NetUtils.createURLConnection(imageUri);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);

            if (conn instanceof HttpsURLConnection) {
                SSLContext sslContext = SSLUtils.getSSLContext();
                if(sslContext != null){
                    ((HttpsURLConnection) conn).setSSLSocketFactory(sslContext.getSocketFactory());
                }
                HostnameVerifier hostVerifier = SSLUtils.getHostVerifier();
                if (hostVerifier != null){
                    ((HttpsURLConnection) conn).setHostnameVerifier((hostVerifier));
                }
            }
            conn.connect();
            if (conn.getResponseCode() >= 300 && conn.getResponseCode() < 400) {
                String redirectUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) new URL(redirectUrl).openConnection();
                conn.setConnectTimeout(connectTimeout);
                conn.setReadTimeout(readTimeout);

                if (conn instanceof HttpsURLConnection) {
                    SSLContext sslContext = SSLUtils.getSSLContext();
                    if(sslContext != null){
                        ((HttpsURLConnection) conn).setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                    HostnameVerifier hostVerifier = SSLUtils.getHostVerifier();
                    if (hostVerifier != null){
                        ((HttpsURLConnection) conn).setHostnameVerifier((hostVerifier));
                    }
                }
                conn.connect();
            }
            imageStream = conn.getInputStream();
        } catch (IOException e) {
            // handle url like http://avatar.zbjimg.com/014/02/64/200x200_avatar_10.jpg!big
            // return error image when 404
            if (conn != null && conn.getContentLength() > 0 && conn.getContentType().contains("image/")) {
                imageStream = conn.getErrorStream();
            } else {
                // Read all data to allow reuse connection (http://bit.ly/1ad35PY)
                if (conn != null) {
                    IoUtils.readAndCloseStream(conn.getErrorStream());
                }
                throw e;
            }
        }
        if (!shouldBeProcessed(conn)) {
            IoUtils.closeSilently(imageStream);
            throw new IOException("Image request failed with response code " + conn.getResponseCode());
        }

        return new ContentLengthInputStream(new BufferedInputStream(imageStream, BUFFER_SIZE), conn.getContentLength());
    }
}
