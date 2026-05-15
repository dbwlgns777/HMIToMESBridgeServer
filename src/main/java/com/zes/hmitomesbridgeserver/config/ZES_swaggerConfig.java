package com.zes.hmitomesbridgeserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZES_swaggerConfig
{
    @Bean
    public OpenAPI ZES_openApi()
    {
        return new OpenAPI()
                .info(new Info()
                        .title("HMI To MES Bridge Server API")
                        .description("HMI to MES bridge API 문서")
                        .version("v1.0.0"));
    }
}
