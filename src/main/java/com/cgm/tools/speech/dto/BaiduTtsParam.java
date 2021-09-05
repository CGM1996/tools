package com.cgm.tools.speech.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BaiduTtsParam {

    @ApiModelProperty(value = "语言选择,目前只有中英文混合模式，填写固定值zh", hidden = true)
    private final String lan = "ch";

    @ApiModelProperty(value = "客户端类型选择，web端填写固定值1", hidden = true)
    private final int ctp = 1;

    @ApiModelProperty(value = "文本，使用文件时不需要填写", example = "欢迎使用百度语音")
    private String tex;

    @ApiModelProperty(value = "token", hidden = true)
    private String tok;

    @ApiModelProperty(value = "发音人选择，度小宇=1，度小美=0，度逍遥（基础）=3，度丫丫=4，度逍遥（精品）=5003", example = "5003")
    private int per;

    @ApiModelProperty(value = "语速，取值0-15，默认为5中语速", example = "5")
    private int spd;

    @ApiModelProperty(value = "音调，取值0-15，默认为5中语调", example = "5")
    private int pit;

    @ApiModelProperty(value = "音量，取值0-9，默认为5中音量", example = "5")
    private int vol;

    @ApiModelProperty(value = "下载的文件格式, 3：mp3(默认)|4： pcm-16k|5： pcm-8k|6. wav", example = "3")
    private int aue;

    @ApiModelProperty(value = "用户唯一标识，用来计算UV值", example = "1234567JAVA")
    private String cuid;

    @ApiModelProperty(value = "部分，一段最长2000字，超长需要分段处理", example = "1")
    private int part;
}
