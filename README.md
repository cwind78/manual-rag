프로젝트 루트에서:

docker compose up -d

정상:

Container rag-postgres Started

확인:

docker ps

결과 예:

CONTAINER ID   IMAGE
xxxx           pgvector/pgvector:pg16
아래는 실제 ps 했을때 출력
CONTAINER ID   IMAGE                    COMMAND                   CREATED          STATUS          PORTS                    NAMES
46b14c5c10a0   pgvector/pgvector:pg16   "docker-entrypoint.s…"   13 seconds ago   Up 11 seconds   0.0.0.0:5432->5432/tcp   rag-postgres

6. pgvector 활성화 확인

PostgreSQL 접속:

docker exec -it rag-postgres psql -U raguser -d ragdb

접속되면(쿼리실행할때도):접속 되면 바로 sql명령을 날릴 수 있다.
\l 접속한 데이터베이스
\dt 테이블 목록


CREATE EXTENSION vector;

실행.

확인:

\dx

결과:

vector | 0.7.x | vector data type

이면 완료입니다.

TRUNCATE TABLE vector_store RESTART IDENTITY;
테이블 초기화
TRUNCATE TABLE mcp_tools, mcp_servers RESTART IDENTITY;
포린키 때문에 안될때 같이 트렁케이트

종료:

\q

ollama 윈도우용 설치 파일 다운로드
https://ollama.com/download?utm_source=chatgpt.com

설치 완료 후 cmd창을 다시 열어서
ollama --version 하여 나오면 설치 성공

ollama list
아무것도 안 찍힘

llm 모델 다운로드
ollama pull qwen2.5:7b

ollama list
다운로드 된 qwen2.5:7b 찍힘

테스트 실행
ollama run qwen2.5:7b

냉장고 문이 잘 닫히지 않는 원인은?
답변이 나오면 성공

종료
ctrl+d
or /bye

임베딩 모델 다운로드
ollama pull nomic-embed-text
ollama list
결과 아래 2개가 보임
qwen2.5:7b
nomic-embed-text

임베딩 테스트(대화 모델이 아니기 때문에 챗팅 할 수 없음)
ollama run nomic-embed-text

파워쉘에서
curl http://localhost:11434/api/embeddings `
-d '{"model":"nomic-embed-text","prompt":"냉장고 필터 청소 방법"}'
위 명령어는 안된다고 하네 아래 명령으로 실행 하라고 하네

