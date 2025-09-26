from faster_whisper import WhisperModel
import time
import torch
import logging
import sys
import os
import tempfile
import urllib.request
import json
from urllib.parse import urlparse, unquote
from pathlib import Path
from collections import defaultdict
import subprocess
from datetime import datetime

# Flask imports
from flask import Flask, request, jsonify, Response
import json as json_module

# pyannote는 선택적 - 없어도 실행 가능
try:
    from pyannote.audio import Pipeline
    PYANNOTE_AVAILABLE = True
except ImportError:
    PYANNOTE_AVAILABLE = False

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('whisper_transcription.log', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

# Hugging Face 토큰 (화자 분리를 위해 필요)
HF_TOKEN = os.environ.get("HF_TOKEN", "")  # 환경변수로 설정

# Flask app 초기화
app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False  # 한글 깨짐 방지

# 전역 모델 초기화
logger.info("Whisper 모델 로딩 시작...")
whisper_model = WhisperModel(
    "large-v2",
    device="cuda",
    compute_type="float16",
    device_index=0,
    download_root="/tmp/whisper-models"
)
logger.info("Whisper 모델 로딩 완료")

# 화자 분리 모델 초기화
diarization_pipeline = None
if PYANNOTE_AVAILABLE and HF_TOKEN:
    try:
        logger.info("화자 분리 모델 로딩 시작...")
        diarization_pipeline = Pipeline.from_pretrained(
            "pyannote/speaker-diarization-3.1",
            use_auth_token=HF_TOKEN
        )
        if torch.cuda.is_available():
            diarization_pipeline.to(torch.device("cuda"))
        logger.info("화자 분리 모델 로딩 완료")
    except Exception as e:
        logger.warning(f"화자 분리 모델 로딩 실패: {e}")
        logger.warning("화자 분리 없이 진행합니다.")
elif not PYANNOTE_AVAILABLE:
    logger.warning("pyannote.audio가 설치되지 않음. 화자 분리 기능을 사용하려면 'pip install pyannote.audio'를 실행하세요.")
    logger.info("화자 분리 없이 음성 인식만 진행합니다.")
else:
    logger.warning("HF_TOKEN이 설정되지 않음. 화자 분리를 사용하려면 환경변수를 설정하세요.")

def get_file_size_from_url(url):
    """URL로부터 파일 크기 가져오기"""
    try:
        # URL을 안전하게 인코딩
        from urllib.parse import quote
        safe_url = quote(url, safe=':/?&=')

        req = urllib.request.Request(safe_url, method='HEAD')

        response = urllib.request.urlopen(req)
        file_size = response.headers.get('Content-Length')
        return int(file_size) if file_size else None
    except Exception as e:
        logger.warning(f"파일 크기 가져오기 실패: {e}")
        return None

def parse_filename_metadata(filename):
    """파일명에서 고객 정보 추출
    포맷1 (언더바 1개): 고객전화번호_상담완료일시.m4a
    포맷2 (언더바 2개): 고객이름_고객전화번호_상담완료일시.m4a
    """
    try:
        # 확장자 제거
        name_without_ext = os.path.splitext(filename)[0]
        parts = name_without_ext.split('_')

        metadata = {}

        if len(parts) == 2:
            # 포맷1: 고객전화번호_상담완료일시
            phone_number = parts[0]
            counsel_datetime_str = parts[1]

            metadata['customerPhoneNumber'] = phone_number

        elif len(parts) == 3:
            # 포맷2: 고객이름_고객전화번호_상담완료일시
            customer_name = parts[0]
            phone_number = parts[1]
            counsel_datetime_str = parts[2]

            metadata['customer'] = customer_name
            metadata['customerPhoneNumber'] = phone_number
        else:
            # 예상하지 못한 포맷
            logger.warning(f"예상하지 못한 파일명 포맷: {filename}")
            return metadata

        # 상담완료일시 파싱 (YYYYMMDDHHmmss 형식을 datetime으로)
        if len(parts) >= 2:
            counsel_datetime_str = parts[-1]  # 마지막 부분이 날짜
            try:
                # 20250822124621 -> 2025-08-22 12:46:21
                if len(counsel_datetime_str) == 14 and counsel_datetime_str.isdigit():
                    year = counsel_datetime_str[0:4]
                    month = counsel_datetime_str[4:6]
                    day = counsel_datetime_str[6:8]
                    hour = counsel_datetime_str[8:10]
                    minute = counsel_datetime_str[10:12]
                    second = counsel_datetime_str[12:14]

                    metadata['counselAt'] = f"{year}-{month}-{day} {hour}:{minute}:{second}"
            except Exception as e:
                logger.warning(f"상담일시 파싱 실패: {counsel_datetime_str}, 오류: {e}")

        return metadata

    except Exception as e:
        logger.error(f"파일명 메타데이터 파싱 실패: {e}")
        return {}

def extract_file_metadata(url):
    """URL로부터 파일 메타데이터 추출"""
    try:
        parsed_url = urlparse(url)
        encoded_filename = os.path.basename(parsed_url.path) or "audio.m4a"

        # URL 디코딩하여 한글 복원
        filename = unquote(encoded_filename)
        logger.info(f"디코딩된 파일명: {filename}")

        # 파일 확장자 추출
        file_extension = os.path.splitext(filename)[1].lower()
        if file_extension:
            file_type = file_extension[1:]  # . 제거
        else:
            file_type = 'm4a'  # 기본값

        # 파일 크기 가져오기
        file_size = get_file_size_from_url(url)

        # 파일명에서 고객 정보 추출
        filename_metadata = parse_filename_metadata(filename)

        metadata = {
            'fileName': filename,
            'fileType': file_type,
            'fileSize': file_size,
            'url': url
        }

        # 파일명에서 추출한 메타데이터 병합
        metadata.update(filename_metadata)

        return metadata
    except Exception as e:
        logger.error(f"메타데이터 추출 실패: {e}")
        return None

@app.route('/api/health', methods=['GET'])
def health_check():
    """헬스 체크 API"""
    return jsonify({"result": "ok"})

@app.route('/api/counsel/add', methods=['POST'])
def add_counsel():
    """상담 녹취 파일 처리 API - URL을 받아서 화자 분리 후 JSON 반환"""
    try:
        # 요청 데이터 파싱
        data = request.get_json()
        if not data or 'url' not in data:
            return jsonify({"error": "url parameter is required"}), 400

        url = data['url']
        num_speakers = data.get('num_speakers', None)  # 옵션: 예상 화자 수

        logger.info(f"처리 시작: URL={url}, 예상 화자 수={num_speakers}")

        # URL로부터 메타데이터 추출
        metadata = extract_file_metadata(url)
        if not metadata:
            return jsonify({"error": "Failed to extract file metadata"}), 500

        # 임시 디렉토리에서 작업
        with tempfile.TemporaryDirectory() as temp_dir:
            # 1. 파일 다운로드
            audio_file = download_audio_from_url(url, temp_dir)

            # 2. m4a 파일인 경우 wav로 변환 (옵션)
            if audio_file.lower().endswith('.m4a'):
                converted_file = convert_m4a_to_wav(audio_file, temp_dir)
                if converted_file:
                    audio_file = converted_file
                    logger.info(f"m4a 파일을 wav로 변환 완료: {converted_file}")

            # 3. 화자 분리 및 음성 인식 수행
            result = process_audio_file(audio_file, num_speakers)

            # 4. 메타데이터 추가
            result['metadata'] = metadata

            # 5. 처리 성공 응답 - 한글을 정상적으로 출력
            response_data = {
                "result": "ok",
                "data": result
            }

            # JSON 문자열로 직접 변환 (ensure_ascii=False로 한글 유지, 한줄로)
            json_str = json_module.dumps(response_data, ensure_ascii=False, separators=(',', ':'))

            # Response 객체로 반환
            return Response(
                json_str,
                mimetype='application/json; charset=utf-8',
                status=200
            )

    except Exception as e:
        logger.error(f"상담 처리 실패: {e}", exc_info=True)
        error_data = {"error": str(e)}
        json_str = json_module.dumps(error_data, ensure_ascii=False, separators=(',', ':'))
        return Response(
            json_str,
            mimetype='application/json; charset=utf-8',
            status=500
        )

def download_audio_from_url(url, temp_dir):
    """URL에서 오디오 파일을 다운로드"""
    logger.info(f"오디오 다운로드 시작: {url}")

    try:
        from urllib.parse import quote

        parsed_url = urlparse(url)
        encoded_filename = os.path.basename(parsed_url.path) or "audio.m4a"

        # URL 디코딩하여 한글 복원
        filename = unquote(encoded_filename)

        if not filename.endswith(('.m4a', '.wav', '.mp3', '.flac')):
            filename = filename + '.m4a'

        temp_path = os.path.join(temp_dir, filename)

        # URL을 안전하게 인코딩 (스키마, 호스트, 파라미터 등은 그대로 유지)
        safe_url = quote(url, safe=':/?&=')

        # 다운로드 진행상황 표시
        def download_progress(block_num, block_size, total_size):
            if total_size > 0:
                downloaded = block_num * block_size
                percent = min(100, (downloaded / total_size) * 100)
                if block_num % 100 == 0:  # 100블록마다 로그
                    logger.info(f"다운로드 진행: {percent:.1f}%")

        urllib.request.urlretrieve(safe_url, temp_path, reporthook=download_progress)

        file_size = os.path.getsize(temp_path) / (1024 * 1024)
        logger.info(f"다운로드 완료: {filename} ({file_size:.2f} MB)")

        return temp_path

    except Exception as e:
        logger.error(f"오디오 다운로드 실패: {e}")
        raise

def convert_m4a_to_wav(m4a_file, temp_dir):
    """m4a 파일을 wav로 변환"""
    try:
        wav_filename = os.path.basename(m4a_file).replace('.m4a', '.wav')
        wav_path = os.path.join(temp_dir, wav_filename)

        logger.info(f"m4a를 wav로 변환 시작: {m4a_file} -> {wav_path}")

        # ffmpeg를 사용하여 변환
        result = subprocess.run([
            'ffmpeg', '-i', m4a_file,
            '-ar', '16000',  # 16kHz 샘플레이트
            '-ac', '1',      # 모노
            '-y', wav_path
        ], capture_output=True, text=True)

        if result.returncode != 0:
            logger.error(f"ffmpeg 변환 실패: {result.stderr}")
            return None

        logger.info(f"wav 변환 성공: {wav_path}")
        return wav_path

    except Exception as e:
        logger.error(f"m4a 변환 실패: {e}")
        return None

def perform_diarization(audio_file, num_speakers=None):
    """화자 분리 수행"""
    if not diarization_pipeline:
        logger.warning("화자 분리 모델이 없습니다.")
        return None

    try:
        logger.info("화자 분리 시작...")
        start_time = time.time()

        # GPU 메모리 정리
        if torch.cuda.is_available():
            torch.cuda.empty_cache()

        # 화자 분리 실행
        if num_speakers:
            diarization = diarization_pipeline(audio_file, num_speakers=num_speakers)
        else:
            diarization = diarization_pipeline(audio_file)

        elapsed = time.time() - start_time

        # 화자별 시간 구간 정리
        speaker_segments = defaultdict(list)
        for turn, _, speaker in diarization.itertracks(yield_label=True):
            speaker_segments[speaker].append({
                "start": turn.start,
                "end": turn.end
            })

        num_detected_speakers = len(speaker_segments)
        logger.info(f"화자 분리 완료: {num_detected_speakers}명 감지 (소요시간: {elapsed:.2f}초)")

        return diarization

    except Exception as e:
        logger.error(f"화자 분리 실패: {e}")
        return None

def assign_speaker_to_segments(segments, diarization):
    """Whisper 세그먼트에 화자 할당"""
    if not diarization:
        return segments

    logger.info("세그먼트에 화자 할당 시작...")

    for segment in segments:
        segment_start = segment.start
        segment_end = segment.end
        segment_mid = (segment_start + segment_end) / 2

        # 중간 지점의 화자 찾기
        speaker = "Unknown"
        for turn, _, label in diarization.itertracks(yield_label=True):
            if turn.start <= segment_mid <= turn.end:
                speaker = label
                break

        segment.speaker = speaker

    logger.info("화자 할당 완료")
    return segments

def process_audio_file(audio_file, num_speakers=None):
    """실제 오디오 파일 처리 - 화자 분리 및 음성 인식"""
    start_time = time.time()

    # GPU 정보 로깅
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
        gpu_name = torch.cuda.get_device_name(0)
        gpu_memory = torch.cuda.memory_allocated(0) / 1024**3
        logger.info(f"GPU 사용: {gpu_name}")
        logger.info(f"GPU 메모리: {gpu_memory:.2f} GB")
    else:
        logger.warning("GPU를 사용할 수 없습니다. CPU로 실행됩니다.")

    # 1. 화자 분리
    diarization = perform_diarization(audio_file, num_speakers)

    # 2. 음성 인식
    logger.info("음성 인식 시작...")
    transcribe_start = time.time()

    segments, info = whisper_model.transcribe(
        audio_file,
        language="ko",
        beam_size=5,
        vad_filter=True,
        vad_parameters=dict(
            min_silence_duration_ms=500,
            threshold=0.5
        ),
        temperature=0,
        compression_ratio_threshold=2.4,
        log_prob_threshold=-1.0,
        no_speech_threshold=0.6
    )

    # segments를 리스트로 변환 (generator일 수 있음)
    segments_list = list(segments)

    transcribe_elapsed = time.time() - transcribe_start
    logger.info(f"음성 인식 완료 (소요시간: {transcribe_elapsed:.2f}초)")

    # 3. 화자 할당
    if diarization:
        segments_list = assign_speaker_to_segments(segments_list, diarization)

    # 4. 결과 정리
    output_data = {
        "duration": info.duration,
        "duration_ms": int(info.duration * 1000),
        "processing_time": time.time() - start_time,
        "segments": [],
        "full_transcript": "",
        "speaker_transcripts": defaultdict(str)
    }

    # 세그먼트 정리 및 전체 텍스트 생성
    for segment in segments_list:
        speaker = getattr(segment, 'speaker', 'Unknown')
        text = segment.text.strip()

        # 세그먼트 추가
        output_data["segments"].append({
            "speaker": speaker,
            "start": round(segment.start, 2),
            "end": round(segment.end, 2),
            "text": text
        })

        # 전체 텍스트 생성
        output_data["full_transcript"] += text + " "

        # 화자별 텍스트 생성
        output_data["speaker_transcripts"][speaker] += text + " "

    # defaultdict를 일반 dict로 변환
    output_data["speaker_transcripts"] = dict(output_data["speaker_transcripts"])

    # 전체 텍스트 정리
    output_data["full_transcript"] = output_data["full_transcript"].strip()

    # 통계 정보
    total_elapsed = time.time() - start_time

    # 처리 정보 추가
    output_data["statistics"] = {
        "total_processing_time": round(total_elapsed, 2),
        "audio_duration": round(info.duration, 2),
        "processing_speed": round(info.duration / total_elapsed, 2),
        "num_segments": len(output_data["segments"]),
        "num_speakers": len(output_data["speaker_transcripts"])
    }

    logger.info(f"\n=== 처리 완료 ===")
    logger.info(f"총 처리 시간: {total_elapsed:.2f}초")
    logger.info(f"오디오 길이: {info.duration:.2f}초")
    logger.info(f"처리 속도: {info.duration / total_elapsed:.2f}x")
    logger.info(f"감지된 화자 수: {len(output_data['speaker_transcripts'])}")

    # GPU 메모리 상태
    if torch.cuda.is_available():
        gpu_memory = torch.cuda.memory_allocated(0) / 1024**3
        logger.info(f"최종 GPU 메모리 사용량: {gpu_memory:.2f} GB")

    return output_data

@app.route('/api/transcribe', methods=['POST'])
def transcribe_audio():
    """간단한 음성 인식 API (화자 분리 없이)"""
    try:
        data = request.get_json()
        if not data or 'url' not in data:
            return jsonify({"error": "url parameter is required"}), 400

        url = data['url']

        with tempfile.TemporaryDirectory() as temp_dir:
            # 파일 다운로드
            audio_file = download_audio_from_url(url, temp_dir)

            # 음성 인식만 수행
            logger.info("음성 인식 시작 (화자 분리 없음)...")
            segments, info = whisper_model.transcribe(
                audio_file,
                language="ko",
                beam_size=5,
                vad_filter=True,
                temperature=0
            )

            # 결과 정리
            result = {
                "duration": info.duration,
                "segments": [],
                "full_transcript": ""
            }

            for segment in segments:
                result["segments"].append({
                    "start": round(segment.start, 2),
                    "end": round(segment.end, 2),
                    "text": segment.text.strip()
                })
                result["full_transcript"] += segment.text.strip() + " "

            result["full_transcript"] = result["full_transcript"].strip()

            # JSON 문자열로 직접 변환 (ensure_ascii=False로 한글 유지, 한줄로)
            response_data = {
                "result": "ok",
                "data": result
            }
            json_str = json_module.dumps(response_data, ensure_ascii=False, separators=(',', ':'))

            return Response(
                json_str,
                mimetype='application/json; charset=utf-8',
                status=200
            )

    except Exception as e:
        logger.error(f"음성 인식 실패: {e}", exc_info=True)
        error_data = {"error": str(e)}
        json_str = json_module.dumps(error_data, ensure_ascii=False, separators=(',', ':'))
        return Response(
            json_str,
            mimetype='application/json; charset=utf-8',
            status=500
        )

if __name__ == "__main__":
    # Flask 서버 모드로 실행
    logger.info("Flask API 서버 시작...")
    logger.info("사용 가능한 엔드포인트:")
    logger.info("  - GET  /api/health - 헬스 체크")
    logger.info("  - POST /api/counsel/add - 화자 분리 포함 전체 처리")
    logger.info("  - POST /api/transcribe - 간단한 음성 인식 (화자 분리 없음)")

    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port, debug=False)