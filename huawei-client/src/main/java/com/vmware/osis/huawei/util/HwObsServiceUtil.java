package com.vmware.osis.huawei.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.XML;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Administrator
 * @ClassName HwObsServiceUtil
 * @Description TODO
 **/
public class HwObsServiceUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HwObsServiceUtil.class);

    public static void main(String[] str) throws NoSuchAlgorithmException, KeyManagementException {
        String endpoint = "https://10.100.1.21";
        //String endpoint = "10.100.1.21";
        String bucketname = "testWang1102";
        String accessKey = "BA430CF08DCB201BB2B7"; //取值为获取的AK
        String securityKey = "0pl7WZAxXoZC/WZI5DW4xhQ6f1cAAAGLjcsgG0IZ";  //取值为获取的SK
        //LOGGER.info(listAllMyBuckets(endpoint, accessKey, securityKey).getData());
        //getBucketStorageInfo(endpoint,accessKey,securityKey,bucketname);
        createBucket(endpoint, accessKey, securityKey, bucketname);
    }

    public static Result<String> listAllMyBuckets(String endpoint, String accessKey, String securityKey) {
        LOGGER.info("listAllMyBuckets begin!endpoint={}", endpoint);

        int statusCode = 500;
        String errMsg = "";
        String dataStr = "";
        HttpClient httpClient;
        try {
            httpClient = HwClientUtil.getHttpClient();
            String requestTime = DateUtils.formateDate(System.currentTimeMillis());
            HttpUriRequest httpGet = new HttpGet(endpoint);
            httpGet.addHeader("Date", requestTime);
            /** 根据请求计算签名**/
            String contentMD5 = "";
            String contentType = "";
            String canonicalizedHeaders = "";
            String canonicalizedResource = "/";
            // Content-MD5 、Content-Type 没有直接换行， data格式为RFC 1123，和请求中的时间一致
            String canonicalString = "GET" + "\n" + contentMD5 + "\n" + contentType + "\n" + requestTime + "\n"
                + canonicalizedHeaders + canonicalizedResource;
            LOGGER.info("StringToSign:[" + canonicalString + "]");
            String signature = Signature.signWithHmacSha1(securityKey, canonicalString);

            // 增加签名头域 Authorization: AWS AccessKeyID:signature
            httpGet.addHeader("Authorization", "AWS " + accessKey + ":" + signature);

            // 打印发送请求信息和收到的响应消息
            LOGGER.info("Request Message:" + httpGet.getRequestLine());
            HttpResponse httpResponse = httpClient.execute(httpGet);
            LOGGER.info("Response Message:" + httpResponse.getStatusLine());
            statusCode = httpResponse.getStatusLine().getStatusCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuffer response = new StringBuffer();
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();

            dataStr = XML.toJSONObject(response.toString()).toString();
            if (statusCode / 100 != 2) {
                JSONObject errObj = JSONUtil.parseObj(dataStr).getJSONObject("Error");
                errMsg = errObj.getStr("Message");
            }
        } catch (NoSuchAlgorithmException ex) {
            errMsg = ex.getMessage();
        } catch (KeyManagementException ex) {
            errMsg = ex.getMessage();
        } catch (UnsupportedEncodingException ex) {
            errMsg = ex.getMessage();
        } catch (ClientProtocolException ex) {
            errMsg = ex.getMessage();
        } catch (IOException ex) {
            errMsg = ex.getMessage();
        }
        LOGGER.info("listAllMyBuckets end!endpoint={},statusCode={},dataStr={}", endpoint, statusCode, dataStr);

        return ResultBuilder.custom().data(dataStr).code(statusCode).msg(errMsg).build();
    }

    public static Result<String> getBucketStorageInfo(String endpoint, String accessKey, String securityKey,
        String bucketName) {
        LOGGER.info("getBucketStorageInfo begin!endpoint={}", endpoint);

        int statusCode = 500;
        String errMsg = "";
        HttpClient httpClient;
        try {
            httpClient = HwClientUtil.getHttpClient();
            String requesttime = DateUtils.formateDate(System.currentTimeMillis());
            String action = "?storageinfo";
            // 获取桶名为bucket001的桶的存量信息，构造请求
            HttpGet httpGet = new HttpGet(endpoint + "/" + bucketName + action);
            httpGet.addHeader("Date", requesttime);

            /** 根据请求计算签名**/
            String contentMD5 = "";
            String contentType = "";
            String canonicalizedHeaders = "";
            // 构造请求资源
            String canonicalizedResource = "/" + bucketName + action;
            // Content-MD5 、Content-Type 没有直接换行， data格式为RFC 1123，和请求中的时间一致
            String canonicalString = "GET" + "\n" + contentMD5 + "\n" + contentType + "\n" + requesttime + "\n"
                + canonicalizedHeaders + canonicalizedResource;
            LOGGER.info("StringToSign:[" + canonicalString + "]");
            String signature = Signature.signWithHmacSha1(securityKey, canonicalString);
            // 增加签名头域 Authorization: AWS AccessKeyID:signature
            httpGet.addHeader("Authorization", "AWS " + accessKey + ":" + signature);

            LOGGER.info("Request Message:" + httpGet.getRequestLine());
            HttpResponse httpResponse = httpClient.execute(httpGet);
            LOGGER.info("Response Message:" + httpResponse.getStatusLine());
            statusCode = httpResponse.getStatusLine().getStatusCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuffer response = new StringBuffer();
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();

            String jsonStr = XML.toJSONObject(response.toString()).toString();
            if (statusCode / 100 != 2) {
                JSONObject errObj = JSONUtil.parseObj(jsonStr).getJSONObject("Error");
                errMsg = errObj.getStr("Message");
            } else {
                return ResultBuilder.success(jsonStr, statusCode).build();
            }
            LOGGER.info(response.toString());
        } catch (NoSuchAlgorithmException ex) {
            errMsg = ex.getMessage();
        } catch (KeyManagementException ex) {
            errMsg = ex.getMessage();
        } catch (UnsupportedEncodingException ex) {
            errMsg = ex.getMessage();
        } catch (ClientProtocolException ex) {
            errMsg = ex.getMessage();
        } catch (IOException ex) {
            errMsg = ex.getMessage();
        }
        LOGGER.info("getBucketStorageInfo end!endpoint={},statusCode={}", endpoint, statusCode);

        return ResultBuilder.failure(errMsg, statusCode).build();
    }

    private static void createBucket(String endpoint, String accessKey, String securityKey, String bucketName)
        throws NoSuchAlgorithmException, KeyManagementException {
        HttpClient httpClient = HwClientUtil.getHttpClient();
        String requesttime = DateUtils.formateDate(System.currentTimeMillis());

        //构造请求为创桶bucket001
        HttpPut httpPut = new HttpPut(endpoint + "/" + bucketName);

        httpPut.addHeader("Date", requesttime);

        /** 根据请求计算签名**/
        String contentMD5 = "";
        String contentType = "";

        // 创建桶需要增加的请求头,用于发送请求和鉴权摘要
        Map<String, String> awsHeader = new LinkedHashMap<>();
        //设置桶的acl权限，如需增加其他x-amz开头的请求体，请在map中继续添加
        awsHeader.put("x-amz-acl", "public-read");

        // 创建桶指定桶位置信息，可以设置为null
        String location = null;
        // 指定桶位置信息需要增加contentType
        if (location != null) {
            contentType = "text/plain; charset=ISO-8859-1";
        }
        String canonicalizedHeaders = "";
        for (Map.Entry entry : awsHeader.entrySet()) {
            canonicalizedHeaders = canonicalizedHeaders + entry.getKey() + ":" + entry.getValue() + "\n";
        }
        String canonicalizedResource = "/" + bucketName;
        // Content-MD5 、Content-Type 没有直接换行， data格式为RFC 1123，和请求中的时间一致
        String canonicalString = "PUT" + "\n" + contentMD5 + "\n" + contentType + "\n" + requesttime + "\n"
            + canonicalizedHeaders + canonicalizedResource;
        LOGGER.info("StringToSign:[" + canonicalString + "]");
        try {
            String signature = Signature.signWithHmacSha1(securityKey, canonicalString);
            // 增加签名头域 Authorization: AWS AccessKeyID:signature
            httpPut.addHeader("Authorization", "AWS " + accessKey + ":" + signature);
            for (Map.Entry entry : awsHeader.entrySet()) {
                httpPut.addHeader(entry.getKey().toString(), entry.getValue().toString());
            }
            // 设置body请求体中的数据
            if (location != null) {
                httpPut.setEntity(new StringEntity(location));
            }
            HttpResponse httpResponse = httpClient.execute(httpPut);

            // 打印发送请求信息和收到的响应消息
            LOGGER.info("Request Message:{}", httpPut.getRequestLine());
            for (Header header : httpPut.getAllHeaders()) {
                LOGGER.info(header.getName() + ":" + header.getValue());
            }
            LOGGER.info("Response Message:{}", httpResponse.getStatusLine());
            for (Header header : httpResponse.getAllHeaders()) {
                LOGGER.info(header.getName() + ":" + header.getValue());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            LOGGER.info(response.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
