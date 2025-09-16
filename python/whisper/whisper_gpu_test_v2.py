from faster_whisper import WhisperModel
import time
import torch
import logging
import sys
import os
import tempfile
import urllib.request
import json
from urllib.parse import urlparse
from pathlib import Path
from collections import defaultdict
import subprocess

# pyannote는 선택적 - 없어도 실행 가능
try:
    from pyannote.audio import Pipeline
    PYANNOTE_AVAILABLE = True
except ImportError:
    PYANNOTE_AVAILABLE = False
    logger = None  # 임시로 None, 나중에 설정됨

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

def download_audio_from_url(url, temp_dir):
    """URL에서 오디오 파일을 다운로드"""
    logger.info(f"오디오 다운로드 시작: {url}")
    
    try:
        parsed_url = urlparse(url)
        filename = os.path.basename(parsed_url.path) or "audio.m4a"
        
        if not filename.endswith('.m4a'):
            filename = filename + '.m4a'
        
        temp_path = os.path.join(temp_dir, filename)
        
        # 다운로드 진행상황 표시
        def download_progress(block_num, block_size, total_size):
            if total_size > 0:
                downloaded = block_num * block_size
                percent = min(100, (downloaded / total_size) * 100)
                if block_num % 100 == 0:  # 100블록마다 로그
                    logger.info(f"다운로드 진행: {percent:.1f}%")
        
        urllib.request.urlretrieve(url, temp_path, reporthook=download_progress)
        
        file_size = os.path.getsize(temp_path) / (1024 * 1024)
        logger.info(f"다운로드 완료: {filename} ({file_size:.2f} MB)")
        
        return temp_path
    
    except Exception as e:
        logger.error(f"오디오 다운로드 실패: {e}")
        raise

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
        
        # m4a 파일인 경우 직접 처리 (pyannote는 m4a를 직접 지원)
        # 하지만 문제가 있으면 ffmpeg으로 변환
        temp_wav = None
        audio_for_diarization = audio_file
        
        if audio_file.lower().endswith('.m4a'):
            try:
                # 먼저 직접 시도
                if num_speakers:
                    diarization = diarization_pipeline(
                        audio_file, 
                        num_speakers=num_speakers
                    )
                else:
                    diarization = diarization_pipeline(audio_file)
            except Exception as e:
                # 실패하면 ffmpeg으로 변환
                logger.warning(f"m4a 직접 처리 실패, ffmpeg으로 변환 시도: {e}")
                import subprocess
                temp_wav = audio_file.replace('.m4a', '_temp.wav')
                try:
                    subprocess.run([
                        'ffmpeg', '-i', audio_file, '-ar', '16000', 
                        '-ac', '1', '-y', temp_wav
                    ], check=True, capture_output=True)
                    audio_for_diarization = temp_wav
                    logger.info("wav 변환 완료")
                    
                    # 변환된 파일로 화자 분리 실행
                    if num_speakers:
                        diarization = diarization_pipeline(
                            audio_for_diarization, 
                            num_speakers=num_speakers
                        )
                    else:
                        diarization = diarization_pipeline(audio_for_diarization)
                except subprocess.CalledProcessError as ffmpeg_error:
                    logger.error(f"ffmpeg 변환 실패: {ffmpeg_error}")
                    return None
        else:
            # m4a가 아닌 경우 직접 처리
            if num_speakers:
                diarization = diarization_pipeline(
                    audio_for_diarization, 
                    num_speakers=num_speakers
                )
            else:
                diarization = diarization_pipeline(audio_for_diarization)
        
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
        
        # 임시 파일 정리
        if temp_wav and os.path.exists(temp_wav):
            os.remove(temp_wav)
            logger.info("임시 wav 파일 삭제")
        
        return diarization
    
    except Exception as e:
        logger.error(f"화자 분리 실패: {e}")
        # 임시 파일 정리
        if 'temp_wav' in locals() and temp_wav and os.path.exists(temp_wav):
            os.remove(temp_wav)
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

