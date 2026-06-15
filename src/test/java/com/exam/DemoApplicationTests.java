package com.exam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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

    @Test
    void contextLoads() {
    }
}
