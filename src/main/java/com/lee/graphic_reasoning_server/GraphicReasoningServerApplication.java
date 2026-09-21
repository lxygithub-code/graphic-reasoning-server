package com.lee.graphic_reasoning_server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lee.graphic_reasoning_server.mapper")
public class GraphicReasoningServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GraphicReasoningServerApplication.class, args);
	}

}
