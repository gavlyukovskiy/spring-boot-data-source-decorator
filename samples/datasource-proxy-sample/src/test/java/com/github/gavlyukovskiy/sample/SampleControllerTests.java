package com.github.gavlyukovskiy.sample;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleControllerTests {

    @Value("http://localhost:${local.server.port}")
    private String localhost;

    private RestTemplate restTemplate = new RestTemplate();

    @Test
    public void select() {
        restTemplate.getForEntity(localhost + "/commit", String.class);
    }

    @Test
    public void rollback() {
        restTemplate.getForEntity(localhost + "/rollback", String.class);
    }

    @Test
    public void error() {
        restTemplate.getForEntity(localhost + "/query-error", String.class);
    }
}