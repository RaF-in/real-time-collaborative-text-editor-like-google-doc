package com.mmtext.editorservershare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class
})
public class EditorServerSharedApplication {

    public static void main(String[] args) {
        SpringApplication.run(EditorServerSharedApplication.class, args);
    }

}