def transcribe_with_speakers(audio_source, num_speakers=None):
    """음성 인식 및 화자 분리 통합 처리"""
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
    
    # URL인 경우 다운로드
    if audio_source.startswith(('http://', 'https://')):
        with tempfile.TemporaryDirectory() as temp_dir:
            audio_file = download_audio_from_url(audio_source, temp_dir)
            return process_audio_file(audio_file, num_speakers, start_time)
    else:
        # 로컬 파일
        return process_audio_file(audio_source, num_speakers, start_time)

def process_audio_file(audio_file, num_speakers, start_time):
    """실제 오디오 파일 처리"""
    
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
    
    # 4. 결과 저장 및 출력
    output_data = {
        "audio_file": audio_file,
        "duration": info.duration,
        "processing_time": time.time() - start_time,
        "segments": []
    }
    
    logger.info("\n=== 전사 결과 ===")
    
    with open("transcript_with_speakers.txt", "w", encoding="utf-8") as f:
        current_speaker = None
        
        for segment in segments_list:
            speaker = getattr(segment, 'speaker', 'Unknown')
            
            # 화자가 바뀔 때만 화자 표시
            if speaker != current_speaker:
                speaker_header = f"\n[화자 {speaker}]"
                f.write(speaker_header + "\n")
                logger.info(speaker_header)
                current_speaker = speaker
            
            # 타임스탬프와 텍스트
            text_line = f"  [{segment.start:.2f}s - {segment.end:.2f}s] {segment.text.strip()}"
            f.write(text_line + "\n")
            print(text_line)
            
            # JSON 데이터용
            output_data["segments"].append({
                "speaker": speaker,
                "start": segment.start,
                "end": segment.end,
                "text": segment.text.strip()
            })
    
    # JSON 파일로도 저장
    with open("transcript_with_speakers.json", "w", encoding="utf-8") as f:
        json.dump(output_data, f, ensure_ascii=False, indent=2)
    
    # 통계 정보
    total_elapsed = time.time() - start_time
    
    logger.info(f"\n=== 처리 완료 ===")
    logger.info(f"총 처리 시간: {total_elapsed:.2f}초")
    logger.info(f"오디오 길이: {info.duration:.2f}초")
    logger.info(f"처리 속도: {info.duration / total_elapsed:.2f}x")
    
    if diarization:
        speakers = set(s.get('speaker') for s in output_data['segments'])
        logger.info(f"감지된 화자 수: {len(speakers)}")
    
    # GPU 메모리 상태
    if torch.cuda.is_available():
        gpu_memory = torch.cuda.memory_allocated(0) / 1024**3
        logger.info(f"최종 GPU 메모리 사용량: {gpu_memory:.2f} GB")
    
    logger.info(f"결과 파일 생성: transcript_with_speakers.txt, transcript_with_speakers.json")
    
    return output_data

if __name__ == "__main__":
    # 명령줄 인자 처리
    if len(sys.argv) < 2:
        print("사용법: python whisper_gpu_test.py <audio_file_or_url> [num_speakers]")
        print("예시: python whisper_gpu_test.py https://example.com/audio.m4a")
        print("예시: python whisper_gpu_test.py ./test_mom.m4a 2")
        sys.exit(1)
    
    audio_input = sys.argv[1]
    num_speakers = int(sys.argv[2]) if len(sys.argv) > 2 else None
    
    try:
        logger.info(f"처리 시작: {audio_input}")
        if num_speakers:
            logger.info(f"예상 화자 수: {num_speakers}")
        
        result = transcribe_with_speakers(audio_input, num_speakers)
        
        logger.info("모든 처리가 완료되었습니다.")
        
    except Exception as e:
        logger.error(f"오류 발생: {e}", exc_info=True)
        sys.exit(1)

