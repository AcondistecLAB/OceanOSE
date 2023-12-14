package com.vmware.osis.huawei.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;


public class BucketUtil {
    public static String accessKey = "s3user"; //取值为获取的AK
    public static String securityKey = "Pbu4@123";  //取值为获取的SK

    public static void main(String[] str) {

        listAllMyBuckets();

    }


    private static void listAllMyBuckets() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String requesttime = DateUtils.formateDate(System.currentTimeMillis());
        HttpGet httpGet = new HttpGet("http://obs.myclouds.com/doc/2015-06-30/");
        httpGet.addHeader("Date", requesttime);

        /** 根据请求计算签名**/
        String contentMD5 = "";
        String contentType = "";
        String canonicalizedHeaders = "";
        String canonicalizedResource = "/";
        // Content-MD5 、Content-Type 没有直接换行， data格式为RFC 1123，和请求中的时间一致
        String canonicalString = "GET" + "\n" + contentMD5 + "\n" + contentType + "\n" + requesttime + "\n" + canonicalizedHeaders + canonicalizedResource;
        System.out.println("StringToSign:[" + canonicalString + "]");
        String signature = null;
        try {
            signature = Signature.signWithHmacSha1(securityKey, canonicalString);

            // 增加签名头域 Authorization: AWS AccessKeyID:signature
            httpGet.addHeader("Authorization", "AWS " + accessKey + ":" + signature);
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

            // 打印发送请求信息和收到的响应消息
            System.out.println("Request Message:");
            System.out.println(httpGet.getRequestLine());
            for (Header header : httpGet.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }

            System.out.println("Response Message:");
            System.out.println(httpResponse.getStatusLine());
            for (Header header : httpResponse.getAllHeaders()) {
                System.out.println(header.getName() + ":" + header.getValue());
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpResponse.getEntity().getContent()));

            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            // print result
            System.out.println(response.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}

