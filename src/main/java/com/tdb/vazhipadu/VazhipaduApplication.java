package com.tdb.vazhipadu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VazhipaduApplication {

    public static void main(String[] args) {
        SpringApplication.run(VazhipaduApplication.class, args);
    }
}
