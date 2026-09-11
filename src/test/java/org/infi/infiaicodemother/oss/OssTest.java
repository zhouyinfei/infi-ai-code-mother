package org.infi.infiaicodemother.oss;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class OssTest {

    @Test
    public void testOss() {
//        // 从环境变量中加载凭证信息，用于身份验证
//        CredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
//
//        // 填写Bucket所在地域。
//        String region = "<region-id>";
//
//        // 请填写您的自定义域名。例如www.example-***.com
//        String endpoint = "https://www.example-***.com";
//
//        // 使用配置好的信息创建OSS客户端
//        try (OSSClient client = OSSClient.newBuilder()
//                .credentialsProvider(credentialsProvider)
//                .region(region)
//                .endpoint(endpoint)
//                // 请注意，设置true开启CNAME选项，否则无法使用自定义域名
//                .useCName(true)
//                .build()) {
//            // 使用创建好的client执行后续操作...
//            client
//        } catch (Exception e) {
//            System.err.println("Error occurred: " + e.getMessage());
//        }
    }
}
