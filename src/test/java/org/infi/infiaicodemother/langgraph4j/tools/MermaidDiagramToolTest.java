package org.infi.infiaicodemother.langgraph4j.tools;

import jakarta.annotation.Resource;
import org.infi.infiaicodemother.langgraph4j.model.ImageCategoryEnum;
import org.infi.infiaicodemother.langgraph4j.model.ImageResource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MermaidDiagramToolTest {

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Test
    void testGenerateMermaidDiagram() {
        // 测试生成 Mermaid 架构图
        String mermaidCode = """
                flowchart LR
                    Start([开始]) --> Input[输入数据]
                    Input --> Process[处理数据]
                    Process --> Decision{是否有效?}
                    Decision -->|是| Output[输出结果]
                    Decision -->|否| Error[错误处理]
                    Output --> End([结束])
                    Error --> End
                """;
        String description = "简单系统架构图";
        List<ImageResource> diagrams = mermaidDiagramTool.generateMermaidDiagram(mermaidCode, description);
        assertNotNull(diagrams);
        // 明确校验生成结果，避免空列表直接越界导致报错信息不清晰
        assertFalse(diagrams.isEmpty(),
                "架构图生成失败（未返回图片），请确认：1) 已安装 mermaid-cli (npm install -g @mermaid-js/mermaid-cli)；"
                        + "2) COS 配置正确可上传文件");
        // 如果有结果，验证图表资源
        ImageResource firstDiagram = diagrams.get(0);
        assertEquals(ImageCategoryEnum.ARCHITECTURE, firstDiagram.getCategory());
        assertEquals(description, firstDiagram.getDescription());
        assertNotNull(firstDiagram.getUrl());
        assertTrue(firstDiagram.getUrl().startsWith("http"));
        System.out.println("生成了架构图: " + firstDiagram.getUrl());
    }
}
