package com.example.ogani.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
    
      return new OpenAPI()
        .info(new Info().title("OGANI REST API DOCUMENT")
        .contact(new Contact().name("Tô Văn Huy").email("tovanhuy2003@gmail.com").url("https://www.facebook.com/profile.php?id=100021843918926"))
        .license(new License().name("Apache 2.0")
        .url("http://springdoc.org")));
    }    
}
