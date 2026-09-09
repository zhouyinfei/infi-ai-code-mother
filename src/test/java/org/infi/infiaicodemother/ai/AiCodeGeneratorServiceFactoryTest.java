package org.infi.infiaicodemother.ai;

import jakarta.annotation.Resource;
import org.infi.infiaicodemother.ai.model.HtmlCodeResult;
import org.infi.infiaicodemother.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
//class AiCodeGeneratorServiceTest {
//
//    @Resource
//    private AiCodeGeneratorService aiCodeGeneratorService;
//
//    @Test
//    void generateHtmlCode() {
//        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员鱼皮的工作记录小工具, 不超过20行");
//        Assertions.assertNotNull(result);
//    }
//
//    @Test
//    void generateMultiFileCode() {
//        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个程序员鱼皮的留言板, 不超过50行");
//        Assertions.assertNotNull(multiFileCode);
//    }
//}
