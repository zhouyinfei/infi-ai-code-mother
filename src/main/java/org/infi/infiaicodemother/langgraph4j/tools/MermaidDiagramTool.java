package org.infi.infiaicodemother.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.infi.infiaicodemother.exception.BusinessException;
import org.infi.infiaicodemother.exception.ErrorCode;
import org.infi.infiaicodemother.langgraph4j.model.ImageCategoryEnum;
import org.infi.infiaicodemother.langgraph4j.model.ImageResource;
import org.infi.infiaicodemother.manager.CosManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private CosManager cosManager;

    @Tool("将 Mermaid 代码转换为架构图图片，用于展示系统结构和技术关系")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid 图表代码") String mermaidCode,
                                                      @P("架构图描述") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        try {
            // 转换为SVG图片
            File diagramFile = convertMermaidToSvg(mermaidCode);
            // 上传到COS
            String keyName = String.format("/mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String cosUrl = cosManager.uploadFile(keyName, diagramFile);
            // 清理临时文件
            FileUtil.del(diagramFile);
            if (StrUtil.isNotBlank(cosUrl)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(cosUrl)
                        .build());
            }
        } catch (Exception e) {
            log.error("生成架构图失败: {}", e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * 将Mermaid代码转换为SVG图片
     */
    private File convertMermaidToSvg(String mermaidCode) throws IOException, InterruptedException {
        // 创建临时输入文件
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        // 创建临时输出文件
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        try {
            FileUtil.writeUtf8String(mermaidCode, tempInputFile);
            // 根据操作系统选择命令
            String command = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
            // 第一次尝试：使用 mermaid-cli 默认浏览器（Chrome for Testing，由 puppeteer 自动管理）
            String errorOutput = execMmdc(command, null, tempInputFile, tempOutputFile);
            if (!isValidOutput(tempOutputFile)) {
                // 回退：使用系统已安装的 Chrome / Edge，避免依赖 puppeteer 下载的浏览器内核
                String browserPath = findSystemBrowser();
                if (browserPath != null) {
                    log.info("mermaid-cli 默认浏览器渲染失败，回退到系统浏览器: {}", browserPath);
                    FileUtil.del(tempOutputFile);
                    File puppeteerConfigFile = writePuppeteerConfig(browserPath);
                    errorOutput = execMmdc(command, puppeteerConfigFile, tempInputFile, tempOutputFile);
                    FileUtil.del(puppeteerConfigFile);
                }
            }
            if (!isValidOutput(tempOutputFile)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, StrUtil.format(
                        "Mermaid CLI 渲染失败，请确认已安装 mermaid-cli (npm install -g @mermaid-js/mermaid-cli)"
                                + "且系统中存在 Chrome 或 Edge 浏览器。命令输出: {}",
                        StrUtil.maxLength(errorOutput, 500)));
            }
            // 清理输入文件，保留输出文件供上传使用
            FileUtil.del(tempInputFile);
            return tempOutputFile;
        } catch (Exception e) {
            FileUtil.del(tempInputFile);
            FileUtil.del(tempOutputFile);
            throw e;
        }
    }

    /**
     * 执行 mmdc 命令，返回合并后的命令输出
     */
    private String execMmdc(String command, File puppeteerConfigFile, File tempInputFile, File tempOutputFile)
            throws IOException, InterruptedException {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(command);
        if (puppeteerConfigFile != null) {
            cmdLine.add("-p");
            cmdLine.add(puppeteerConfigFile.getAbsolutePath());
        }
        cmdLine.add("-i");
        cmdLine.add(tempInputFile.getAbsolutePath());
        cmdLine.add("-o");
        cmdLine.add(tempOutputFile.getAbsolutePath());
        cmdLine.add("-b");
        cmdLine.add("transparent");
        ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
        // 合并标准输出与错误输出，便于定位失败原因
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return output;
    }

    private boolean isValidOutput(File file) {
        return file.exists() && file.length() > 0;
    }

    /**
     * 查找系统中已安装的 Chrome / Edge 浏览器可执行文件
     */
    private String findSystemBrowser() {
        String[] candidates;
        if (SystemUtil.getOsInfo().isWindows()) {
            candidates = new String[]{
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
            };
        } else if (SystemUtil.getOsInfo().isMac()) {
            candidates = new String[]{
                    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
            };
        } else {
            candidates = new String[]{
                    "/usr/bin/google-chrome",
                    "/usr/bin/chromium",
                    "/usr/bin/chromium-browser"
            };
        }
        for (String candidate : candidates) {
            if (FileUtil.exist(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 生成 puppeteer 配置文件，指定使用系统浏览器渲染
     */
    private File writePuppeteerConfig(String browserPath) {
        File configFile = FileUtil.createTempFile("puppeteer_config_", ".json", true);
        String escapedPath = browserPath.replace("\\", "\\\\");
        String json = "{\"executablePath\":\"" + escapedPath + "\",\"args\":[\"--no-sandbox\"]}";
        FileUtil.writeUtf8String(json, configFile);
        return configFile;
    }
}
