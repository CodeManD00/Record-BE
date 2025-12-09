# ============================================================================
# Record Project - Backend Makefile
# ============================================================================
#
# Code를 만들어 낼 수 있는 방법 및 Script 포함
# - Backend 빌드: make be-build
#
# ============================================================================

.PHONY: help be-build be-run be-test be-clean

# 기본 타겟
help:
	@echo "Record Project - Backend Makefile"
	@echo ""
	@echo "Backend 명령어:"
	@echo "  make be-build       - Backend 빌드"
	@echo "  make be-run         - Backend 실행"
	@echo "  make be-test        - Backend 테스트"
	@echo "  make be-clean       - Backend 정리"
	@echo ""
	@echo "  make help           - 이 도움말 표시"

# ============================================
# Backend 명령어
# ============================================

# Backend 의존성 다운로드
be-install:
	@echo "📦 Backend 의존성 다운로드 중..."
	@cd Record-BE/Record-BE && ./gradlew dependencies

# Backend 빌드
be-build:
	@echo "🔨 Backend 빌드 중..."
	@cd Record-BE/Record-BE && ./gradlew build

# Backend 실행
be-run:
	@echo "🚀 Backend 실행 중..."
	@cd Record-BE/Record-BE && ./gradlew bootRun

# Backend 테스트
be-test:
	@echo "🧪 Backend 테스트 실행 중..."
	@cd Record-BE/Record-BE && ./gradlew test

# Backend 정리
be-clean:
	@echo "🧹 Backend 빌드 산출물 정리 중..."
	@cd Record-BE/Record-BE && ./gradlew clean

# Backend JAR 생성
be-jar: be-build
	@echo "📦 Backend JAR 파일 생성 완료: Record-BE/Record-BE/build/libs/Record-BE-0.0.1-SNAPSHOT.jar"

# ============================================
# 전체 정리
# ============================================

# 전체 정리
clean: be-clean
	@echo "✅ Backend 정리 완료"
