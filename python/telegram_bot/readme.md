
사용 방법:

1. 필요한 패키지 설치:
   pip install -r requirements.txt

2. .env 파일 생성:
   cp .env.example .env
   그리고 .env 파일에 실제 값을 입력:
- TELEGRAM_BOT_TOKEN: BotFather에서 받은 봇 토큰
- EC2_INSTANCE_ID: 제어할 EC2 인스턴스 ID
- AWS_ACCESS_KEY_ID: AWS 액세스 키
- AWS_SECRET_ACCESS_KEY: AWS 시크릿 키

3. 봇 실행:
   python telegram_ec2_bot.py

기능:

- 🖥️ AI서버상태: EC2 인스턴스의 현재 상태를 한글로 표시 (실행 중, 중지됨 등)
- ✅ AI서버 켜기: 서버 시작 (이미 켜져 있으면 알림)
- ❌ AI서버 끄기: 서버 중지 (이미 꺼져 있으면 알림)

봇을 시작하면 메뉴가 키보드 버튼으로 표시되어 쉽게 사용할 수 있습니다.
