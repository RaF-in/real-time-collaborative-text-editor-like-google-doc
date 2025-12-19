package com.mmtext.editorserversnapshot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class
})
public class EditorServerSnapshotApplication {

    public static void main(String[] args) {
        SpringApplication.run(EditorServerSnapshotApplication.class, args);
    }

}
