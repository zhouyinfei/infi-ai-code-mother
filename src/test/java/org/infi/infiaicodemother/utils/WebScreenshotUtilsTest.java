package org.infi.infiaicodemother.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String url = "https://www.baidu.com";
        String screenshot = WebScreenshotUtils.saveWebPageScreenshot(url);
        log.info("截图结果：{}", screenshot);
        assertNotNull(screenshot);
    }
}