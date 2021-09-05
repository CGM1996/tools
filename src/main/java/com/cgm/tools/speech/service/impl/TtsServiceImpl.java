package com.cgm.tools.speech.service.impl;

import com.cgm.tools.base.BaseException;
import com.cgm.tools.base.Constant;
import com.cgm.tools.base.ErrorCode;
import com.cgm.tools.config.ExtraConfig;
import com.cgm.tools.speech.common.ConnUtil;
import com.cgm.tools.speech.common.DemoException;
import com.cgm.tools.speech.common.TokenHolder;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import com.cgm.tools.speech.service.ITtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Slf4j
@Service
public class TtsServiceImpl implements ITtsService {
    // 可以使用https
    public final String url = "http://tsn.baidu.com/text2audio";

    @Resource
    private ExtraConfig extraConfig;

    @Override
    public void textTts(HttpServletResponse response, BaiduTtsParam paramDTO) throws DemoException, IOException {
        TokenHolder holder = new TokenHolder(extraConfig.getBaiduAppKey(), extraConfig.getBaiduSecretKey(),
                TokenHolder.ASR_SCOPE);
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
        // 反馈请带上此url，浏览器上可以测试
        System.out.println(url + "?" + params);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        PrintWriter printWriter = new PrintWriter(conn.getOutputStream());
        printWriter.write(params);
        printWriter.close();
        String contentType = conn.getContentType();
        if (contentType.contains("audio/")) {
            response.setContentType(contentType);
            byte[] bytes = ConnUtil.getResponseBytes(conn);
            String format = getFormat(paramDTO.getAue());
            // 打开mp3文件即可播放
            File file = new File("result." + format);
            FileOutputStream os = new FileOutputStream(file);
            os.write(bytes);
            os.close();

            response.setHeader("Content-disposition",
                    "attachment;filename=" + URLEncoder.encode(file.getName(), "utf-8"));
            response.setContentLength((int) file.length());

            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                FileCopyUtils.copy(inputStream, response.getOutputStream());
            } catch (FileNotFoundException e) {
                log.error(Constant.EXCEPTION_LOG_TITLE, e);
                throw new BaseException(ErrorCode.SYS_FILE_NOT_FOUND);
            } catch (IOException e) {
                log.error(Constant.EXCEPTION_LOG_TITLE, e);
                throw new BaseException(ErrorCode.SYS_IO_EXCEPTION);
            }
        } else {
            System.err.println("ERROR: content-type= " + contentType);
            String res = ConnUtil.getResponseString(conn);
            System.err.println(res);
            throw new BaseException(ErrorCode.SYS_INTERNAL_ERROR);
        }
    }

    @Override
    public void fileTts(HttpServletResponse response, MultipartFile file, BaiduTtsParam param) {

    }


    // 下载的文件格式, 3：mp3(default) 4： pcm-16k 5： pcm-8k 6. wav
    private String getFormat(int aue) {
        String[] formats = {"mp3", "pcm", "pcm", "wav"};
        return formats[aue - 3];
    }
}
