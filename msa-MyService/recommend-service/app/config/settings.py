"""
설정 우선순위: 컨테이너 환경변수(docker-compose) > .env > 아래 기본값

.env 는 커밋하지 않는다. 새로 받은 사람은 .env.example 을 복사해서 쓴다.
"""

from datetime import date
from typing import Optional

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # --- 서버 ---
    app_port: int = 8085
    app_name: str = "recommend-service"

    # --- Eureka ---
    eureka_server_url: str = "http://eureka-server:8761/eureka"
    eureka_instance_host: str = "recommend-service"

    # --- Auth Server (JWT 검증) ---
    jwt_issuer_uri: str = "http://localhost:8080"
    jwk_set_uri: str = "http://auth-server:9000/oauth2/jwks"

    # --- 의존 서비스 ---
    # mock_mode=true 면 아래 URL 을 호출하지 않고 app/data/mock_catalog.py 를 쓴다.
    mock_mode: bool = True
    material_service_url: str = "http://material-service:8086"
    order_service_url: str = "http://order-service:8087"

    # --- 수요 예측 ---
    # llm  = OpenAI Responses API + web_search (openai 패키지와 API 키가 있어야 한다)
    # mock = 합성 데이터 추세(SYNTHETIC_TREND). 외부 호출 없음
    # llm 으로 두어도 키가 없거나 호출이 실패하면 자동으로 mock 으로 폴백한다.
    forecaster: str = "mock"
    openai_api_key: Optional[str] = None
    openai_model: str = "gpt-4o"
    openai_timeout: float = 10.0
    forecast_cache_ttl_hours: int = 6
    forecast_cache_dir: str = ".forecast-cache"

    # --- 소진 시뮬레이션 ---
    lead_time_days: int = 30
    forecast_default_days: int = 90
    factor_recent_weeks: int = 12
    recommend_max_count: int = 5

    # --- 데모용 ---
    # 합성 데이터 구간(2025-07-14 ~ 2026-08-16) 안의 날짜를 기준일로 고정한다.
    # 오늘 날짜가 구간 밖이면 최근 12주가 통째로 비어 예측이 무의미해진다.
    #
    # 2026-03-12 을 쓰는 이유:
    #  - 90일 비교 구간 두 개(180일)가 모두 데이터 안에 들어와야 증감률이 나온다
    #    (데이터 시작 2025-07-14 + 180일 = 2026-01-10 이후)
    #  - 한겨울(1~2월)은 합성 수요가 평상시의 2~3배까지 튀어 90일치 발주량이
    #    공장 하나의 여유 생산능력을 넘어선다. 환절기가 "수요 증가 + 공급 가능"이
    #    같이 성립하는 구간이다.
    demo_as_of: Optional[date] = date(2026, 3, 12)
    # 합성 데이터에는 신뢰도를 계산할 근거가 없다. 화면에 "-"가 남지 않도록 고정값을 쓴다.
    mock_confidence: Optional[float] = 0.72

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
