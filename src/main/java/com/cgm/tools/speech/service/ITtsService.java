package com.cgm.tools.speech.service;

import com.cgm.tools.speech.common.DemoException;
import com.cgm.tools.speech.dto.BaiduTtsParam;
import com.cgm.tools.speech.subtitleFile.FatalParsingException;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ITtsService {
    void textTts(HttpServletResponse response, BaiduTtsParam param) throws DemoException, IOException;

    void fileTts(HttpServletResponse response, MultipartFile file, BaiduTtsParam param) throws IOException, FatalParsingException, DemoException;
}