Invoke-RestMethod `
  -Uri "http://localhost:11434/api/embeddings" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"model":"nomic-embed-text","prompt":"냉장고 필터 청소 방법"}'

{
  "embedding":[
     0.123,
     -0.234,
     ...
  ]
}
이면 성공
한글이 깨져서
$body = @{
    question = "uv 청정탈취필터는 어떤 기능인가"
    productName   = "냉장고"
    manufacturer    = "엘지전자"
    modelName     = "RNL12NO1231"
} | ConvertTo-Json

$bytes = [System.Text.Encoding]::UTF8.GetBytes($body)

Invoke-RestMethod `
   -Uri "http://localhost:8080/api/rag/question" `
   -Method Post `
   -ContentType "application/json; charset=utf-8" `
   -Body $bytes

스웨거 확인 주소
http://localhost:8080/swagger-ui.html
(http://localhost:8080/swagger-ui/index.html)
둘다 되는 것으로 확인. 포스트 맨 없이 여기서 테스트 가능

올라마 로컬에 설치 했는데 도커로 돌림
먼제 로컬에 실행중인 올라마 종료: 숨겨진 아이콘에 라마 이모티콘 우클릭 종료
컴포즈 파일에 올라마 컨테이너 설정 추가하고 컴포즈 파일이 있는 위치에서 cmd열어 아래 실행
docker compose up -d
docker ps
아래와 비슷하게 나오면 정상
rag-postgres
rag-ollama
실제로는
CONTAINER ID   IMAGE                    COMMAND                   CREATED          STATUS          PORTS                      NAMES
60eb959bfd91   ollama/ollama:latest     "/bin/ollama serve"       11 seconds ago   Up 10 seconds   0.0.0.0:11434->11434/tcp   rag-ollama
46b14c5c10a0   pgvector/pgvector:pg16   "docker-entrypoint.s…"   3 days ago       Up 3 days       0.0.0.0:5432->5432/tcp     rag-postgres

모델 다시 다운로드
docker exec -it rag-ollama ollama pull qwen2.5:7b
docker exec -it rag-ollama ollama pull nomic-embed-text

확인
docker exec -it rag-ollama ollama list
NAME                       ID              SIZE      MODIFIED       
nomic-embed-text:latest    0a109f422b47    274 MB    9 seconds ago
qwen2.5:7b                 845dbda0ea48    4.7 GB    54 seconds ago

깃헙에 붇기
git status
git add .
git commit -m "변경 내용"
git push origin main

vue 프로젝트 생성
프로젝트 루트로 이동
PS C:\workspace\manual-rag>npm create vite@latest frontend
설치 하는 중 아래 프람프트 나오면 선택
Framework:
Vue
Variant:
JavaScript
그 외 다른건 읽어보고 처리

cd frontend
npm install
npm install -D tailwindcss @tailwindcss/vite
npm install lucide-vue-next
npm install axios
npm run dev

searxng를 뒤늦게 추가해서 추가로 설치할때 아래 처럼(처음 환경 구성할때는 이것 필요 없음)
docker compose up -d searxng
docker ps
searxng 컨테이너 up 되어 있는지 확인
http://localhost:8081/search?q=냉장고&format=json
해보래서 했는데 403. 설정 문제라고 함
브라우저에서
http://localhost:8081/search
했더니 검색화면 나오고 오늘날짜라고 검색했더니 이것저것 검색 결과 보여줌
api 활성화
docker exec -it rag-searxng sh
ls /etc/searxng
settings.yml
파일내용이
search:
  formats:
    - html
이렇게 되어 있으면
    - json
을 추가하라는데 저런 내용이 없다. 제일 아래에 전체 추가해 준다
search:
  formats:
    - html
    - json

다했다면 빠져 나와서
docker restart rag-searxng

컴퓨터 재기동 했더니 도커가 꺼져 있어서 도커 데스크탑을 다시 실행 했는데 컨테이너가 같이 올라왔다.

맥북에서 임베딩 시멘틱 검색할때 메모리 부족하다는 메시지 나와서 도커 메모리 확인 했더니
podman machine inspect
MEMORY: 2048로 되어 있어서

podman machine stop
podman machine set --memory 8192
podman machine start
podman compose up -d

실행

podman logs rag-ollama --tail 100

2.5:7b 모델은 mcp를 잘 선택하지 않는 경향이 있다고 해서 2.5:14b를 설치하고 설정한다.
docker exec -it rag-ollama ollama run qwen2.5:14b
메모리 부족해서 삭제
docker exec -it rag-ollama ollama rm qwen2.5:14b

벡터 db에서 capability 조회
SELECT
id,
content,
metadata
FROM vector_store
WHERE metadata ->> 'capability' = 'true';

문서 Capability만 조회:

SELECT
id,
content,
metadata
FROM vector_store
WHERE metadata ->> 'capabilityType' = 'DOCUMENT';

MCP Capability만 조회:

SELECT
id,
content,
metadata
FROM vector_store
WHERE metadata ->> 'capabilityType' = 'MCP_TOOL';

searXNG 검색 결과 이상
컨테이너 로그 확인
docker logs rag-searxng
settings.yml의 설정이 없어서라고 함
docker exec -it rag-searxng sh
위치 확인
ls /etc/searxng
vi /etc/searxng/settings.yml
use_default_settings: false <-- true에서 false로 바꿈

server:
secret_key: "..."
image_proxy: true

search:
formats:
- html
- json

engines: <-- 여기부터 추가
- name: google
  engine: google
  disabled: false

- name: duckduckgo
  engine: duckduckgo
  disabled: false

바꾸고 컨테이너 나와서 리스타트
docker restart rag-searxng
접속이 안되어
docker logs rag-searxng --tail=100
아래 로그
[INFO] Starting granian (main PID: 1) [INFO] Listening at: http://:::8080 [INFO] Spawning worker-1 with PID: 844 2026-08-03 02:06:47,457 ERROR:searx.engines: Engine config error: ambiguous shortcut: - [ERROR] Unexpected exit from worker-1 [INFO] Shutting down granian [INFO] Stopped worker-1 [INFO] Granian shutdown completed, see ya! SearXNG 2026.7.19-6da6eee26 Updating certificates in /etc/ssl/certs... 0 added, 0 removed; done. Running hooks in /etc/ca-certificates/update.d... done.

docker compose stop rag-searxng
docker volume rm searxng-data
docker compose up -d rag-searxng
팟맨 컴포즈로 관리되고 있어 볼륨이 삭제 안됨
docker volume ls
볼륨 이름이 manual-rag_searxng-data로 확인 됨
~~docker volume rm manual-rag_searxng-data~~
podman rm -f rag-searxng
podman volume rm manual-rag_searxng-data
podman-compose up -d
podman ps
정상적으로 컨테이너가 실행 되었지만 api는 차단 되어 있어 윗쪽의 설정 방법을 참고하여 api활성화

searxng 컨테이너 지우고 다시 생성하는 범
podman rm -f rag-searxng
podman-compose config --service
podman-compose up -d searxng
podman ps -a
//podman logs -f rag-searxng
searxng 설정 파일만 수정 하기 위해 설정 파일이 보이는지 확인
podman run --rm \
-v manual-rag_searxng-data:/data \
alpine \
ls -la /data
새로운 키 생성
openssl rand -hex 32
63093bc306e3086df38aa034c5326a4d6aac990752c2a3de2ac7e9199dbde203
podman run --rm -it \
-v manual-rag_searxng-data:/data \
alpine \
vi /data/settings.yml


torproxy 추가
podman compose up -d rag-tor
podman exec -it rag-searxng sh
cd /etc/searxng
vi settings.yml

outgoing:
proxies:
http: socks5h://tor:9050
https: socks5h://tor:9050
추가하고 도커 컨테이너에서 나와서

curl --socks5-hostname 127.0.0.1:9050 https://api.ipify.org
109.70.100.6%       이런게 나온다.

차단 되서 프록시로 접근하기 위해 tor설치 했는데 되려 tor에 블랙리스트에 있는지 차단되서 프록시 설정 지웠더니 검색 함