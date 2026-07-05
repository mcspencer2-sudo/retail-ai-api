package com.retailai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebStaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /*
         * Serve normal frontend static files:
         *
         * src/main/resources/static/css/...
         * src/main/resources/static/js/...
         * src/main/resources/static/images/...
         * src/main/resources/static/*.html
         */

        registry
                .addResourceHandler(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/*.html",
                        "/favicon.ico",
                        "/retailers.js"
                )
                .addResourceLocations(
                        "classpath:/static/css/",
                        "classpath:/static/js/",
                        "classpath:/static/images/",
                        "classpath:/META-INF/resources/webjars/",
                        "classpath:/static/"
                )
                .setCacheControl(
                        CacheControl
                                .maxAge(0, TimeUnit.SECONDS)
                                .cachePrivate()
                                .mustRevalidate()
                );

        /*
         * Fallback for any other static resource under /static.
         * This helps mirror.html, landing.html, index.html, and root static files.
         */
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(
                        CacheControl
                                .maxAge(0, TimeUnit.SECONDS)
                                .cachePrivate()
                                .mustRevalidate()
                );
    }
}