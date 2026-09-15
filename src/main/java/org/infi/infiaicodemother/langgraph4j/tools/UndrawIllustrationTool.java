package org.infi.infiaicodemother.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.infi.infiaicodemother.langgraph4j.model.ImageCategoryEnum;
import org.infi.infiaicodemother.langgraph4j.model.ImageResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String UNDRAW_HOME_URL = "https://undraw.co";
    private static final String UNDRAW_API_URL = "%s/_next/data/%s/search/%s.json?term=%s";
    private static final Pattern BUILD_ID_PATTERN = Pattern.compile("\"buildId\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"");

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        // undraw.co 每次重新部署都会更换 Next.js buildId，硬编码会 404，需动态获取
        String buildId = fetchBuildId();
        if (StrUtil.isBlank(buildId)) {
            log.warn("获取 undraw.co 当前 buildId 失败，跳过搜索");
            return imageList;
        }
        String apiUrl = String.format(UNDRAW_API_URL, UNDRAW_HOME_URL, buildId, query, query);

        // 使用 try-with-resources 自动释放 HTTP 资源
        try (HttpResponse response = HttpRequest.get(apiUrl).timeout(10000).execute()) {
            if (!response.isOk()) {
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONObject pageProps = result.getJSONObject("pageProps");
            if (pageProps == null) {
                return imageList;
            }
            JSONArray initialResults = pageProps.getJSONArray("initialResults");
            if (initialResults == null || initialResults.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, initialResults.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject illustration = initialResults.getJSONObject(i);
                String title = illustration.getStr("title", "插画");
                String media = illustration.getStr("media", "");
                if (StrUtil.isNotBlank(media)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(title)
                            .url(media)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }
        return imageList;
    }

    /**
     * 动态获取 undraw.co 当前部署的 Next.js buildId，避免硬编码失效
     */
    private String fetchBuildId() {
        try (HttpResponse response = HttpRequest.get(UNDRAW_HOME_URL).timeout(10000).execute()) {
            if (!response.isOk()) {
                return null;
            }
            Matcher matcher = BUILD_ID_PATTERN.matcher(response.body());
            return matcher.find() ? matcher.group(1) : null;
        } catch (Exception e) {
            log.warn("获取 undraw.co buildId 失败：{}", e.getMessage());
            return null;
        }
    }
}
