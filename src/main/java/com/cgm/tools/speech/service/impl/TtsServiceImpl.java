package com.cgm.tools.speech.service.impl;

import com.cgm.tools.base.BaseException;
import com.cgm.tools.base.Constant;
import com.cgm.tools.base.ErrorCode;
import com.cgm.tools.config.ExtraConfig;
import com.cgm.tools.speech.common.ConnUtil;
import com.cgm.tools.speech.common.DemoException;
import com.cgm.tools.speech.common.TokenHolder;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import com.cgm.tools.speech.dto.VideoAddSpeechParam;
import com.cgm.tools.speech.service.ITtsService;
import com.cgm.tools.speech.subtitleFile.*;
import com.cgm.tools.util.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TtsServiceImpl implements ITtsService {
    // 可以使用https
    public static final String API_URL = "http://tsn.baidu.com/text2audio";

    private static final String DEFAULT_CHARSET = "utf-8";

    public static final String HEADER_NAME = "Content-disposition";

    public static final String ENV_USER_DIR = "user.dir";

    private final ExtraConfig extraConfig;

    @Autowired
    public TtsServiceImpl(ExtraConfig extraConfig) {
        this.extraConfig = extraConfig;
    }

    @Override
    public void textTts(HttpServletResponse response, BaiduTtsParam paramDTO) throws IOException {
        paramDTO.setTok(this.getToken());

        // 此处2次url encode， 确保特殊字符被正确编码
        String params = HttpUtils.baiduParamToString(paramDTO);

        HttpPost httpPost = new HttpPost(API_URL);
        StringEntity entity = new StringEntity(params, DEFAULT_CHARSET);
        httpPost.setEntity(entity);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            CloseableHttpResponse closeableHttpResponse = httpClient.execute(httpPost);
            InputStream responseContent = closeableHttpResponse.getEntity().getContent();

            String contentType = closeableHttpResponse.getEntity().getContentType().getValue();
            if (contentType.contains("audio/")) {
                response.setContentType(contentType);
                String format = getFormat(paramDTO.getAue());

                response.setHeader(HEADER_NAME, getHeaderValue("result." + format));
                FileCopyUtils.copy(responseContent, response.getOutputStream());
            } else {
                log.error("ERROR: content-type= {}", contentType);
                byte[] result = ConnUtil.getInputStreamContent(responseContent);
                log.error(new String(result));
                throw new BaseException(ErrorCode.SYS_INTERNAL_ERROR);
            }
        }

    }

    @Override
    public void fileTts(HttpServletResponse response, MultipartFile file, BaiduTtsParam param)
            throws FatalParsingException, IOException {
        if (param.isMerge()) {
            this.fileTtsMerge(response, file, param);
        } else {
            this.fileTtsOnTime(response, file, param);
        }
    }

    public void fileTtsMerge(HttpServletResponse response, MultipartFile file, BaiduTtsParam param)
            throws IOException, FatalParsingException {
        String saveDir = System.getProperty(ENV_USER_DIR) + "/";
        Assert.isTrue(file.getOriginalFilename() != null, ErrorCode.USER_INVALID_INPUT);
        String filename = file.getOriginalFilename();
        Assert.isTrue(filename != null, ErrorCode.SYS_INTERNAL_ERROR);
        Assert.isTrue(filename.contains("."), ErrorCode.USER_INVALID_INPUT);
        File saveFile = this.getUniqueFile(saveDir, filename);

        // MultipartFile转File, 保存
        file.transferTo(saveFile);

        String format = filename.substring(filename.lastIndexOf(".") + 1);
        String[] stringArray = Convert.toStringArray(saveFile.getAbsolutePath(), format);
        saveFile.deleteOnExit();
        int currentLength = 0;
        int fullCount = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : stringArray) {
            if (currentLength + s.length() < 2000) {
                if (StringUtils.hasText(s)) {
                    stringBuilder.append(s).append("。");
                    currentLength += s.length() + 1;
                }
            } else {
                fullCount++;
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

    public void fileTtsOnTime(HttpServletResponse response, MultipartFile captions, BaiduTtsParam param)
            throws IOException {
        param.setTok(this.getToken());
        String saveDir = System.getProperty(ENV_USER_DIR) + "/";

        Assert.isTrue(captions.getOriginalFilename() != null, ErrorCode.USER_INVALID_INPUT);
        String captionsFilename = captions.getOriginalFilename();
        Assert.isTrue(captionsFilename != null, ErrorCode.SYS_INTERNAL_ERROR);
        Assert.isTrue(captionsFilename.contains("."), ErrorCode.USER_INVALID_INPUT);
        File captionsFile = this.getUniqueFile(saveDir, captionsFilename);

        // MultipartFile转File, 保存
        captions.transferTo(captionsFile);

        String output = extraConfig.getOutputFolder() + "web-output.mp3";
        File outputFile = new File(output);

        try (FrameRecorder recorder = new FFmpegFrameRecorder(output, 1)) {
            //创建录制
            recorder.setFormat("mp3");
            recorder.start();

            // 录入音频
            Frame audioFrame;
            String format = captionsFilename.substring(captionsFilename.lastIndexOf(".") + 1);
            TimedTextObject tto = Convert.toTto(captionsFilename, format);

            int currentFrameIndex = 0;
            for (Caption current : tto.captions.values()) {
                Time start = current.start;
                String[] lines = FormatSRT.cleanTextForSRT(current);
                int audioPerFrameMills = 36;
                int startFrameIndex = start.getMseconds() / audioPerFrameMills;
                log.info("Current text: {}", lines[0]);

                this.getSeqAudioIS(lines, param);
                File tempFile = new File(extraConfig.getOutputFolder() + "temp.mp3");
                try (FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(tempFile)) {
                    audioGrabber.start();
                    while ((audioFrame = audioGrabber.grab()) != null) {
                        while (currentFrameIndex < startFrameIndex) {
                            recorder.record(audioFrame);
                            currentFrameIndex++;
                        }
                        recorder.record(audioFrame);
                        currentFrameIndex++;
                    }
                    audioGrabber.stop();
                }
            }

            recorder.flush();
            recorder.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.setContentType("audio/mp3");
        response.setHeader(HEADER_NAME, getHeaderValue("audio-on-time.mp3"));
        response.setContentLength((int) outputFile.length());

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(outputFile))) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        } catch (FileNotFoundException e) {
            log.error(Constant.EXCEPTION_LOG_TITLE, e);
            throw new BaseException(ErrorCode.SYS_FILE_NOT_FOUND);
        } catch (IOException e) {
            log.error(Constant.EXCEPTION_LOG_TITLE, e);
            throw new BaseException(ErrorCode.SYS_IO_EXCEPTION);
        }
    }

    @Override
    public void videoAddSpeech(HttpServletResponse response, MultipartFile video, MultipartFile captions,
                               VideoAddSpeechParam param) throws IOException {
        String saveDir = System.getProperty(ENV_USER_DIR) + "/";
        Assert.isTrue(video.getOriginalFilename() != null, ErrorCode.USER_INVALID_INPUT);
        String videoFilename = video.getOriginalFilename();
        Assert.isTrue(videoFilename != null, ErrorCode.SYS_INTERNAL_ERROR);
        Assert.isTrue(videoFilename.contains("."), ErrorCode.USER_INVALID_INPUT);
        File videoFile = this.getUniqueFile(saveDir, videoFilename);

        Assert.isTrue(captions.getOriginalFilename() != null, ErrorCode.USER_INVALID_INPUT);
        String captionsFilename = captions.getOriginalFilename();
        Assert.isTrue(captionsFilename != null, ErrorCode.SYS_INTERNAL_ERROR);
        Assert.isTrue(captionsFilename.contains("."), ErrorCode.USER_INVALID_INPUT);
        File captionsFile = this.getUniqueFile(saveDir, captionsFilename);

        // MultipartFile转File, 保存
        video.transferTo(videoFile);
        captions.transferTo(captionsFile);

        File file = new File(videoFilename);
        if (!file.exists()) {
            return;
        }
        FFmpegFrameGrabber audioGrabber = null;
        String output = extraConfig.getOutputFolder() + "web-output.mp4";
        File outputFile = new File(output);

        try (FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoFilename);
             FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, videoGrabber.getImageWidth(),
                     videoGrabber.getImageHeight(), 1)) {
            //抓取视频帧
            videoGrabber.start();
            //创建录制
            recorder.setFormat("mp4");
            recorder.setFrameRate(videoGrabber.getFrameRate());
            recorder.setVideoQuality(param.getVideoQuality());
            recorder.setVideoCodec(param.isUseH264() ? avcodec.AV_CODEC_ID_H264 : avcodec.AV_CODEC_ID_NONE);
            recorder.start();

            // 录入音频
            Frame audioFrame;
            String format = captionsFilename.substring(captionsFilename.lastIndexOf(".") + 1);
            TimedTextObject tto = Convert.toTto(captionsFilename, format);

            int currentFrameIndex = 0;
            for (Caption current : tto.captions.values()) {
                Time start = current.start;
                String[] lines = FormatSRT.cleanTextForSRT(current);
                int audioPerFrameMills = 36;
                int startFrameIndex = start.getMseconds() / audioPerFrameMills;
                audioGrabber = new FFmpegFrameGrabber(this.getSeqAudioIS(lines));
                log.info("Channels: {}", audioGrabber.getAudioChannels());
                audioGrabber.start();
                while ((audioFrame = audioGrabber.grab()) != null) {
                    while (currentFrameIndex < startFrameIndex) {
                        recorder.record(audioFrame);
                        currentFrameIndex++;
                    }
                    recorder.record(audioFrame);
                    currentFrameIndex++;
                }
                audioGrabber.stop();
            }

            // 录入视频
            Frame videoFrame;
            recorder.setTimestamp(0);
            while ((videoFrame = videoGrabber.grab()) != null) {
                recorder.record(videoFrame);
            }

            videoGrabber.stop();
            recorder.flush();
            recorder.stop();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (audioGrabber != null) {
                    audioGrabber.release();
                }
            } catch (FFmpegFrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }

        response.setHeader(HEADER_NAME, getHeaderValue(output));
        response.setContentLength((int) outputFile.length());

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(outputFile))) {
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        } catch (FileNotFoundException e) {
            log.error(Constant.EXCEPTION_LOG_TITLE, e);
            throw new BaseException(ErrorCode.SYS_FILE_NOT_FOUND);
        } catch (IOException e) {
            log.error(Constant.EXCEPTION_LOG_TITLE, e);
            throw new BaseException(ErrorCode.SYS_IO_EXCEPTION);
        }
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

    private String getToken() throws IOException {
        String appKey = extraConfig.getBaiduAppKey();
        String secretKey = extraConfig.getBaiduSecretKey();
        TokenHolder holder = new TokenHolder(appKey, secretKey, TokenHolder.ASR_SCOPE);
        try {
            holder.refresh();
        } catch (DemoException e) {
            log.error(Constant.EXCEPTION_LOG_TITLE, e);
            throw new BaseException("DemoException...");
        }
        return holder.getToken();
    }

    private String getHeaderValue(String content) throws UnsupportedEncodingException {
        return "attachment;filename=" + URLEncoder.encode(content, DEFAULT_CHARSET);
    }

    private InputStream getSeqAudioIS(String[] textArray) throws DemoException, IOException {
        String appKey = extraConfig.getBaiduAppKey();
        String secretKey = extraConfig.getBaiduSecretKey();
        TokenHolder holder = new TokenHolder(appKey, secretKey, TokenHolder.ASR_SCOPE);
        holder.refresh();
        String token = holder.getToken();
        BaiduTtsParam param = new BaiduTtsParam();
        param.setTok(token);
        param.setPer(5003);
        param.setSpd(5);
        param.setPit(5);
        param.setVol(5);
        param.setCuid("JAVA12345678");
        param.setAue(3);

        return getSeqAudioIS(textArray, param);
    }

    private InputStream getSeqAudioIS(String[] textArray, BaiduTtsParam paramDTO) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String text : textArray) {
            if (!StringUtils.hasText(text)) {
                continue;
            }
            sb.append(text);
        }
        if (sb.length() < 1) {
            throw new IllegalArgumentException("No text!");
        }
        paramDTO.setTex(sb.toString());
        String params = HttpUtils.baiduParamToString(paramDTO);

        HttpPost httpPost = new HttpPost(API_URL);
        StringEntity entity = new StringEntity(params, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            CloseableHttpResponse closeableHttpResponse = httpClient.execute(httpPost);
            InputStream responseContent = closeableHttpResponse.getEntity().getContent();

            String contentType = closeableHttpResponse.getEntity().getContentType().getValue();
            if (contentType.contains("audio/")) {
                File tempFile = new File(extraConfig.getOutputFolder() + "temp.mp3");
                FileUtils.copyInputStreamToFile(responseContent, tempFile);
                return responseContent;
            } else {
                log.error("ERROR: content-type= {}", contentType);
                byte[] result = ConnUtil.getInputStreamContent(responseContent);
                log.error(new String(result));
                throw new BaseException(ErrorCode.SYS_INTERNAL_ERROR);
            }
        }
    }
}
