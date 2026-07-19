package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.domain.Intent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {
    private final ChatClient chatClient;
    public Intent classify(String question){
        String result =
                chatClient
                        .prompt()
                        .system("""
                                사용자의 질문 의도를 분류한다.

                                DOCUMENT:
                                업로드된 문서,
                                제품 설명서,
                                규정,
                                계약서,
                                PDF,
                                사내 자료와 관련된 질문

                                WEB:
                                최신 정보,
                                현재 가격,
                                뉴스,
                                오늘 날씨,
                                최근 사건,
                                인터넷 정보가 필요한 질문

                                GENERAL:
                                일반 상식,
                                일상 질문

                                반드시 DOCUMENT, WEB, GENERAL 중 하나만 출력한다.
                                """)
                        .user(question)
                        .call()
                        .content();

        log.info("intent result={}", result);

        if(result.contains("DOCUMENT")){
            return Intent.DOCUMENT;
        } else if(result.contains("WEB")){
            return Intent.WEB;
        }
        return Intent.GENERAL;
    }
}