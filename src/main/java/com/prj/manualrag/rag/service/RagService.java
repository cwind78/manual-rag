package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.dto.QuestionResponse;

import lombok.RequiredArgsConstructor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.stereotype.Service;


import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {


    private final VectorStore vectorStore;

    private final ChatClient chatClient;
//    private QuestionResponse questionResponse;


    public QuestionResponse answer(
            String question
    ){


        /*
         * 1. Vector 검색
         */
        List<Document> documents =
                vectorStore.similaritySearch(
                        SearchRequest
                                .builder()
                                .query(question)
                                .topK(5)
                                .build()
                );



        /*
         * 2. 검색 결과 Context 생성
         */
        String context =
                documents.stream()
                        .map(Document::getText)
                        .reduce(
                                "",
                                (a,b) ->
                                        a + "\n\n" + b
                        );



        /*
         * 3. LLM Prompt 생성
         */
        String prompt =
                """
                당신은 제품 사용 설명서 안내 AI입니다.

                반드시 아래 설명서 내용만 이용해서 답변하세요.

                설명서 내용:
                %s


                사용자 질문:
                %s


                모르는 내용이면
                "설명서에서 확인할 수 없습니다."
                라고 답하세요.
                """
                        .formatted(
                                context,
                                question
                        );



        /*
         * 4. LLM 호출
         */
        String answer =
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();



        return new QuestionResponse(answer);
    }
}
