package com.cgm.tools.speech.controller;

import com.cgm.tools.base.ResponseData;
import com.cgm.tools.speech.common.DemoException;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import com.cgm.tools.speech.service.ITtsService;
import com.cgm.tools.speech.subtitleFile.FatalParsingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author cgm
 */
@CrossOrigin
@RestController
@Api(tags = "文字转语音")
@RequestMapping("/api/public/tts")
public class TtsController {
    @Resource
    private ITtsService ttsService;

    @ApiOperation("文本转语音")
    @PostMapping("/text")
    public ResponseData textTts(HttpServletResponse response, @ApiParam(value = "参数") @RequestBody BaiduTtsParam param)
            throws DemoException, IOException {
        ttsService.textTts(response, param);
        return new ResponseData();
    }

    @ApiOperation("文件转语音")
    @PostMapping("/file")
    public ResponseData fileTts(HttpServletResponse response, @ApiParam(value = "语料文件") MultipartFile file,
            @ApiParam(value = "参数") BaiduTtsParam param) throws FatalParsingException, DemoException, IOException {
        ttsService.fileTts(response, file, param);
        return new ResponseData();
    }
}
