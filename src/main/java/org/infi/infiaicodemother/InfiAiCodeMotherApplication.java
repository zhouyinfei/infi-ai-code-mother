package org.infi.infiaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("org.infi.infiaicodemother.mapper")
public class InfiAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfiAiCodeMotherApplication.class, args);
    }

}
