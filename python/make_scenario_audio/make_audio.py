from openai import OpenAI
from pydub import AudioSegment
import json
import os

client = OpenAI(api_key='')

with open('scenario_04_fraud.json') as f:
    data = json.load(f)

# 음성 설정 (변호사: 남성, 의뢰인: 여성)
voices = {
    "변호사": "onyx",    # 남성 목소리
    "의뢰인": "nova"     # 여성 목소리
}

final_audio = AudioSegment.silent(duration=0)
current_time = 0

for i, segment in enumerate(data['segments']):
    temp_file = f"temp_{i}.mp3"

    # 스트리밍 방식으로 처리
    with client.audio.speech.with_streaming_response.create(
        model="tts-1",
        voice=voices[segment['speaker']],
        input=segment['text']
    ) as response:
        response.stream_to_file(temp_file)

    # 오디오 처리
    audio = AudioSegment.from_mp3(temp_file)

    # 타이밍 조정
    silence_duration = (segment['start'] - current_time) * 1000
    if silence_duration > 0:
        final_audio += AudioSegment.silent(duration=silence_duration)

    final_audio += audio
    current_time = segment['start'] + len(audio) / 1000

    os.remove(temp_file)

# 최종 저장
final_audio.export("scenario_04_fraud.m4a", format="ipod")
print("오디오 파일 생성 완료!")