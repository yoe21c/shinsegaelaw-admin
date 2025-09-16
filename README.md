# shinsegaelaw system

# ai 서버에서 설치할것
* gpt-oss:20b 설치하기
* sudo apt-get install -y ffmpeg 도 설치해야함. 
  * 왜냐면 화자구분모델에서 m4a 를 못쓰기 때문에  변환해야 되서.
* 대화추출/화자추출 api 테스트
  * curl 3.38.59.217:5000/api/health 
* 대화추출/화자추출 작업
  * curl -H 'Content-Type: application/json' 3.38.59.217:5000/api/counsel/add -d '{"url":"https://gpgpadad.s3.ap-northeast-2.amazonaws.com/counsel/%EC%97%84%EB%A7%88_01099650393_20250822124621.m4a"}'
* gpt-oss:20b 로 요청하기
  * curl 3.38.59.217:11434/api/tags | jq .
  * legal_consultation_evaluator.json 를 실행하기.