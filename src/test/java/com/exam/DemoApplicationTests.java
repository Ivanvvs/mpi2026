package com.exam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ExamApplication.class,
        properties = {
                "debug=false",
                "logging.level.root=warn",
                "logging.level.com.exam=info",
                "logging.level.org.springframework=warn"
        }
)
@ActiveProfiles("test")
class DemoApplicationTests {

    private final ApplicationContext applicationContext;

    DemoApplicationTests(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }
}
