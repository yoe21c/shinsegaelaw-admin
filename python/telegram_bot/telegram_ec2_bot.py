#!/usr/bin/env python3
import os
import boto3
from telegram import Update, ReplyKeyboardMarkup, KeyboardButton
from telegram.ext import Application, CommandHandler, MessageHandler, filters, ContextTypes
import asyncio
from dotenv import load_dotenv
import requests
from datetime import datetime, timedelta

# 환경 변수 로드
load_dotenv()

# AWS EC2 클라이언트 설정
ec2 = boto3.client(
    'ec2',
    region_name=os.getenv('AWS_REGION', 'ap-northeast-2'),
    aws_access_key_id=os.getenv('AWS_ACCESS_KEY_ID'),
    aws_secret_access_key=os.getenv('AWS_SECRET_ACCESS_KEY')
)

# EC2 인스턴스 ID
# ai-server: i-064afa675153e9d4c
INSTANCE_ID = os.getenv('EC2_INSTANCE_ID')

# 텔레그램 봇 토큰
# shinsegaelaw_bot
BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')

# 상태 한글 매핑
STATUS_KOREAN = {
    'pending': '🟡 시작 중',
    'running': '🟢 실행 중',
    'stopping': '🟠 중지 중',
    'stopped': '🔴 중지됨',
    'shutting-down': '⚫ 종료 중',
    'terminated': '⚫ 종료됨',
    'rebooting': '🔄 재부팅 중'
}

# 메뉴 키보드
def get_main_keyboard():
    keyboard = [
        [KeyboardButton("🖥️ AI서버상태"), KeyboardButton("⏱️ 서버 실행시간")],
        [KeyboardButton("✅ AI서버 켜기"), KeyboardButton("❌ AI서버 끄기")],
        [KeyboardButton("🎤 AI음성인식 서버"), KeyboardButton("🔬 AI분석 서버")]
    ]
    return ReplyKeyboardMarkup(keyboard, resize_keyboard=True)

# EC2 인스턴스 상태 확인
def get_instance_status():
    try:
        response = ec2.describe_instances(InstanceIds=[INSTANCE_ID])
        instance = response['Reservations'][0]['Instances'][0]
        state = instance['State']['Name']

        # Public IP 가져오기 (실행 중일 때만)
        public_ip = instance.get('PublicIpAddress', 'N/A')
        private_ip = instance.get('PrivateIpAddress', 'N/A')

        # LaunchTime 가져오기
        launch_time = instance.get('LaunchTime', None)

        return {
            'state': state,
            'state_korean': STATUS_KOREAN.get(state, state),
            'public_ip': public_ip,
            'private_ip': private_ip,
            'instance_type': instance.get('InstanceType', 'N/A'),
            'launch_time': launch_time
        }
    except Exception as e:
        return {'error': str(e)}

# EC2 인스턴스 시작
def start_instance():
    try:
        # 현재 상태 확인
        status = get_instance_status()
        if status.get('state') == 'running':
            return {'already_running': True, 'status': status}

        # 인스턴스 시작
        ec2.start_instances(InstanceIds=[INSTANCE_ID])
        return {'success': True}
    except Exception as e:
        return {'error': str(e)}

# EC2 인스턴스 중지
def stop_instance():
    try:
        # 현재 상태 확인
        status = get_instance_status()
        if status.get('state') == 'stopped':
            return {'already_stopped': True, 'status': status}

        # 인스턴스 중지
        ec2.stop_instances(InstanceIds=[INSTANCE_ID])
        return {'success': True}
    except Exception as e:
        return {'error': str(e)}

# 서버 실행 시간 계산
def get_uptime(launch_time):
    if launch_time:
        try:
            # launch_time은 이미 datetime 객체임 (AWS SDK가 반환)
            # 현재 UTC 시간을 가져옴
            from datetime import timezone
            now_utc = datetime.now(timezone.utc)

            # launch_time이 timezone aware인지 확인
            if launch_time.tzinfo is None:
                # timezone naive인 경우 UTC로 설정
                launch_time = launch_time.replace(tzinfo=timezone.utc)

            uptime = now_utc - launch_time

            days = uptime.days
            hours, remainder = divmod(uptime.seconds, 3600)
            minutes, _ = divmod(remainder, 60)

            if days > 0:
                return f"{days}일 {hours}시간 {minutes}분"
            elif hours > 0:
                return f"{hours}시간 {minutes}분"
            else:
                return f"{minutes}분"
        except Exception as e:
            return f"계산 오류: {str(e)}"
    return "N/A"

