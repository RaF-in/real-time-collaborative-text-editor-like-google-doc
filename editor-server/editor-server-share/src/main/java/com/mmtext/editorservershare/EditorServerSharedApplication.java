package com.mmtext.editorservershare;

import com.mmtext.editorservershare.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = {
        net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class
})
@EnableConfigurationProperties(CorsProperties.class)
public class EditorServerSharedApplication {

    public static void main(String[] args) {
        SpringApplication.run(EditorServerSharedApplication.class, args);
    }

}
