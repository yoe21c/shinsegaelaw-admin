import json
import requests
from typing import Dict, Any, Optional
import time
import sys

class LegalConsultationEvaluator:
    def __init__(self, ollama_host: str = "localhost", ollama_port: int = 11434):
        """
        법률 상담 평가 시스템 초기화

        Args:
            ollama_host: Ollama 서버 호스트 (IP 주소 또는 도메인)
            ollama_port: Ollama 서버 포트 (기본값: 11434)
        """
        self.ollama_url = f"http://{ollama_host}:{ollama_port}"
        self.model = "gpt-oss:20b"
        print(f"🔗 Ollama 서버 설정: {self.ollama_url}")

        # 서버 초기화 시 연결 확인 (최대 10분 대기)
        if not self.wait_for_server():
            print("⚠️  Ollama 서버 연결을 건너뜁니다. 나중에 다시 시도됩니다.")

    def wait_for_server(self, max_wait_seconds: int = 600, check_interval: int = 5) -> bool:
        """
        Ollama 서버가 실행될 때까지 대기

        Args:
            max_wait_seconds: 최대 대기 시간 (초) - 기본값: 600초 (10분)
            check_interval: 확인 간격 (초) - 기본값: 5초

        Returns:
            서버 연결 성공 여부
        """
        start_time = time.time()
        attempt = 0

        print(f"⏳ Ollama 서버 연결 대기 중... (최대 {max_wait_seconds}초)")
        print(f"   확인 간격: {check_interval}초")

        while time.time() - start_time < max_wait_seconds:
            attempt += 1
            elapsed = int(time.time() - start_time)

            try:
                # API 상태 확인
                response = requests.get(f"{self.ollama_url}/api/tags", timeout=3)

                if response.status_code == 200:
                    data = response.json()
                    models = data.get("models", [])

                    if models:
                        model_names = [m["name"] for m in models]
                        print(f"\n✅ Ollama 서버 연결 성공! (시도 {attempt}회, {elapsed}초 경과)")
                        print(f"📋 사용 가능한 모델: {', '.join(model_names)}")

                        if self.model in model_names:
                            print(f"✅ {self.model} 모델 확인됨!")
                            return True
                        else:
                            print(f"⚠️  {self.model} 모델이 없습니다. 모델을 로드해주세요.")
                            print(f"   명령어: ollama run {self.model}")
                    else:
                        print(f"\r⏳ [{elapsed}초] 서버는 실행 중이나 모델이 없습니다... (시도 {attempt}회)", end="")

                else:
                    print(f"\r⏳ [{elapsed}초] 서버 응답 오류 (상태 코드: {response.status_code})... (시도 {attempt}회)", end="")

            except requests.exceptions.Timeout:
                print(f"\r⏳ [{elapsed}초] 연결 시간 초과... (시도 {attempt}회)", end="")

            except requests.exceptions.ConnectionError:
                print(f"\r⏳ [{elapsed}초] 서버 연결 대기 중... (시도 {attempt}회)", end="")

            except Exception as e:
                print(f"\r⏳ [{elapsed}초] 오류 발생: {str(e)[:50]}... (시도 {attempt}회)", end="")

            # 최대 시간에 도달하지 않았다면 대기
            if time.time() - start_time < max_wait_seconds:
                time.sleep(check_interval)

        # 타임아웃
        elapsed_total = int(time.time() - start_time)
        print(f"\n\n❌ {elapsed_total}초 동안 서버 연결 실패 (시도 {attempt}회)")
        print(f"   서버 주소: {self.ollama_url}")
        print("\n💡 해결 방법:")
        print("   1. Ollama 서버가 실행 중인지 확인")
        print("   2. 서버 주소와 포트가 올바른지 확인")
        print("   3. 방화벽 설정 확인")
        print("   4. 원격 서버의 경우: OLLAMA_HOST=0.0.0.0 ollama serve")
        return False

    def create_prompt(self, conversation_data: Dict[str, Any]) -> str:
        """
        평가용 프롬프트 생성

        Args:
            conversation_data: 대화 내용 데이터

        Returns:
            완성된 프롬프트 문자열
        """
        prompt = """중요: 반드시 유효한 JSON 형식으로만 응답하세요. 설명이나 주석 없이 JSON만 제공하세요.

다음 고객 상담 내용을 분석하고 품질 평가를 JSON 형식으로 제공하세요.

법률 상담이 아니더라도 일반적인 고객 서비스 품질 기준으로 평가하세요.

필수 JSON 구조 (정확히 이 형식으로 응답):
{
  "evaluation": {
    "total_score": [0-100 사이의 숫자],
    "positive_aspects": [
      {
        "aspect": "[측면 이름 - 한글로]",
        "description": "[설명 - 한글로]",
        "related_segments": [인덱스 배열]
      }
    ],
    "negative_aspects": [
      {
        "aspect": "[측면 이름 - 한글로]",
        "description": "[설명 - 한글로]",
        "related_segments": [인덱스 배열]
      }
    ],
    "most_impressive": {
      "point": "[포인트 - 한글로]",
      "description": "[설명 - 한글로]",
      "segment_index": [인덱스 번호]
    },
    "improvements_needed": [
      {
        "segment_index": [인덱스],
        "current_response": "[현재 텍스트 - 한글로]",
        "suggestion": "[제안 텍스트 - 한글로]",
        "reason": "[이유 - 한글로]"
      }
    ],
    "comprehensive_evaluation": {
      "communication_skill": [0-100 점수],
      "legal_expertise": [0-100 점수],
      "customer_satisfaction": [0-100 점수],
      "problem_solving": [0-100 점수],
      "overall_comment": "[총평 - 한글로]"
    }
  }
}

대화 데이터:
"""
        # 대화 데이터를 보기 좋게 포맷팅
        conversation_str = json.dumps(conversation_data, ensure_ascii=False, indent=2)
        return prompt + conversation_str

    def test_connection(self) -> bool:
        """
        Ollama 서버 연결 테스트
        
        Returns:
            연결 성공 여부
        """
        try:
            response = requests.get(f"{self.ollama_url}/api/tags", timeout=5)
            if response.status_code == 200:
                models = response.json().get("models", [])
                model_names = [m["name"] for m in models]
                
                print(f"✅ Ollama 서버 연결 성공!")
                print(f"📋 사용 가능한 모델: {', '.join(model_names) if model_names else '없음'}")
                
                if self.model in model_names:
                    print(f"✅ {self.model} 모델 확인됨!")
                    return True
                else:
                    print(f"⚠️  {self.model} 모델을 찾을 수 없습니다.")
                    if model_names:
                        print(f"   다른 모델을 사용하려면 self.model 값을 변경하세요.")
                    else:
                        print(f"   원격 서버에서 'ollama run {self.model}' 명령을 실행하세요.")
                    return False
            else:
                print(f"❌ Ollama 서버 응답 오류: {response.status_code}")
                return False
                
        except requests.exceptions.Timeout:
            print(f"⏱️  연결 시간 초과. 서버 주소를 확인하세요: {self.ollama_url}")
            return False
        except requests.exceptions.ConnectionError as e:
            print(f"❌ 서버에 연결할 수 없습니다: {self.ollama_url}")
            print(f"   오류: {str(e)}")
            print("\n💡 확인사항:")
            print("   1. 원격 서버에서 Ollama가 실행 중인지 확인")
            print("   2. 방화벽 설정 확인 (포트 11434)")
            print("   3. 원격 서버에서 다음 명령으로 Ollama 실행:")
            print("      OLLAMA_HOST=0.0.0.0 ollama serve")
            return False
        except Exception as e:
            print(f"❌ 예상치 못한 오류: {e}")
            return False

    def call_ollama(self, prompt: str, retry_count: int = 0) -> Optional[Dict[str, Any]]:
        """
        Ollama API 호출

        Args:
            prompt: 평가 프롬프트
            retry_count: 현재 재시도 횟수

        Returns:
            API 응답 결과
        """
        url = f"{self.ollama_url}/api/generate"
        
        payload = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": 0.1 if retry_count > 0 else 0.2,  # 재시도 시 더 낮은 temperature
                "top_p": 0.9,
                "num_predict": 8000,  # 더 큰 응답 허용
                "seed": 42 + retry_count  # 재시도마다 다른 seed
            }
        }
        
        try:
            print(f"🤖 Ollama API 호출 중... (모델: {self.model})")
            print(f"📡 서버: {self.ollama_url}")
            
            response = requests.post(url, json=payload, timeout=180)  # 타임아웃 증가
            response.raise_for_status()
            
            result = response.json()

            # 응답 크기 확인
            if "response" in result:
                print(f"   응답 크기: {len(result['response'])} 문자")

            return result
            
        except requests.exceptions.Timeout:
            print(f"⏱️  API 호출 시간 초과 (180초)")
            print("   대용량 모델의 경우 응답 시간이 길 수 있습니다.")
            return None
        except requests.exceptions.ConnectionError as e:
            print(f"❌ 서버 연결 실패: {e}")
            print(f"   서버 주소를 확인하세요: {self.ollama_url}")
            return None
        except requests.exceptions.RequestException as e:
            print(f"❌ API 호출 오류: {e}")
            if hasattr(e, 'response') and e.response is not None:
                print(f"   응답 코드: {e.response.status_code}")
                print(f"   응답 내용: {e.response.text[:200]}")
            return None
        except json.JSONDecodeError as e:
            print(f"❌ JSON 파싱 오류: {e}")
            return None

    def parse_evaluation(self, response: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        Ollama 응답에서 평가 결과 파싱

        Args:
            response: Ollama API 응답

        Returns:
            파싱된 평가 결과
        """
        if not response or "response" not in response:
            return None

        # Ollama 응답에서 JSON 추출
        evaluation_text = response["response"]

        # 텍스트 정리 - 코드 블록 제거
        evaluation_text = evaluation_text.strip()
        if evaluation_text.startswith('```json'):
            evaluation_text = evaluation_text[7:]
        if evaluation_text.startswith('```'):
            evaluation_text = evaluation_text[3:]
        if evaluation_text.endswith('```'):
            evaluation_text = evaluation_text[:-3]
        evaluation_text = evaluation_text.strip()

        # JSON 파싱 시도
        try:
            evaluation = json.loads(evaluation_text)
            # 필수 키 검증
            if "evaluation" in evaluation:
                return evaluation
            else:
                # evaluation 키가 없으면 전체를 evaluation으로 감싸기
                return {"evaluation": evaluation}
        except json.JSONDecodeError:
            pass

        # JSON이 잘린 경우 복구 시도
        print(f"⚠️  완전한 JSON 파싱 실패, 복구 시도 중...")

        # 잘린 JSON 복구 함수
        def fix_truncated_json(text):
            import re
            # 열린 괄호와 닫힌 괄호 수 계산
            open_braces = text.count('{')
            close_braces = text.count('}')
            open_brackets = text.count('[')
            close_brackets = text.count(']')

            # 부족한 닫는 괄호 추가
            if open_brackets > close_brackets:
                text += ']' * (open_brackets - close_brackets)
            if open_braces > close_braces:
                text += '}' * (open_braces - close_braces)

            # 마지막 콤마 제거
            text = re.sub(r',\s*([\]}])', r'\1', text)

            return text

        # 복구 시도
        fixed_text = fix_truncated_json(evaluation_text)

        try:
            evaluation = json.loads(fixed_text)
            print("✅ 잘린 JSON 복구 성공!")

            if "evaluation" in evaluation:
                return evaluation
            else:
                return {"evaluation": evaluation}

        except json.JSONDecodeError as e:
            print(f"⚠️  JSON 복구 실패: {e}")
            print(f"원본 응답 일부: {evaluation_text[:500] if len(evaluation_text) > 500 else evaluation_text}")

        # JSON 부분만 추출 시도
        try:
            import re
            # 더 정확한 JSON 추출 패턴
            json_patterns = [
                r'\{\s*"evaluation"\s*:\s*\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}\s*\}',  # 중첩된 evaluation 객체
                r'\{[^{}]*"total_score"[^{}]*(?:\{[^{}]*\}[^{}]*)*\}',  # total_score 포함 객체
            ]

            for pattern in json_patterns:
                json_match = re.search(pattern, evaluation_text, re.DOTALL)
                if json_match:
                    try:
                        extracted = json_match.group()
                        extracted = fix_truncated_json(extracted)
                        evaluation = json.loads(extracted)
                        print("✅ JSON 패턴 추출 성공!")

                        if "evaluation" not in evaluation:
                            return {"evaluation": evaluation}
                        return evaluation
                    except:
                        continue

        except Exception as parse_error:
            print(f"❌ JSON 추출 최종 실패: {parse_error}")

        return None

    def evaluate(self, conversation_data: Dict[str, Any], max_retries: int = 3) -> Dict[str, Any]:
        """
        대화 내용 평가 실행 (재시도 로직 포함)
        
        Args:
            conversation_data: 평가할 대화 데이터
            max_retries: 최대 재시도 횟수
            
        Returns:
            평가 결과
        """
        # 연결 테스트
        if not self.test_connection():
            return {"error": "Ollama 서버 연결 실패"}
        
        # 프롬프트 생성
        prompt = self.create_prompt(conversation_data)
        
        # 재시도 로직
        for retry in range(max_retries):
            print(f"\n{'='*60}")
            if retry == 0:
                print("🚀 평가 시작...")
            else:
                print(f"🔄 재시도 {retry}/{max_retries}...")
            print('='*60)
            
            # Ollama API 호출
            response = self.call_ollama(prompt, retry_count=retry)
            
            if not response:
                if retry < max_retries - 1:
                    print("⏳ 3초 후 재시도...")
                    time.sleep(3)
                    continue
                else:
                    return {"error": "API 호출 실패 (모든 재시도 실패)"}
            
            # 평가 결과 파싱
            evaluation = self.parse_evaluation(response)
            
            if evaluation:
                print("\n" + "="*60)
                print("✅ 평가 완료!")
                print("="*60)
                return evaluation
            
            if retry < max_retries - 1:
                print("\n⏳ 파싱 실패, 3초 후 재시도...")
                time.sleep(3)
        
        # 모든 재시도 실패
        return {
            "error": "평가 결과 파싱 실패 (모든 재시도 실패)",
            "raw_response": response.get("response", "") if response else ""
        }

    def print_evaluation(self, evaluation: Dict[str, Any]):
        """
        평가 결과를 보기 좋게 출력
        
        Args:
            evaluation: 평가 결과
        """
        if "error" in evaluation:
            print(f"\n❌ 오류 발생: {evaluation['error']}")
            if "raw_response" in evaluation:
                print(f"원본 응답 일부: {evaluation['raw_response'][:500]}...")
            return
            
        eval_data = evaluation.get("evaluation", evaluation)
        
        print("\n" + "="*60)
        print("📊 법률 상담 품질 평가 결과")
        print("="*60)
        
        print(f"\n🎯 총점: {eval_data.get('total_score', 'N/A')}/100점")
        
        # 긍정적 측면
        print("\n✅ 긍정적 측면:")
        for aspect in eval_data.get("positive_aspects", []):
            print(f"  • {aspect['aspect']}: {aspect['description']}")
            print(f"    관련 대화: {aspect.get('related_segments', [])}")
        
        # 부정적 측면
        print("\n❌ 개선 필요 측면:")
        for aspect in eval_data.get("negative_aspects", []):
            print(f"  • {aspect['aspect']}: {aspect['description']}")
            print(f"    관련 대화: {aspect.get('related_segments', [])}")
        
        # 가장 인상적인 점
        impressive = eval_data.get("most_impressive", {})
        if impressive:
            print(f"\n⭐ 가장 인상적인 점:")
            print(f"  {impressive.get('point', 'N/A')}: {impressive.get('description', 'N/A')}")
        
        # 개선 제안
        print("\n💡 개선 제안:")
        for improvement in eval_data.get("improvements_needed", []):
            print(f"  대화 #{improvement.get('segment_index', 'N/A')}:")
            print(f"    현재: \"{improvement.get('current_response', 'N/A')}\"")
            print(f"    제안: \"{improvement.get('suggestion', improvement.get('suggested_response', 'N/A'))}\"")
            print(f"    이유: {improvement.get('reason', 'N/A')}")
        
        # 종합 평가
        comp_eval = eval_data.get("comprehensive_evaluation", {})
        if comp_eval:
            print("\n📈 세부 평가:")
            print(f"  • 의사소통 능력: {comp_eval.get('communication_skill', 'N/A')}점")
            print(f"  • 법률 전문성: {comp_eval.get('legal_expertise', 'N/A')}점")
            print(f"  • 고객 만족도: {comp_eval.get('customer_satisfaction', 'N/A')}점")
            print(f"  • 문제 해결력: {comp_eval.get('problem_solving', 'N/A')}점")
            print(f"\n📝 총평: {comp_eval.get('overall_comment', 'N/A')}")
        
        print("\n" + "="*60)


# 테스트용 샘플 데이터
def get_sample_conversation():
    """테스트용 샘플 대화 데이터 생성"""
    return {
        "audio_file": "./test_consultation.m4a",
        "duration": 223.9565,
        "processing_time": 30.722412824630737,
        "segments": [
            {
                "speaker": "상담사",
                "start": 0.72,
                "end": 2.72,
                "text": "안녕하세요, 김변호사 사무실입니다. 무엇을 도와드릴까요?"
            },
            {
                "speaker": "고객",
                "start": 2.72,
                "end": 5.72,
                "text": "아, 네. 저 교통사고 관련해서 상담받고 싶은데요."
            },
            {
                "speaker": "상담사",
                "start": 5.72,
                "end": 8.72,
                "text": "네, 교통사고 상담이시군요. 언제 사고가 발생하셨나요?"
            },
            {
                "speaker": "고객",
                "start": 8.72,
                "end": 11.72,
                "text": "어제 오후에 발생했는데, 상대방이 신호위반을 했어요."
            },
            {
                "speaker": "상담사",
                "start": 11.72,
                "end": 15.72,
                "text": "아, 그러셨군요. 경찰 신고는 하셨나요? 그리고 다치신 곳은 없으신가요?"
            },
            {
                "speaker": "고객",
                "start": 15.72,
                "end": 18.72,
                "text": "네, 경찰 신고했고요. 목이 좀 아픈데 병원은 아직 안 갔어요."
            },
            {
                "speaker": "상담사",
                "start": 18.72,
                "end": 22.72,
                "text": "일단 병원부터 가셔야 할 것 같은데요. 진단서가 있어야 보상받기 수월합니다."
            },
            {
                "speaker": "고객",
                "start": 22.72,
                "end": 25.72,
                "text": "그런가요? 그럼 어느 병원을 가야 하나요?"
            },
            {
                "speaker": "상담사",
                "start": 25.72,
                "end": 28.72,
                "text": "가까운 정형외과나 신경외과 가시면 됩니다."
            },
            {
                "speaker": "고객",
                "start": 28.72,
                "end": 31.72,
                "text": "알겠습니다. 그런데 상대방 보험사에서 연락이 왔는데 어떻게 해야 하나요?"
            },
            {
                "speaker": "상담사",
                "start": 31.72,
                "end": 35.72,
                "text": "아직 합의하지 마시고, 일단 치료부터 받으신 후에 저희와 상담하시는 게 좋을 것 같습니다."
            },
            {
                "speaker": "고객",
                "start": 35.72,
                "end": 38.72,
                "text": "네, 알겠습니다. 그럼 언제 방문하면 될까요?"
            },
            {
                "speaker": "상담사",
                "start": 38.72,
                "end": 42.72,
                "text": "내일 오후 2시나 4시 중에 가능하신 시간 있으신가요?"
            },
            {
                "speaker": "고객",
                "start": 42.72,
                "end": 44.72,
                "text": "오후 2시에 가겠습니다."
            },
            {
                "speaker": "상담사",
                "start": 44.72,
                "end": 48.72,
                "text": "네, 내일 2시로 예약 도와드리겠습니다. 성함이 어떻게 되시나요?"
            }
        ]
    }


def load_conversation_from_file(filepath: str) -> Dict[str, Any]:
    """
    JSON 파일에서 대화 데이터 로드
    
    Args:
        filepath: JSON 파일 경로
        
    Returns:
        대화 데이터
    """
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"❌ 파일을 찾을 수 없습니다: {filepath}")
        return None
    except json.JSONDecodeError as e:
        print(f"❌ JSON 파일 파싱 오류: {e}")
        return None


# 메인 실행 코드
if __name__ == "__main__":
    import argparse
    
    # 명령줄 인자 파서 설정
    parser = argparse.ArgumentParser(description='법률 상담 품질 평가 시스템')
    parser.add_argument('--host', type=str, default='localhost',
                       help='Ollama 서버 호스트 (기본값: localhost)')
    parser.add_argument('--port', type=int, default=11434,
                       help='Ollama 서버 포트 (기본값: 11434)')
    parser.add_argument('--file', type=str, default=None,
                       help='평가할 대화 JSON 파일 경로')
    parser.add_argument('--output', type=str, default='evaluation_result.json',
                       help='결과 저장 파일명 (기본값: evaluation_result.json)')
    
    args = parser.parse_args()
    
    print("="*60)
    print("🏛️  법률 상담 품질 평가 시스템")
    print("="*60)
    
    # 평가 시스템 초기화
    evaluator = LegalConsultationEvaluator(
        ollama_host=args.host,
        ollama_port=args.port
    )
    
    # 대화 데이터 로드
    if args.file:
        print(f"\n📂 파일에서 대화 데이터 로드 중: {args.file}")
        conversation_data = load_conversation_from_file(args.file)
        if not conversation_data:
            print("샘플 데이터를 사용합니다.")
            conversation_data = get_sample_conversation()
    else:
        print("\n📝 샘플 대화 데이터 사용")
        conversation_data = get_sample_conversation()
    
    print(f"✅ 대화 데이터 로드 완료!")
    print(f"   • 총 {len(conversation_data.get('segments', []))}개의 대화 세그먼트")
    print(f"   • 대화 시간: {conversation_data.get('duration', 0):.1f}초")
    
    # 평가 실행
    print("\n" + "="*60)
    print("🤖 AI 평가 시작...")
    print("="*60)
    
    start_time = time.time()
    evaluation_result = evaluator.evaluate(conversation_data)
    elapsed_time = time.time() - start_time
    
    if "error" not in evaluation_result:
        print(f"\n⏱️  평가 완료! (소요 시간: {elapsed_time:.1f}초)")
    
    # 결과 출력
    evaluator.print_evaluation(evaluation_result)
    
    # JSON 파일로 저장
    if "error" not in evaluation_result:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(evaluation_result, f, ensure_ascii=False, indent=2)
        print(f"\n💾 평가 결과가 '{args.output}'에 저장되었습니다.")
    else:
        print(f"\n⚠️  오류로 인해 결과를 저장하지 않았습니다.")
    
    print("\n프로그램을 종료합니다.")


# 사용 예시를 위한 추가 함수
def example_usage():
    """
    다양한 사용 예시
    """
    # 예시 1: 기본 사용
    evaluator = LegalConsultationEvaluator()
    
    # 예시 2: 원격 서버 지정
    evaluator = LegalConsultationEvaluator(
        ollama_host="192.168.1.100",  # 원격 서버 IP
        ollama_port=11434
    )
    
    # 예시 3: 도메인 이름 사용
    evaluator = LegalConsultationEvaluator(
        ollama_host="ollama.mycompany.com",
        ollama_port=11434
    )
    
    # 대화 데이터 평가
    conversation = get_sample_conversation()
    result = evaluator.evaluate(conversation)
    evaluator.print_evaluation(result)