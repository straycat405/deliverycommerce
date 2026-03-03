package com.babjo.deliverycommerce.product.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDescriptionServiceImpl implements AiDescriptionService {

    private final Client geminiClient;

    @Override
    public String generateProductDescription(String productName, String point) {

        String prompt = """
                당신은 배달앱 음식 메뉴 소개를 작성하는 마케터입니다.
                
                [메뉴 이름]
                %s
                
                [메뉴 특징]
                %s
                
                위 정보를 기반으로 배달앱에 등록할 음식 설명을 작성하세요.
                
                조건:
                - 답변을 최대한 간결하게 50자 이하로
                - 먹고 싶어지게 작성
                - 과장 광고 금지
                - 이모지 사용 금지
                """.formatted(productName, point);

        GenerateContentResponse response = geminiClient.models.generateContent(
                "gemini-3-flash-preview",
                prompt,
                null
        );

        String result = response.text();

        if(result == null || result.isBlank()) {
            throw new RuntimeException("AI 설명 생성 실패");
        }

        return result.trim();
    }
}
