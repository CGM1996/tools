package com.cgm.tools.speech.controller;

import com.cgm.tools.speech.common.DemoException;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import com.cgm.tools.speech.dto.VideoAddSpeechParam;
import com.cgm.tools.speech.service.ITtsService;
import com.cgm.tools.speech.subtitleFile.FatalParsingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author cgm
 */
@CrossOrigin
@RestController
@Api(tags = "文字转语音")
@RequestMapping("/api/tts")
public class TtsController {
    private final ITtsService ttsService;

    @Autowired
    public TtsController(ITtsService ttsService) {
        this.ttsService = ttsService;
    }

    @ApiOperation("文本转语音")
    @PostMapping("/text")
    public void textTts(HttpServletResponse response, @ApiParam(value = "参数") @RequestBody BaiduTtsParam param)
            throws DemoException, IOException {
        ttsService.textTts(response, param);
    }

    @ApiOperation("文件转语音")
    @PostMapping("/file")
    public void fileTts(HttpServletResponse response,
                                @ApiParam(value = "字幕文件（SRT,STL,SCC,XML,ASS）") MultipartFile file,
                                @ApiParam(value = "参数") BaiduTtsParam param)
            throws FatalParsingException, DemoException, IOException {
        ttsService.fileTts(response, file, param);
    }

    @ApiOperation("文件转语音")
    @PostMapping("/video-add-speech")
    public void videoAddSpeech(HttpServletResponse response,
                               @ApiParam(value = "视频文件") MultipartFile video,
                        @ApiParam(value = "字幕文件（SRT,STL,SCC,XML,ASS）") MultipartFile captions,
                        @ApiParam(value = "参数") VideoAddSpeechParam param)
            throws IOException {
        ttsService.videoAddSpeech(response, video, captions, param);
    }
}
