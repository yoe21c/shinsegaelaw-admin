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

    def create_prompt(self, conversation_data: Dict[str, Any], evaluation_type: str = "comprehensive") -> str:
        """
        평가용 프롬프트 생성

        Args:
            conversation_data: 대화 내용 데이터
            evaluation_type: 평가 유형 (business_potential, expertise, communication, friendliness, comprehensive)

        Returns:
            완성된 프롬프트 문자열
        """
        base_prompt = "중요: 반드시 유효한 JSON 형식으로만 응답하세요. 설명이나 주석 없이 JSON만 제공하세요.\n\n"

        # 평가 유형별 프롬프트
        if evaluation_type == "business_potential":
            prompt = base_prompt + """당신은 대형 로펌의 대표입니다. 다음 상담 내용을 분석하여 수임 가능성과 매출 기여도를 평가해주세요.

평가 기준:
1. 고객의 사건 규모와 복잡성
2. 고객의 지불 능력 파악 여부
3. 수임 가능성을 높이는 상담사의 전략
4. 고객의 긴급성과 필요성 파악
5. 경쟁 로펌 대비 차별화 포인트 제시

필수 JSON 구조:
{
  "evaluation": {
    "business_score": [0-100 점수],
    "potential_revenue": "[예상 수임료 범위]",
    "conversion_probability": [0-100 퍼센트],
    "client_profile": {
      "urgency_level": "[높음/중간/낮음]",
      "case_complexity": "[복잡/보통/단순]",
      "payment_ability": "[상/중/하]",
      "loyalty_potential": "[높음/중간/낮음]"
    },
    "consultant_performance": {
      "needs_identification": [0-100],
      "value_proposition": [0-100],
      "closing_technique": [0-100],
      "follow_up_strategy": [0-100]
    },
    "critical_moments": [
      {
        "segment_index": [인덱스],
        "moment_type": "[기회/위기]",
        "description": "[설명]",
        "impact": "[긍정적/부정적]"
      }
    ],
    "recommendations": "[수임 전략 제안]"
  }
}"""

        elif evaluation_type == "expertise":
            prompt = base_prompt + """당신은 법률 전문가입니다. 상담사의 법률 전문성을 엄격하게 평가해주세요.

평가 기준:
1. 법률 용어 사용의 정확성
2. 관련 법령 및 판례 인용 여부
3. 법적 쟁점 파악 능력
4. 해결 방안의 구체성과 실현 가능성
5. 잘못된 법률 정보 제공 여부

필수 JSON 구조:
{
  "evaluation": {
    "expertise_score": [0-100 점수],
    "knowledge_areas": {
      "legal_terminology": [0-100],
      "case_law_knowledge": [0-100],
      "procedural_understanding": [0-100],
      "strategic_thinking": [0-100]
    },
    "professional_indicators": [
      {
        "segment_index": [인덱스],
        "indicator_type": "[전문성/비전문성]",
        "description": "[구체적 내용]",
        "severity": "[심각/중간/경미]"
      }
    ],
    "legal_errors": [
      {
        "segment_index": [인덱스],
        "error_description": "[오류 내용]",
        "correct_information": "[올바른 정보]",
        "risk_level": "[높음/중간/낮음]"
      }
    ],
    "expertise_gaps": "[보완 필요 영역]",
    "training_needs": "[교육 필요사항]"
  }
}"""

        elif evaluation_type == "communication":
            prompt = base_prompt + """커뮤니케이션 전문가 관점에서 상담사의 의사소통 명확성을 평가해주세요.

평가 기준:
1. 복잡한 법률 내용의 쉬운 설명
2. 고객 질문에 대한 직접적 답변
3. 정보의 구조화와 논리적 전달
4. 고객 이해도 확인 여부
5. 다음 단계 안내의 명확성

필수 JSON 구조:
{
  "evaluation": {
    "communication_score": [0-100 점수],
    "clarity_metrics": {
      "simplification_ability": [0-100],
      "directness": [0-100],
      "structure_logic": [0-100],
      "confirmation_checks": [0-100],
      "action_guidance": [0-100]
    },
    "communication_patterns": [
      {
        "segment_index": [인덱스],
        "pattern_type": "[우수/개선필요]",
        "description": "[구체적 내용]",
        "impact_on_understanding": "[높음/중간/낮음]"
      }
    ],
    "jargon_usage": [
      {
        "segment_index": [인덱스],
        "term_used": "[전문용어]",
        "explanation_provided": "[예/아니오]",
        "simplification_suggestion": "[쉬운 표현]"
      }
    ],
    "missed_clarifications": "[놓친 설명 기회]",
    "communication_improvements": "[개선 방안]"
  }
}"""

        elif evaluation_type == "friendliness":
            prompt = base_prompt + """고객 서비스 관점에서 상담사의 친절도와 관계 구축 능력을 평가해주세요.

평가 기준:
1. 공감과 이해 표현
2. 고객 감정 상태 파악과 대응
3. 적극적 경청 자세
4. 신뢰감 형성 노력
5. 장기적 관계 구축 가능성

필수 JSON 구조:
{
  "evaluation": {
    "friendliness_score": [0-100 점수],
    "relationship_metrics": {
      "empathy_level": [0-100],
      "emotional_intelligence": [0-100],
      "active_listening": [0-100],
      "trust_building": [0-100],
      "warmth_authenticity": [0-100]
    },
    "emotional_moments": [
      {
        "segment_index": [인덱스],
        "emotion_type": "[고객감정/상담사대응]",
        "description": "[구체적 상황]",
        "response_quality": "[우수/보통/미흡]"
      }
    ],
    "rapport_indicators": [
      {
        "segment_index": [인덱스],
        "indicator": "[지표 내용]",
        "strength": "[강함/중간/약함]"
      }
    ],
    "relationship_potential": "[장기 관계 가능성 평가]",
    "customer_retention_likelihood": [0-100]
  }
}"""

        else:  # comprehensive (기본값)
            prompt = base_prompt + """로펌 대표 관점에서 전체적인 상담 품질을 종합 평가해주세요.

필수 JSON 구조:
{
  "evaluation": {
    "total_score": [0-100],
    "summary_scores": {
      "business_potential": [0-100],
      "legal_expertise": [0-100],
      "communication_clarity": [0-100],
      "customer_friendliness": [0-100]
    },
    "executive_summary": "[대표 보고용 핵심 요약]",
    "strengths": ["[강점1]", "[강점2]"],
    "weaknesses": ["[약점1]", "[약점2]"],
    "action_items": ["[조치사항1]", "[조치사항2]"],
    "consultant_rating": "[S/A/B/C/D 등급]"
  }
}"""

        prompt += "\n\n대화 데이터:\n"
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

        # 프롬프트 생성 (종합 평가)
        prompt = self.create_prompt(conversation_data, "comprehensive")

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

    def evaluate_detailed(self, conversation_data: Dict[str, Any], max_retries: int = 2) -> Dict[str, Any]:
        """
        4가지 핵심 영역별 상세 평가 실행

        Args:
            conversation_data: 평가할 대화 데이터
            max_retries: 각 평가별 최대 재시도 횟수

        Returns:
            통합 평가 결과
        """
        # 연결 테스트
        if not self.test_connection():
            return {"error": "Ollama 서버 연결 실패"}

        evaluation_types = [
            ("business_potential", "💼 수임 가능성 및 매출 평가"),
            ("expertise", "🎓 법률 전문성 평가"),
            ("communication", "💬 명확한 의사소통 평가"),
            ("friendliness", "🤝 친절도 및 관계 구축 평가")
        ]

        detailed_results = {}

        for eval_type, eval_name in evaluation_types:
            print(f"\n{'='*70}")
            print(f"{eval_name}")
            print('='*70)

            # 각 평가 유형별 프롬프트 생성
            prompt = self.create_prompt(conversation_data, eval_type)

            # 재시도 로직
            for retry in range(max_retries):
                if retry > 0:
                    print(f"🔄 재시도 {retry}/{max_retries}...")

                # Ollama API 호출
                response = self.call_ollama(prompt, retry_count=retry)

                if not response:
                    if retry < max_retries - 1:
                        print("⏳ 3초 후 재시도...")
                        time.sleep(3)
                        continue
                    else:
                        detailed_results[eval_type] = {"error": "API 호출 실패"}
                        break

                # 평가 결과 파싱
                evaluation = self.parse_evaluation(response)

                if evaluation:
                    print(f"✅ {eval_name} 완료!")
                    detailed_results[eval_type] = evaluation.get("evaluation", evaluation)
                    break

                if retry < max_retries - 1:
                    print("⏳ 파싱 실패, 3초 후 재시도...")
                    time.sleep(3)
                else:
                    detailed_results[eval_type] = {"error": "평가 파싱 실패"}

            # API 부하 방지를 위한 대기
            time.sleep(2)

        # 종합 평가 실행
        print(f"\n{'='*70}")
        print("📊 종합 평가")
        print('='*70)

        comprehensive_prompt = self.create_prompt(conversation_data, "comprehensive")
        comprehensive_result = None

        for retry in range(max_retries):
            if retry > 0:
                print(f"🔄 재시도 {retry}/{max_retries}...")

            response = self.call_ollama(comprehensive_prompt, retry_count=retry)

            if response:
                evaluation = self.parse_evaluation(response)
                if evaluation:
                    comprehensive_result = evaluation.get("evaluation", evaluation)
                    print("✅ 종합 평가 완료!")
                    break

            if retry < max_retries - 1:
                time.sleep(3)

        # 최종 결과 통합
        return {
            "detailed_evaluations": detailed_results,
            "comprehensive_evaluation": comprehensive_result or {"error": "종합 평가 실패"},
            "evaluation_timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "conversation_info": {
                "duration": conversation_data.get("duration", 0),
                "segments_count": len(conversation_data.get("segments", []))
            }
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

    def print_detailed_evaluation(self, evaluation: Dict[str, Any]):
        """
        상세 평가 결과를 로펌 대표를 위해 출력

        Args:
            evaluation: 상세 평가 결과
        """
        if "error" in evaluation:
            print(f"\n❌ 오류 발생: {evaluation['error']}")
            return

        print("\n" + "="*80)
        print("🏢 로펌 대표 보고서 - 상담 품질 상세 평가")
        print("="*80)

        # 상담 정보
        info = evaluation.get("conversation_info", {})
        print(f"\n📋 상담 정보:")
        print(f"  • 상담 시간: {info.get('duration', 0):.1f}초")
        print(f"  • 대화 수: {info.get('segments_count', 0)}개")
        print(f"  • 평가 일시: {evaluation.get('evaluation_timestamp', 'N/A')}")

        detailed = evaluation.get("detailed_evaluations", {})

        # 1. 수임 가능성 평가
        if "business_potential" in detailed:
            business = detailed["business_potential"]
            if "error" not in business:
                print(f"\n{'='*80}")
                print("💼 1. 수임 가능성 및 매출 평가")
                print("-"*80)
                print(f"  📊 비즈니스 점수: {business.get('business_score', 'N/A')}/100점")
                print(f"  💰 예상 수임료: {business.get('potential_revenue', 'N/A')}")
                print(f"  📈 수임 확률: {business.get('conversion_probability', 'N/A')}%")

                profile = business.get("client_profile", {})
                if profile:
                    print(f"\n  고객 프로필:")
                    print(f"    • 긴급성: {profile.get('urgency_level', 'N/A')}")
                    print(f"    • 사건 복잡도: {profile.get('case_complexity', 'N/A')}")
                    print(f"    • 지불 능력: {profile.get('payment_ability', 'N/A')}")
                    print(f"    • 충성도 가능성: {profile.get('loyalty_potential', 'N/A')}")

                perf = business.get("consultant_performance", {})
                if perf:
                    print(f"\n  상담사 영업 성과:")
                    print(f"    • 니즈 파악: {perf.get('needs_identification', 'N/A')}/100")
                    print(f"    • 가치 제안: {perf.get('value_proposition', 'N/A')}/100")
                    print(f"    • 클로징 기법: {perf.get('closing_technique', 'N/A')}/100")
                    print(f"    • 후속 전략: {perf.get('follow_up_strategy', 'N/A')}/100")

                if business.get("recommendations"):
                    print(f"\n  💡 수임 전략 제안:")
                    print(f"    {business['recommendations']}")

        # 2. 전문성 평가
        if "expertise" in detailed:
            expertise = detailed["expertise"]
            if "error" not in expertise:
                print(f"\n{'='*80}")
                print("🎓 2. 법률 전문성 평가")
                print("-"*80)
                print(f"  📊 전문성 점수: {expertise.get('expertise_score', 'N/A')}/100점")

                knowledge = expertise.get("knowledge_areas", {})
                if knowledge:
                    print(f"\n  지식 영역 평가:")
                    print(f"    • 법률 용어: {knowledge.get('legal_terminology', 'N/A')}/100")
                    print(f"    • 판례 지식: {knowledge.get('case_law_knowledge', 'N/A')}/100")
                    print(f"    • 절차 이해: {knowledge.get('procedural_understanding', 'N/A')}/100")
                    print(f"    • 전략적 사고: {knowledge.get('strategic_thinking', 'N/A')}/100")

                errors = expertise.get("legal_errors", [])
                if errors:
                    print(f"\n  ⚠️ 법률 오류 발견:")
                    for error in errors[:3]:  # 최대 3개만 표시
                        print(f"    • 대화 #{error.get('segment_index', 'N/A')}: {error.get('error_description', 'N/A')}")
                        print(f"      위험도: {error.get('risk_level', 'N/A')}")

                if expertise.get("training_needs"):
                    print(f"\n  📚 교육 필요사항:")
                    print(f"    {expertise['training_needs']}")

        # 3. 의사소통 평가
        if "communication" in detailed:
            comm = detailed["communication"]
            if "error" not in comm:
                print(f"\n{'='*80}")
                print("💬 3. 명확한 의사소통 평가")
                print("-"*80)
                print(f"  📊 의사소통 점수: {comm.get('communication_score', 'N/A')}/100점")

                clarity = comm.get("clarity_metrics", {})
                if clarity:
                    print(f"\n  명확성 지표:")
                    print(f"    • 단순화 능력: {clarity.get('simplification_ability', 'N/A')}/100")
                    print(f"    • 직접성: {clarity.get('directness', 'N/A')}/100")
                    print(f"    • 논리 구조: {clarity.get('structure_logic', 'N/A')}/100")
                    print(f"    • 확인 체크: {clarity.get('confirmation_checks', 'N/A')}/100")
                    print(f"    • 행동 안내: {clarity.get('action_guidance', 'N/A')}/100")

                if comm.get("communication_improvements"):
                    print(f"\n  💡 의사소통 개선 방안:")
                    print(f"    {comm['communication_improvements']}")

        # 4. 친절도 평가
        if "friendliness" in detailed:
            friend = detailed["friendliness"]
            if "error" not in friend:
                print(f"\n{'='*80}")
                print("🤝 4. 친절도 및 관계 구축 평가")
                print("-"*80)
                print(f"  📊 친절도 점수: {friend.get('friendliness_score', 'N/A')}/100점")
                print(f"  🔄 고객 유지 가능성: {friend.get('customer_retention_likelihood', 'N/A')}%")

                relation = friend.get("relationship_metrics", {})
                if relation:
                    print(f"\n  관계 지표:")
                    print(f"    • 공감 수준: {relation.get('empathy_level', 'N/A')}/100")
                    print(f"    • 감정 지능: {relation.get('emotional_intelligence', 'N/A')}/100")
                    print(f"    • 적극적 경청: {relation.get('active_listening', 'N/A')}/100")
                    print(f"    • 신뢰 구축: {relation.get('trust_building', 'N/A')}/100")
                    print(f"    • 따뜻함/진정성: {relation.get('warmth_authenticity', 'N/A')}/100")

                if friend.get("relationship_potential"):
                    print(f"\n  💡 장기 관계 평가:")
                    print(f"    {friend['relationship_potential']}")

        # 종합 평가
        comprehensive = evaluation.get("comprehensive_evaluation", {})
        if comprehensive and "error" not in comprehensive:
            print(f"\n{'='*80}")
            print("📊 종합 평가")
            print("-"*80)

            scores = comprehensive.get("summary_scores", {})
            if scores:
                print(f"\n  핵심 점수:")
                print(f"    • 수임 가능성: {scores.get('business_potential', 'N/A')}/100")
                print(f"    • 법률 전문성: {scores.get('legal_expertise', 'N/A')}/100")
                print(f"    • 의사소통: {scores.get('communication_clarity', 'N/A')}/100")
                print(f"    • 고객 친화: {scores.get('customer_friendliness', 'N/A')}/100")

            print(f"\n  🏆 상담사 등급: {comprehensive.get('consultant_rating', 'N/A')}")
            print(f"\n  💼 대표 보고 요약:")
            print(f"    {comprehensive.get('executive_summary', 'N/A')}")

            if comprehensive.get("action_items"):
                print(f"\n  ⚡ 즉시 조치사항:")
                for item in comprehensive.get("action_items", []):
                    print(f"    • {item}")

        print("\n" + "="*80)
        print("보고서 끝")
        print("="*80)


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
    parser.add_argument('--mode', type=str, default='detailed',
                       choices=['simple', 'detailed'],
                       help='평가 모드 - simple: 간단평가, detailed: 상세평가 (기본값: detailed)')

    args = parser.parse_args()

    print("="*80)
    print("🏛️  법률 상담 품질 평가 시스템 - 로펌 대표용")
    print("="*80)

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

    # 평가 모드 선택
    if args.mode == 'detailed':
        print("\n" + "="*80)
        print("🔍 상세 평가 모드 - 4가지 핵심 영역 개별 분석")
        print("="*80)
        print("평가 영역:")
        print("  1. 수임 가능성 및 매출 평가")
        print("  2. 법률 전문성 평가")
        print("  3. 명확한 의사소통 평가")
        print("  4. 친절도 및 관계 구축 평가")
        print("="*80)

        start_time = time.time()
        evaluation_result = evaluator.evaluate_detailed(conversation_data)
        elapsed_time = time.time() - start_time

        if "error" not in evaluation_result:
            print(f"\n⏱️  모든 평가 완료! (총 소요 시간: {elapsed_time:.1f}초)")

        # 상세 결과 출력
        evaluator.print_detailed_evaluation(evaluation_result)

    else:
        print("\n" + "="*60)
        print("🤖 간단 평가 모드")
        print("="*60)

        start_time = time.time()
        evaluation_result = evaluator.evaluate(conversation_data)
        elapsed_time = time.time() - start_time

        if "error" not in evaluation_result:
            print(f"\n⏱️  평가 완료! (소요 시간: {elapsed_time:.1f}초)")

        # 간단 결과 출력
        evaluator.print_evaluation(evaluation_result)

    # JSON 파일로 저장
    if "error" not in evaluation_result:
        # 상세 모드일 경우 파일명 수정
        if args.mode == 'detailed':
            output_file = args.output.replace('.json', '_detailed.json')
        else:
            output_file = args.output

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(evaluation_result, f, ensure_ascii=False, indent=2)
        print(f"\n💾 평가 결과가 '{output_file}'에 저장되었습니다.")

        # 대표 보고용 요약 파일 생성 (상세 모드만)
        if args.mode == 'detailed':
            summary_file = args.output.replace('.json', '_executive_summary.txt')
            with open(summary_file, "w", encoding="utf-8") as f:
                f.write("="*80 + "\n")
                f.write("로펌 대표 보고서 - 상담 품질 평가 요약\n")
                f.write("="*80 + "\n\n")

                # 종합 평가 요약
                comp = evaluation_result.get("comprehensive_evaluation", {})
                if comp and "error" not in comp:
                    f.write(f"상담사 등급: {comp.get('consultant_rating', 'N/A')}\n\n")
                    f.write(f"대표 요약:\n{comp.get('executive_summary', 'N/A')}\n\n")

                    scores = comp.get("summary_scores", {})
                    if scores:
                        f.write("핵심 점수:\n")
                        f.write(f"  • 수임 가능성: {scores.get('business_potential', 0)}/100\n")
                        f.write(f"  • 법률 전문성: {scores.get('legal_expertise', 0)}/100\n")
                        f.write(f"  • 의사소통: {scores.get('communication_clarity', 0)}/100\n")
                        f.write(f"  • 고객 친화: {scores.get('customer_friendliness', 0)}/100\n\n")

                    if comp.get("action_items"):
                        f.write("즉시 조치사항:\n")
                        for item in comp.get("action_items", []):
                            f.write(f"  • {item}\n")

                f.write("\n" + "="*80 + "\n")
            print(f"📋 대표 보고 요약이 '{summary_file}'에 저장되었습니다.")
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