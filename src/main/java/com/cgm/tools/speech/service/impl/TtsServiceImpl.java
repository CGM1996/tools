package com.cgm.tools.speech.service.impl;

import com.cgm.tools.base.BaseException;
import com.cgm.tools.base.ErrorCode;
import com.cgm.tools.config.ExtraConfig;
import com.cgm.tools.speech.common.ConnUtil;
import com.cgm.tools.speech.common.DemoException;
import com.cgm.tools.speech.common.TokenHolder;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import com.cgm.tools.speech.service.ITtsService;
import com.cgm.tools.speech.subtitleFile.Convert;
import com.cgm.tools.speech.subtitleFile.FatalParsingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Arrays;

@Slf4j
@Service
public class TtsServiceImpl implements ITtsService {
    // 可以使用https
    public final String url = "http://tsn.baidu.com/text2audio";

    private static final String DEFAULT_CHARSET = "utf-8";

    @Resource
    private ExtraConfig extraConfig;

    @Override
    public void textTts(HttpServletResponse response, BaiduTtsParam paramDTO) throws DemoException, IOException {
        String appKey = extraConfig.getBaiduAppKey();
        String secretKey = extraConfig.getBaiduSecretKey();
        TokenHolder holder = new TokenHolder(appKey, secretKey, TokenHolder.ASR_SCOPE);
        holder.refresh();
        String token = holder.getToken();
        paramDTO.setTok(token);

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

        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(params, DEFAULT_CHARSET);
        httpPost.setEntity(entity);
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse closeableHttpResponse = httpClient.execute(httpPost);
        InputStream responseContent = closeableHttpResponse.getEntity().getContent();

        String contentType = closeableHttpResponse.getEntity().getContentType().getValue();
        if (contentType.contains("audio/")) {
            response.setContentType(contentType);
            String format = getFormat(paramDTO.getAue());

            response.setHeader("Content-disposition",
                    "attachment;filename=" + URLEncoder.encode("result." + format, "utf-8"));
            FileCopyUtils.copy(responseContent, response.getOutputStream());
        } else {
            System.err.println("ERROR: content-type= " + contentType);
            byte[] result = ConnUtil.getInputStreamContent(responseContent);
            System.err.println(new String(result));
            throw new BaseException(ErrorCode.SYS_INTERNAL_ERROR);
        }
    }

    @Override
    public void fileTts(HttpServletResponse response, MultipartFile file, BaiduTtsParam param)
            throws IOException, FatalParsingException, DemoException {
        String saveDir = System.getProperty("user.dir") + "/";
        Assert.isTrue(file.getOriginalFilename() != null, ErrorCode.USER_INVALID_INPUT);
        String filename = file.getOriginalFilename();
        Assert.isTrue(filename != null, ErrorCode.SYS_INTERNAL_ERROR);
        File saveFile = this.getUniqueFile(saveDir, filename);

        // MultipartFile转File, 保存
        file.transferTo(saveFile);

        String[] stringArray = Convert.toStringArray(saveFile.getAbsolutePath(), "SRT");
        int currentLength = 0;
        int fullCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : stringArray) {
            if (currentLength + s.length() < 2000) {
                stringBuilder.append(s);
                currentLength += s.length();
            } else {
                fullCount ++;
                if (fullCount == param.getPart()) {
                    param.setTex(stringBuilder.toString());
                    this.textTts(response, param);
                    return;
                }
                stringBuilder.delete(0, stringBuilder.length());
                currentLength = 0;

            }
        }
        param.setTex(stringBuilder.toString());
        this.textTts(response, param);
    }


    // 下载的文件格式, 3：mp3(default) 4： pcm-16k 5： pcm-8k 6. wav
    private String getFormat(int aue) {
        String[] formats = {"mp3", "pcm", "pcm", "wav"};
        return formats[aue - 3];
    }

    /**
     * 获取一个不重名的文件
     *
     * @param dir      目录
     * @param filename 文件名
     * @return 文件
     */
    private File getUniqueFile(String dir, String filename) {
        File uniqueFile = new File(dir, filename);

        // 重名处理
        int sameCount = 1;
        String newFilename;
        while (uniqueFile.exists()) {
            int potIndex = filename.indexOf(".");
            // 有扩展名的序号加在扩展名前, "."开头的除外
            if (potIndex > 0) {
                newFilename = filename.substring(0, potIndex) + "(" + sameCount + ")" + filename.substring(potIndex);
            } else {
                newFilename = filename + "(" + sameCount + ")";
            }
            uniqueFile = new File(dir, newFilename);
            sameCount++;
        }
        return uniqueFile;
    }
}
