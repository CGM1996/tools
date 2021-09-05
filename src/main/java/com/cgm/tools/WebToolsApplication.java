package com.cgm.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @author cgm
 */
@SpringBootApplication
@MapperScan("com.cgm.tools.*.mapper")
public class WebToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebToolsApplication.class, args);
    }

}