# AI 음성인식 서버 상태 확인
def check_voice_server():
    try:
        response = requests.get('http://3.38.59.217:5000/api/health', timeout=5)
        if response.status_code == 200 and response.json().get('result') == 'ok':
            return {'status': 'running', 'message': '🟢 정상 작동 중'}
        else:
            return {'status': 'error', 'message': f'⚠️ 서버 응답 오류 (상태코드: {response.status_code})'}
    except requests.exceptions.ConnectionError:
        return {'status': 'stopped', 'message': '🔴 서버가 꺼져 있습니다'}
    except requests.exceptions.Timeout:
        return {'status': 'timeout', 'message': '⏱️ 서버 응답 시간 초과'}
    except Exception as e:
        return {'status': 'error', 'message': f'❌ 오류: {str(e)}'}

# AI 분석 서버 상태 확인
def check_analysis_server():
    try:
        response = requests.get('http://3.38.59.217:11434/', timeout=5)
        if response.status_code == 200 and 'Ollama is running' in response.text:
            return {'status': 'running', 'message': '🟢 정상 작동 중'}
        else:
            return {'status': 'error', 'message': f'⚠️ 서버 응답 오류 (상태코드: {response.status_code})'}
    except requests.exceptions.ConnectionError:
        return {'status': 'stopped', 'message': '🔴 서버가 꺼져 있습니다'}
    except requests.exceptions.Timeout:
        return {'status': 'timeout', 'message': '⏱️ 서버 응답 시간 초과'}
    except Exception as e:
        return {'status': 'error', 'message': f'❌ 오류: {str(e)}'}

# /start 명령 핸들러
async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(
        "🤖 AI 서버 관리 봇입니다.\n"
        "아래 메뉴를 선택해주세요.",
        reply_markup=get_main_keyboard()
    )

