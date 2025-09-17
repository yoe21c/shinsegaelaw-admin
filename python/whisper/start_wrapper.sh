#!/bin/bash

# Wrapper 스크립트 - 직접 실행과 동일한 환경을 제공

LOG_DIR="/apps/whisper/logs"
mkdir -p $LOG_DIR

# 전체 환경을 SSH 세션과 동일하게 설정
source /etc/profile
source ~/.profile
source ~/.bashrc 2>/dev/null || true

# 로그 함수
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_DIR/monitor.log
}

# 환경변수 명시적 설정
export HOME=/home/ubuntu
export USER=ubuntu
export LOGNAME=ubuntu
export SHELL=/bin/bash
export PATH=/apps/whisper/.venv/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# CUDA 환경 설정
export CUDA_HOME=/usr/local/cuda
export CUDA_VISIBLE_DEVICES=0
export LD_LIBRARY_PATH=/usr/local/cuda/lib64:$LD_LIBRARY_PATH
export PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512

# .env 파일 로드
if [ -f /apps/whisper/.env ]; then
    set -a
    source /apps/whisper/.env
    set +a
fi

# HF_TOKEN 확인
if [ -z "$HF_TOKEN" ]; then
    log_message "WARNING: HF_TOKEN not set"
fi

# 프로세스 체크
check_process() {
    pgrep -f "python.*server.py" > /dev/null
}

check_port() {
    ss -tuln 2>/dev/null | grep -q ":5000 " || netstat -tuln 2>/dev/null | grep -q ":5000 "
}

# 이미 실행 중이면 종료
if check_process; then
    if check_port; then
        exit 0  # 정상 작동 중
    else
        log_message "Process exists but port not listening - killing"
        pkill -f "python.*server.py"
        sleep 2
        pkill -9 -f "python.*server.py" 2>/dev/null
        sleep 1
    fi
fi

# 서버 시작
log_message "Starting Whisper server..."

cd /apps/whisper

# Python 가상환경 활성화
source .venv/bin/activate

# ulimit 설정 (리소스 제한 완화)
ulimit -n 65536  # 파일 디스크립터
ulimit -u 32768  # 프로세스 수
ulimit -m unlimited  # 메모리
ulimit -v unlimited  # 가상 메모리

# 디버그 정보
log_message "Environment:"
log_message "  Python: $(which python)"
log_message "  CUDA_VISIBLE_DEVICES: $CUDA_VISIBLE_DEVICES"
log_message "  HF_TOKEN: ${HF_TOKEN:+SET}"

# setsid를 사용하여 새 세션에서 실행 (TTY 독립적)
setsid python -u server.py >> $LOG_DIR/server.log 2>> $LOG_DIR/error.log &

SERVER_PID=$!
echo $SERVER_PID > $HOME/whisper-server.pid

# 시작 확인
sleep 5

if ps -p $SERVER_PID > /dev/null && check_port; then
    log_message "Server started successfully (PID: $SERVER_PID)"
else
    log_message "Server failed to start"
    tail -20 $LOG_DIR/error.log | while read line; do
        log_message "ERROR: $line"
    done
    exit 1
fi