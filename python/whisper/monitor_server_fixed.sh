#!/bin/bash

# 로그 파일 설정 - 홈 디렉토리 사용
LOG_DIR="/apps/whisper/logs"
PID_FILE="/apps/whisper/whisper-server.pid"
SERVER_LOG="$LOG_DIR/server.log"
ERROR_LOG="$LOG_DIR/error.log"
MONITOR_LOG="$LOG_DIR/monitor.log"
DEBUG_LOG="$LOG_DIR/debug.log"

# 로그 디렉토리 생성 (홈 디렉토리에)
mkdir -p $LOG_DIR

# 로그 함수
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $MONITOR_LOG
}

debug_log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] DEBUG: $1" >> $DEBUG_LOG
}

# Whisper 서버가 실행 중인지 확인
check_process() {
    pgrep -f "python.*server.py" > /dev/null
    return $?
}

# 포트 5000이 열려있는지 확인
check_port() {
    netstat -tuln 2>/dev/null | grep -q ":5000 " || ss -tuln 2>/dev/null | grep -q ":5000 "
    return $?
}

# 환경 변수 설정
export CUDA_VISIBLE_DEVICES=0
export LD_LIBRARY_PATH=/usr/local/cuda/lib64:$LD_LIBRARY_PATH
export PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512
export PATH=/apps/whisper/.venv/bin:$PATH

# .env 파일이 있으면 로드
if [ -f /apps/whisper/.env ]; then
    debug_log "Loading .env file"
    set -a  # 자동 export
    source /apps/whisper/.env
    set +a
fi

# HF_TOKEN이 여전히 비어있으면 기본값 설정
export HF_TOKEN=${HF_TOKEN:-"your_huggingface_token_here"}

# 디버그 정보 로깅
debug_log "=== Environment Check ==="
debug_log "User: $(whoami)"
debug_log "Home: $HOME"
debug_log "Log directory: $LOG_DIR"
debug_log "PATH: $PATH"
debug_log "CUDA_VISIBLE_DEVICES: $CUDA_VISIBLE_DEVICES"
debug_log "HF_TOKEN set: $([ -n "$HF_TOKEN" ] && echo 'Yes' || echo 'No')"
debug_log "Python in venv: $(/apps/whisper/.venv/bin/python --version 2>&1)"

# GPU 확인
if command -v nvidia-smi > /dev/null 2>&1; then
    GPU_INFO=$(nvidia-smi --query-gpu=name,memory.total --format=csv,noheader 2>&1)
    debug_log "GPU Available: $GPU_INFO"
else
    debug_log "nvidia-smi not found"
fi

# 메인 로직
if check_process; then
    # 프로세스는 있지만 포트가 안 열려있는 경우 체크
    if ! check_port; then
        log_message "WARNING: Process exists but port 5000 is not listening"
        debug_log "Process found but port check failed"

        # 프로세스 종료 시도
        pkill -f "python.*server.py"
        sleep 2

        # 강제 종료
        pkill -9 -f "python.*server.py" 2>/dev/null
        sleep 1

        log_message "Killed unresponsive process"
    else
        # 정상 작동 중
        PID=$(pgrep -f 'python.*server.py')
        debug_log "Server is running normally (PID: $PID)"
        exit 0
    fi
fi

# 서버가 죽어있으면 시작
if ! check_process; then
    log_message "Server is down. Starting Whisper server..."
    debug_log "Starting server..."

    # 이전 로그 백업 (100MB 이상일 경우)
    if [ -f "$SERVER_LOG" ]; then
        LOG_SIZE=$(stat -f%z "$SERVER_LOG" 2>/dev/null || stat -c%s "$SERVER_LOG" 2>/dev/null || echo 0)
        if [ "$LOG_SIZE" -gt 104857600 ]; then
            mv "$SERVER_LOG" "$SERVER_LOG.$(date +%Y%m%d_%H%M%S)"
            touch "$SERVER_LOG"
            debug_log "Rotated large log file"
        fi
    fi

    # 필수 파일/디렉토리 확인
    if [ ! -d "/apps/whisper" ]; then
        log_message "ERROR: Directory /apps/whisper does not exist"
        exit 1
    fi

    if [ ! -f "/apps/whisper/server.py" ]; then
        log_message "ERROR: server.py not found in /apps/whisper"
        exit 1
    fi

    if [ ! -d "/apps/whisper/.venv" ]; then
        log_message "ERROR: Virtual environment not found at /apps/whisper/.venv"
        exit 1
    fi

    # 서버 시작 디렉토리로 이동
    cd /apps/whisper
    debug_log "Changed directory to: $(pwd)"

    # Python 모듈 체크 (선택사항)
    debug_log "Checking Python modules..."
    /apps/whisper/.venv/bin/python -c "import flask; print('Flask OK')" >> $DEBUG_LOG 2>&1
    /apps/whisper/.venv/bin/python -c "import torch; print(f'PyTorch OK, CUDA: {torch.cuda.is_available()}')" >> $DEBUG_LOG 2>&1
    /apps/whisper/.venv/bin/python -c "from faster_whisper import WhisperModel; print('Faster-whisper OK')" >> $DEBUG_LOG 2>&1

    # 서버 시작
    debug_log "Starting server with nohup..."

    # nohup으로 백그라운드 실행
    nohup /apps/whisper/.venv/bin/python -u /apps/whisper/server.py \
        >> "$SERVER_LOG" 2>> "$ERROR_LOG" &

    SERVER_PID=$!
    echo $SERVER_PID > $PID_FILE
    debug_log "Server started with PID: $SERVER_PID"

    # 프로세스가 시작되었는지 확인 (짧은 대기)
    sleep 2

    # 프로세스가 여전히 살아있는지 확인
    if ! ps -p $SERVER_PID > /dev/null 2>&1; then
        log_message "ERROR: Process died immediately"
        debug_log "Server process died. Last 20 lines of error log:"
        tail -n 20 $ERROR_LOG >> $DEBUG_LOG 2>&1
        tail -n 20 $ERROR_LOG  # 콘솔에도 출력
        exit 1
    fi

    # 서버가 완전히 시작될 때까지 대기 (최대 30초)
    COUNTER=0
    MAX_WAIT=30

    while [ $COUNTER -lt $MAX_WAIT ]; do
        if check_port; then
            log_message "Server started successfully (PID: $SERVER_PID)"
            debug_log "Port 5000 is now listening"
            exit 0
        fi

        # 프로세스가 죽었는지 확인
        if ! ps -p $SERVER_PID > /dev/null 2>&1; then
            log_message "ERROR: Server process died during startup"
            debug_log "Last 20 lines of server log:"
            tail -n 20 $SERVER_LOG >> $DEBUG_LOG 2>&1
            debug_log "Last 20 lines of error log:"
            tail -n 20 $ERROR_LOG >> $DEBUG_LOG 2>&1
            tail -n 10 $ERROR_LOG  # 콘솔에도 에러 출력
            exit 1
        fi

        sleep 1
        COUNTER=$((COUNTER + 1))

        if [ $((COUNTER % 5)) -eq 0 ]; then
            debug_log "Waiting for server to start... ($COUNTER/$MAX_WAIT seconds)"
        fi
    done

    # 시간 초과
    log_message "ERROR: Server started but port 5000 is not listening after $MAX_WAIT seconds"
    debug_log "Last lines of server log:"
    tail -n 20 $SERVER_LOG >> $DEBUG_LOG 2>&1
    exit 1
fi