# 메시지 핸들러
async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    text = update.message.text

    if text == "🖥️ AI서버상태":
        await update.message.reply_text("⏳ 서버 상태를 확인 중입니다...")

        status = get_instance_status()

        if 'error' in status:
            await update.message.reply_text(
                f"❌ 오류가 발생했습니다:\n{status['error']}",
                reply_markup=get_main_keyboard()
            )
        else:
            message = (
                f"📊 **AI 서버 상태**\n\n"
                f"상태: {status['state_korean']}\n"
#                 f"인스턴스 타입: {status['instance_type']}\n"
#                 f"Public IP: {status['public_ip']}\n"
#                 f"Private IP: {status['private_ip']}\n"
            )
            await update.message.reply_text(
                message,
                parse_mode='Markdown',
                reply_markup=get_main_keyboard()
            )

    elif text == "⏱️ 서버 실행시간":
        await update.message.reply_text("⏳ 서버 실행 시간을 확인 중입니다...")

        try:
            status = get_instance_status()

            if 'error' in status:
                await update.message.reply_text(
                    f"❌ 오류가 발생했습니다:\n{status['error']}",
                    reply_markup=get_main_keyboard()
                )
            else:
                if status['state'] == 'running' and status.get('launch_time'):
                    uptime = get_uptime(status['launch_time'])

                    # timezone aware datetime 처리
                    from datetime import timezone
                    launch_time = status['launch_time']
                    if launch_time.tzinfo is None:
                        launch_time = launch_time.replace(tzinfo=timezone.utc)

                    # KST로 변환 (UTC+9)
                    launch_time_kst = launch_time + timedelta(hours=9)

                    message = (
                        f"⏱️ **서버 실행 정보**\n\n"
                        f"현재 상태: {status['state_korean']}\n"
                        f"시작 시간: {launch_time_kst.strftime('%Y-%m-%d %H:%M:%S')} (KST)\n"
                        f"실행 시간: {uptime}\n"
                    )
                else:
                    message = (
                        f"⏱️ **서버 실행 정보**\n\n"
                        f"현재 상태: {status['state_korean']}\n"
                        f"서버가 실행 중이 아닙니다."
                    )

                await update.message.reply_text(
                    message,
                    parse_mode='Markdown',
                    reply_markup=get_main_keyboard()
                )
        except Exception as e:
            await update.message.reply_text(
                f"❌ 오류가 발생했습니다:\n{str(e)}",
                reply_markup=get_main_keyboard()
            )

    elif text == "🎤 AI음성인식 서버":
        await update.message.reply_text("⏳ AI 음성인식 서버 상태를 확인 중입니다...")

        result = check_voice_server()

        message = (
            f"🎤 **AI 음성인식 서버 상태**\n\n"
            f"{result['message']}"
        )

        await update.message.reply_text(
            message,
            parse_mode='Markdown',
            reply_markup=get_main_keyboard()
        )

    elif text == "🔬 AI분석 서버":
        await update.message.reply_text("⏳ AI 분석 서버 상태를 확인 중입니다...")

        result = check_analysis_server()

        message = (
            f"🔬 **AI 분석 서버 상태 (Ollama)**\n\n"
            f"{result['message']}"
        )

        await update.message.reply_text(
            message,
            parse_mode='Markdown',
            reply_markup=get_main_keyboard()
        )

    elif text == "✅ AI서버 켜기":
        await update.message.reply_text("⏳ 서버를 시작하는 중입니다...")

        result = start_instance()

        if 'error' in result:
            await update.message.reply_text(
                f"❌ 오류가 발생했습니다:\n{result['error']}",
                reply_markup=get_main_keyboard()
            )
        elif result.get('already_running'):
            status = result['status']
            message = (
                f"ℹ️ 서버가 이미 실행 중입니다.\n\n"
                f"📊 **현재 상태**\n"
                f"상태: {status['state_korean']}\n"
#                 f"Public IP: {status['public_ip']}\n"
#                 f"Private IP: {status['private_ip']}"
            )
            await update.message.reply_text(
                message,
                parse_mode='Markdown',
                reply_markup=get_main_keyboard()
            )
        else:
            await update.message.reply_text(
                "✅ 서버 시작 명령을 전송했습니다.\n"
                "완전히 시작되기까지 1-2분 정도 소요됩니다.",
                reply_markup=get_main_keyboard()
            )

            # 10초 후 상태 확인
            await asyncio.sleep(10)
            status = get_instance_status()
            message = (
                f"📊 **현재 상태**\n"
                f"상태: {status['state_korean']}\n"
            )
            await update.message.reply_text(
                message,
                parse_mode='Markdown',
                reply_markup=get_main_keyboard()
            )

    elif text == "❌ AI서버 끄기":
        await update.message.reply_text("⏳ 서버를 중지하는 중입니다...")

        result = stop_instance()

        if 'error' in result:
            await update.message.reply_text(
                f"❌ 오류가 발생했습니다:\n{result['error']}",
                reply_markup=get_main_keyboard()
            )
        elif result.get('already_stopped'):
            status = result['status']
            message = (
                f"ℹ️ 서버가 이미 중지된 상태입니다.\n\n"
                f"📊 **현재 상태**\n"
                f"상태: {status['state_korean']}"
            )
            await update.message.reply_text(
                message,
                parse_mode='Markdown',
                reply_markup=get_main_keyboard()
            )
        else:
            await update.message.reply_text(
                "✅ 서버 중지 명령을 전송했습니다.\n"
                "완전히 중지되기까지 30초-1분 정도 소요됩니다.",
                reply_markup=get_main_keyboard()
            )

            # 10초 후 상태 확인
            await asyncio.sleep(10)
            status = get_instance_status()
            message = (
                f"📊 **현재 상태**\n"
                f"상태: {status['state_korean']}"
            )
            await update.message.reply_text(
                message,
                parse_mode='Markdown',
                reply_markup=get_main_keyboard()
            )
    else:
        await update.message.reply_text(
            "메뉴에서 선택해주세요.",
            reply_markup=get_main_keyboard()
        )

# 메인 함수
def main():
    # 필수 환경 변수 확인
    if not all([BOT_TOKEN, INSTANCE_ID]):
        print("❌ 필수 환경 변수가 설정되지 않았습니다.")
        print("다음 환경 변수를 .env 파일에 설정해주세요:")
        print("- TELEGRAM_BOT_TOKEN")
        print("- EC2_INSTANCE_ID")
        print("- AWS_ACCESS_KEY_ID")
        print("- AWS_SECRET_ACCESS_KEY")
        print("- AWS_REGION (선택사항, 기본값: ap-northeast-2)")
        return

    print(f"🚀 봇을 시작합니다...")
    print(f"📍 대상 인스턴스: {INSTANCE_ID}")

    # 애플리케이션 생성
    application = Application.builder().token(BOT_TOKEN).build()

    # 핸들러 추가
    application.add_handler(CommandHandler("start", start_command))
    application.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))

    # 봇 실행
    print("✅ 봇이 실행 중입니다. Ctrl+C로 종료할 수 있습니다.")
    application.run_polling(allowed_updates=Update.ALL_TYPES)

if __name__ == "__main__":
    main()