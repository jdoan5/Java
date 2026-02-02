package com.johndoan.helpdesk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI helpdeskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Helpdesk Ticket Triage API")
                        .version("v1")
                        .description("REST API for creating, viewing, updating, and deleting helpdesk tickets."));
    }
}