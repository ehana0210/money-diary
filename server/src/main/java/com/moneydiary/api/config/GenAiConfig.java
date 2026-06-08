package com.moneydiary.api.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Gen AI SDK 클라이언트(Vertex AI 백엔드) 설정.
 *
 * <p>API 키 대신 Application Default Credentials(로컬: gcloud, Cloud Run: 런타임 서비스 계정)를
 * 사용하므로 키를 코드/환경변수에 둘 필요가 없다. 런타임 SA 에는 {@code roles/aiplatform.user} 가 필요하다.
 */
@Configuration
public class GenAiConfig {

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.vertex.location:global}")
    private String location;

    @Bean(destroyMethod = "close")
    public Client genAiClient() {
        return Client.builder()
                .project(projectId)
                .location(location)
                .vertexAI(true)
                .httpOptions(HttpOptions.builder().apiVersion("v1").build())
                .build();
    }
}
