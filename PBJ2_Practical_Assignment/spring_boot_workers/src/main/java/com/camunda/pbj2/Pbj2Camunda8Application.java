package com.camunda.pbj2;

import io.camunda.zeebe.spring.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PBJ-2 Practical Assignment: Camunda 8 External Job Workers & Mock REST Microservice.
 * Hosts Zeebe External Job Workers and Mock REST APIs for Labs 1, 2, and 3.
 */
@SpringBootApplication
public class Pbj2Camunda8Application {

    public static void main(String[] args) {
        SpringApplication.run(Pbj2Camunda8Application.class, args);
        System.out.println("=================================================================");
        System.out.println("  PBJ-2 Camunda 8 External Workers & Mock APIs Started (Port 8080)");
        System.out.println("  Endpoints available:");
        System.out.println("    POST /api/credit/assess");
        System.out.println("    POST /api/payment/process");
        System.out.println("    POST /api/hospital/scheduling/available-slots");
        System.out.println("=================================================================");
    }
}
