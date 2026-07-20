package com.prj.manualrag.rag.service;

import com.prj.manualrag.document.entity.DocumentEntity;
import com.prj.manualrag.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentSelectorService {
    private final DocumentRepository documentRepository;
    private final ChatClient chatClient;

    public List<String> select(String question) {
        List<DocumentEntity> docs = documentRepository.findAll();
        StringBuilder sb = new StringBuilder();

        for (DocumentEntity doc : docs) {
            sb.append("""
                    파일명: %s
                    제목: %s
                    메타데이터: %s

                    """
                    .formatted(
                            doc.getFileName(),
                            doc.getTitle(),
                            doc.getMetadata()
                    ));
        }

        String prompt = """
                당신은 업로드된 문서 중에서
                검색해야 할 문서를 고르는 역할만 수행한다.

                아래는 업로드된 문서 목록이다.

                %s

                사용자 질문

                %s

                규칙

                1. 질문과 관련있는 문서만 선택한다.
                2. 관련성이 낮으면 선택하지 않는다.
                3. 확실하지 않으면 선택하지 않는다.
                4. 최대 5개까지 선택한다.
                5. 파일명만 JSON 배열로 출력한다.
                6. 설명하지 않는다.

                예시

                ["바퀴벌레.pdf"]

                또는

                ["자동차.pdf","엔진.pdf"]

                또는

                []

                """
                .formatted(sb, question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(new ParameterizedTypeReference<List<String>>() {});
    }

}
