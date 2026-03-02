package me.bytebeats.mns.network;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.net.URLDecoder;
import java.util.Map;

/**
 * @Author bytebeats
 * @Email <bvzgong@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2021/9/19 15:06
 * @Version 1.0
 * @Description HttpClient Pool
 */

public class HttpClientPool {

    private HttpClientPool() {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(100);
    }

    private static volatile HttpClientPool INSTANCE;
    private PoolingHttpClientConnectionManager connectionManager;

    public static synchronized HttpClientPool getInstance() {
        if (INSTANCE == null) {
            synchronized (HttpClientPool.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HttpClientPool();
                }
            }
        }
        return INSTANCE;
    }

    private CloseableHttpClient getHttpClient() {
        return getHttpClient(2000, 2000);
    }

    private CloseableHttpClient getHttpClient(int connectionTimeout, int socketTimeOut) {
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(connectionTimeout).setSocketTimeout(socketTimeOut).build();
        return HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();
    }

    public String get(String url) throws Exception {
        return get(url, null);
    }

    public String get(String url, Map<String, String> headers) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        if (headers != null && !headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpGet.addHeader(key, headers.get(key));
            }
        }
        return getResponseContent(url, httpGet);
    }

    public String post(String url, Map<String, String> headers) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        if (headers != null && !headers.isEmpty()) {
            for (String key : headers.keySet()) {
                httpPost.addHeader(key, headers.get(key));
            }
        }
        return getResponseContent(url, httpPost);
    }

    public String postJson(String url, String json) throws Exception {
        HttpPost post = new HttpPost(url);
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        return getResponseContent(url, post);
    }

    private String getResponseContent(String url, HttpRequestBase request) throws Exception {
        HttpResponse response = null;
        try {
            response = this.getHttpClient().execute(request);
            // Read raw bytes first
            byte[] rawData = EntityUtils.toByteArray(response.getEntity());
            // Try to detect the correct charset
            String charset = detectCharset(rawData, url);
            return new String(rawData, charset);
        } catch (Exception e) {
            throw new Exception("got an error from HTTP for url : " + URLDecoder.decode(url, "UTF-8"), e);
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
            }
            request.releaseConnection();
        }
    }

    /**
     * Detect charset from raw bytes by checking for BOM or valid UTF-8 sequences
     * Also considers the URL to determine the likely charset for different APIs
     */
    private String detectCharset(byte[] rawData, String url) {
        if (rawData == null || rawData.length < 2) {
            return "UTF-8";
        }
        
        // Check for UTF-8 BOM
        if (rawData.length >= 3 && rawData[0] == (byte)0xEF && rawData[1] == (byte)0xBB && rawData[2] == (byte)0xBF) {
            return "UTF-8";
        }
        
        // Check for UTF-16 BE BOM
        if (rawData.length >= 2 && rawData[0] == (byte)0xFE && rawData[1] == (byte)0xFF) {
            return "UTF-16BE";
        }
        
        // Check for UTF-16 LE BOM
        if (rawData.length >= 2 && rawData[0] == (byte)0xFF && rawData[1] == (byte)0xFE) {
            return "UTF-16LE";
        }
        
        // Check for UTF-32 BE BOM
        if (rawData.length >= 4 && rawData[0] == 0x00 && rawData[1] == 0x00
                && rawData[2] == (byte)0xFE && rawData[3] == (byte)0xFF) {
            return "UTF-32BE";
        }
        
        // Check for UTF-32 LE BOM
        if (rawData.length >= 4 && rawData[0] == 0xFF && rawData[1] == 0xFE
                && rawData[2] == 0x00 && rawData[3] == 0x00) {
            return "UTF-32LE";
        }
        
        // Determine charset based on URL patterns for Chinese stock APIs
        // This is more reliable than byte sequence analysis
        if (url != null) {
            // Tencent stock API (qt.gtimg.cn) returns GBK
            if (url.contains("qt.gtimg.cn")) {
                return "GBK";
            }
            // Sina APIs may return GBK
            if (url.contains("sinajs.cn")) {
                return "GBK";
            }
            // EastMoney APIs typically return UTF-8
            if (url.contains("eastmoney.com")) {
                return "UTF-8";
            }
            // Fund APIs (1234567.com.cn) typically return UTF-8
            if (url.contains("1234567.com.cn")) {
                return "UTF-8";
            }
        }
        
        // Default to GBK for Chinese stock APIs
        return "GBK";
    }
    
    /**
     * Check if the byte array contains valid UTF-8 sequences
     */
    private boolean isValidUtf8(byte[] data) {
        int i = 0;
        while (i < data.length) {
            int byte1 = data[i] & 0xFF;
            
            if (byte1 <= 0x7F) {
                // ASCII
                i++;
            } else if ((byte1 & 0xE0) == 0xC0) {
                // 2-byte sequence
                if (i + 1 >= data.length) return false;
                int byte2 = data[i + 1] & 0xC0;
                if (byte2 != 0x80) return false;
                i += 2;
            } else if ((byte1 & 0xF0) == 0xE0) {
                // 3-byte sequence
                if (i + 2 >= data.length) return false;
                int byte2 = data[i + 1] & 0xC0;
                int byte3 = data[i + 2] & 0xC0;
                if (byte2 != 0x80 || byte3 != 0x80) return false;
                i += 3;
            } else if ((byte1 & 0xF8) == 0xF0) {
                // 4-byte sequence
                if (i + 3 >= data.length) return false;
                int byte2 = data[i + 1] & 0xC0;
                int byte3 = data[i + 2] & 0xC0;
                int byte4 = data[i + 3] & 0xC0;
                if (byte2 != 0x80 || byte3 != 0x80 || byte4 != 0x80) return false;
                i += 4;
            } else {
                return false;
            }
        }
        return true;
    }


//    public static void main(String[] args) throws Exception {
//        System.out.println(HttpClientPool.getInstance().get("http://qt.gtimg.cn/q=s_sh600519,s_sz000001,s_usAAPL"));
//    }
}
