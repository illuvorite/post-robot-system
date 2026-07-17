package com.lu.postrobotsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lu.postrobotsystem.mapper")
public class PostRobotSystemApplication {


    public static void main(String[] args) {
        SpringApplication.run(PostRobotSystemApplication.class, args);
    }

}
