"""
API v1 라우터 모듈.
"""
from fastapi import APIRouter

from app.api.v1 import analysis

api_router = APIRouter()
api_router.include_router(analysis.router, prefix="/analysis", tags=["분석"])
