package com.cgm.tools.util;

import com.cgm.tools.speech.common.ConnUtil;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * @author cgm
 */
public class HttpUtils {
    private static final String DEFAULT_CHARSET = "utf-8";

    private HttpUtils() {}

    public static String doGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        return doRequest(httpGet);
    }

    public static String doPost(String url, String requestBody) {
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(requestBody, DEFAULT_CHARSET);
        httpPost.setEntity(entity);
        return doRequest(httpPost);
    }

    public static String baiduParamToString (BaiduTtsParam paramDTO) {
        // 此处2次url encode， 确保特殊字符被正确编码
        String params = "tex=" + ConnUtil.urlEncode(ConnUtil.urlEncode(paramDTO.getTex()));
        params += "&per=" + paramDTO.getPer();
        params += "&spd=" + paramDTO.getSpd();
        params += "&pit=" + paramDTO.getPit();
        params += "&vol=" + paramDTO.getVol();
        params += "&cuid=" + paramDTO.getCuid();
        params += "&tok=" + paramDTO.getTok();
        params += "&aue=" + paramDTO.getAue();
        params += "&lan=zh&ctp=1";
        return params;
    }

    private static String doRequest(HttpRequestBase requestBase) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
             CloseableHttpResponse response = httpClient.execute(requestBase)) {
            return EntityUtils.toString(response.getEntity(), DEFAULT_CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
