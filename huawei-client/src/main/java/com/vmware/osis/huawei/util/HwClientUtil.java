package com.vmware.osis.huawei.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.XML;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HwClientUtil {
    private static final Logger log = LoggerFactory.getLogger(HwClientUtil.class);

    private static final String CHARSET_UTF_8 = "UTF-8";

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static void main(String[] args) {
        Map<String, String> businessParams = new HashMap<>();
        businessParams.put("UserName", "crappy-firebrick-swan");
        //businessParams.put("PolicyName", "vcd_readWrite");
        //businessParams.put("PolicyDocument", "{\"Statement\": [{\"Sid\": \"vcd_readWrite\",\"Effect\": \"Allow\",\"Action\":[\"s3:*\"],\"Resource\": \"*\"}]}");

        String ak = "AEEB09FC953686FB4A0F";
        String sk = "zhTr1NzIvk1uQV13FjC9J4edOyMAAAGAlTaG/pAt";
        String endpoint = "https://192.168.128.10:9443/";
        String action = "GetSummary";
        Result<String> result = executeGet(action, null, endpoint, ak, sk);
        if (result.is2xxSuccess()) {
            System.out.println(action + " success!" + result.getData());
        } else {
            System.out.println(action + " failed!" + result.getMsg());
        }
    }

    public static Result<String> executeAdminGet(String action, Map<String, String> businessParams, String endpoint,
        String ak, String sk) {
        return executeGet(action, businessParams, endpoint, ak, sk);
    }


    /**
     * @param action 接口名称
     * @param businessParams 业务参数Map
     * @return Result<String>
     */
    public static Result<String> executeGet(String action, Map<String, String> businessParams, String endpoint,
        String ak, String sk) {
        return execute(action, "/poe/rest", businessParams, "GET", endpoint, ak, sk);
    }

    public static Result<String> execute(String action, String url, Map<String, String> businessParams,
        String httpMethod, String endpoint, String ak, String sk) {
        if (businessParams == null) {
            businessParams = new HashMap<>();
        }
        businessParams.put("Action", action);

        SimpleDateFormat df = new SimpleDateFormat(DATE_PATTERN);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String requestTime = df.format(new Date());

        StringBuilder requestString = new StringBuilder();
        //requestString.append("https://" + URL + ":" + port);
        String processEndpoint = endpoint;
        if (processEndpoint.endsWith("/")) {
            processEndpoint = processEndpoint.substring(0, processEndpoint.length() - 1);
        }
        requestString.append(processEndpoint);
        requestString.append(urlEncode(url, true));
        requestString.append('?');
        businessParams.forEach((k, v) -> {
            requestString.append("&").append(k + "=").append(urlEncode(v, false));
        });

        // 添加公共请求参数，包括AWSAccessKeyId、SignatureMethod、SignatureVersion、Timestamp/Expires、Signature
        requestString.append("&").append("AWSAccessKeyId=").append(urlEncode(ak, false));
        requestString.append("&").append("SignatureMethod=").append(urlEncode("HmacSHA256", false));
        requestString.append("&").append("SignatureVersion=").append(urlEncode("2", false));
        requestString.append("&").append("Timestamp=").append(urlEncode(requestTime, false));
        String errMsg = null;
        int statusCode = 500;
        try {
            // 签名参数
            requestString.append("&").append("Signature=");
            int index = processEndpoint.indexOf("//");
            String signature = sign(httpMethod, processEndpoint.substring(index + 2), url,
                getSignParameters(businessParams, ak, requestTime), sk);
            requestString.append(urlEncode(signature, false));
            String stringReq = requestString.toString();
            log.info("{} Request Message:{}", action, stringReq);

            HttpClient httpClient = getHttpClient();
            HttpGet httpGet = new HttpGet(stringReq);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            log.info("{} Response statusCode:{}", action, statusCode);
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String inputLine;
            StringBuffer responseSB = new StringBuffer();
            while ((inputLine = reader.readLine()) != null) {
                responseSB.append(inputLine);
            }
            reader.close();

            // 转化成json格式字符串
            String dataStr = XML.toJSONObject(responseSB.toString()).toString();

            Result<String> result = null;
            if (statusCode / 100 != 2) {
                JSONObject errObj = JSONUtil.parseObj(dataStr).getJSONObject("ErrorResponse").getJSONObject("Error");
                result = ResultBuilder.failure(errObj.getStr("Message"), statusCode).build();
            } else {
                result = ResultBuilder.success(dataStr, statusCode).build();
            }
            log.info("{} Response Message,statusCode={},response={}", action, statusCode, dataStr);

            return result;
        } catch (NoSuchAlgorithmException ex) {
            errMsg = ex.getMessage();
        } catch (IOException ex) {
            errMsg = ex.getMessage();
        } catch (KeyManagementException ex) {
            errMsg = ex.getMessage();
        } catch (InvalidKeyException ex) {
            errMsg = ex.getMessage();
        } catch (Exception ex) {
            errMsg = ex.getMessage();
        }
        return ResultBuilder.failure(errMsg, statusCode).build();
    }

    /**
     * url编码
     *
     * @param value 要编码的数据
     * @param path 是否是rul地址
     * @return 返回编码后的值
     */
    public static String urlEncode(String value, boolean path) {
        try {
            String encoded = URLEncoder.encode(value, CHARSET_UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
            if (path) {
                encoded = encoded.replace("%2F", "/");
            }
            return encoded;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }
    }

    private static Map<String, String> getSignParameters(Map<String, String> params, String ak, String timeStamp) {
        Map<String, String> signParameters = new HashMap<>(params);
        signParameters.put("AWSAccessKeyId", ak);
        signParameters.put("SignatureMethod", "HmacSHA256");
        signParameters.put("SignatureVersion", "2");
        signParameters.put("Timestamp", timeStamp);
        return signParameters;
    }

    /**
     * 签名
     *
     * @param httpMethod 请求方法
     * @param host 请求地址
     * @param uri 请求资源
     * @param signParameters 签名参数
     * @param secretKey admin的私钥
     * @return 返回签名后的值
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    private static String sign(String httpMethod, String host, String uri, Map<String, String> signParameters,
        String secretKey) throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {
        String signatureMethod = signParameters.get("SignatureMethod");
        StringBuilder data = new StringBuilder();
        data.append(httpMethod).append("\n");
        data.append(host).append("\n");
        data.append(urlEncode(uri, true)).append("\n");
        data.append(getCanonicalizedQueryString(signParameters));
        String stringToSign = data.toString();
        return sign(stringToSign.getBytes(CHARSET_UTF_8), secretKey, signatureMethod);
    }

    /**
     * 各参数次序按照参数名的字典顺序（排序区分大小写）
     */
    private static String getCanonicalizedQueryString(Map<String, String> parameters) {
        SortedMap<String, String> sorted = new TreeMap<String, String>();
        sorted.putAll(parameters);
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> pairs = sorted.entrySet().iterator();
        while (pairs.hasNext()) {
            Map.Entry<String, String> pair = pairs.next();
            String key = pair.getKey();
            String value = pair.getValue();
            builder.append(urlEncode(key, false));
            builder.append("=");
            builder.append(urlEncode(value, false));
            if (pairs.hasNext()) {
                builder.append("&");
            }
        }
        return builder.toString();
    }

    private static String sign(byte[] data, String key, String algorithm)
        throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key.getBytes(Charset.defaultCharset()), algorithm));
        byte[] signature = Base64.encodeBase64(mac.doFinal(data));
        return new String(signature, Charset.defaultCharset());
    }

    /**
     * 生成HttpClient
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        SSLContext ctx = SSLContext.getInstance("TLS");
        CustomTrustManager tm = new CustomTrustManager();
        ctx.init((KeyManager[]) null, new TrustManager[] {tm}, (SecureRandom) null);
        SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme sch = new Scheme("https", 443, ssf);
        schemeRegistry.register(sch);
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(schemeRegistry);
        cm.setDefaultMaxPerRoute(10);
        cm.setMaxTotal(100);
        HttpClient client = new DefaultHttpClient(cm);
        client.getParams().setParameter("http.protocol.content-charset", CHARSET_UTF_8);
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 5000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 180000);
        return client;
    }

    private static class CustomTrustManager implements X509TrustManager {

        private CustomTrustManager() {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }
    }

}