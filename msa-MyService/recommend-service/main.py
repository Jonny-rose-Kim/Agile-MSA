import logging
from contextlib import asynccontextmanager

import py_eureka_client.eureka_client as eureka_client
from fastapi import FastAPI

from app.config.settings import settings
from app.router import recommend_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(f"[{settings.app_name}] 서비스 시작 "
                f"(forecaster={settings.forecaster}, mock_mode={settings.mock_mode})")

    try:
        await eureka_client.init_async(
            eureka_server=settings.eureka_server_url,
            app_name=settings.app_name,
            instance_port=settings.app_port,
            instance_host=settings.eureka_instance_host,
        )
        logger.info("[Eureka] 서비스 등록 완료")
    except Exception as e:
        # Eureka 가 없어도 서비스 자체는 떠야 한다 (로컬 단독 실행)
        logger.warning(f"[Eureka] 등록 실패 (개발 환경에서 무시 가능): {e}")

    yield

    logger.info(f"[{settings.app_name}] 서비스 종료")
    try:
        await eureka_client.stop_async()
    except Exception:
        pass


app = FastAPI(
    title="Recommend Service",
    description="원료의약품 수급 매칭 플랫폼 — AI 수요 예측 · 소진 시뮬레이션 · 공급사 추천",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(recommend_router.router)


@app.get("/health")
async def health():
    return {"status": "UP", "service": settings.app_name}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=settings.app_port, reload=True)
