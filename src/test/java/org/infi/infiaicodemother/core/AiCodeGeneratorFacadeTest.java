package org.infi.infiaicodemother.core;

import jakarta.annotation.Resource;
import org.infi.infiaicodemother.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("生成一个登录页面, 不超过20行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }
}