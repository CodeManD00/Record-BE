# ============================================================================
# Record Project - 통합 Makefile
# ============================================================================
#
# Code를 만들어 낼 수 있는 방법 및 Script 포함
# - Backend 빌드: make be-build
# - Frontend 빌드: make fe-install
# - 전체 빌드: make build-all
#
# ============================================================================

.PHONY: help build-all test-all install-all be-build be-run be-test fe-install fe-android fe-ios fe-test clean

# 기본 타겟
help:
	@echo "Record Project - 통합 Makefile"
	@echo ""
	@echo "전체 프로젝트 명령어:"
	@echo "  make build-all      - Backend + Frontend 모두 빌드"
	@echo "  make test-all       - Backend + Frontend 모두 테스트"
	@echo "  make install-all    - Backend + Frontend 모두 설치"
	@echo ""
	@echo "Backend 명령어:"
	@echo "  make be-build       - Backend 빌드"
	@echo "  make be-run         - Backend 실행"
	@echo "  make be-test        - Backend 테스트"
	@echo "  make be-clean       - Backend 정리"
	@echo ""
	@echo "Frontend 명령어:"
	@echo "  make fe-install     - Frontend 의존성 설치"
	@echo "  make fe-start        - Frontend Metro 번들러 시작"
	@echo "  make fe-android     - Frontend Android 실행"
	@echo "  make fe-ios         - Frontend iOS 실행 (macOS만)"
	@echo "  make fe-test        - Frontend 테스트"
	@echo "  make fe-lint        - Frontend 린트 검사"
	@echo "  make fe-clean       - Frontend 정리"
	@echo ""
	@echo "  make help           - 이 도움말 표시"

# 전체 프로젝트 빌드
build-all: be-build fe-install
	@echo "✅ 전체 프로젝트 빌드 완료"

# 전체 프로젝트 테스트
test-all: be-test fe-test
	@echo "✅ 전체 프로젝트 테스트 완료"

# 전체 프로젝트 설치
install-all: be-install fe-install
	@echo "✅ 전체 프로젝트 설치 완료"

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
# Frontend 명령어
# ============================================

# Frontend 의존성 설치
fe-install:
	@echo "📦 Frontend 의존성 설치 중..."
	@cd Record-FE && npm install
	@echo "📦 iOS 의존성 설치 중 (macOS만)..."
	@if [ "$$(uname)" = "Darwin" ]; then \
		cd Record-FE/ios && bundle exec pod install && cd ../..; \
	fi
	@echo "✅ Frontend 의존성 설치 완료"

# Frontend Metro 번들러 시작
fe-start:
	@echo "🚀 Frontend Metro 번들러 시작 중..."
	@cd Record-FE && npm start

# Frontend Android 실행
fe-android:
	@echo "🤖 Frontend Android 앱 빌드 및 실행 중..."
	@cd Record-FE && npm run android

# Frontend iOS 실행 (macOS만)
fe-ios:
	@echo "🍎 Frontend iOS 앱 빌드 및 실행 중..."
	@if [ "$$(uname)" != "Darwin" ]; then \
		echo "❌ iOS 빌드는 macOS에서만 가능합니다."; \
		exit 1; \
	fi
	@cd Record-FE && npm run ios

# Frontend 테스트
fe-test:
	@echo "🧪 Frontend 테스트 실행 중..."
	@cd Record-FE && npm test

# Frontend 린트 검사
fe-lint:
	@echo "🔍 Frontend ESLint 검사 중..."
	@cd Record-FE && npm run lint

# Frontend 정리
fe-clean:
	@echo "🧹 Frontend 빌드 산출물 및 캐시 정리 중..."
	@cd Record-FE && rm -rf node_modules
	@cd Record-FE && rm -rf ios/Pods
	@cd Record-FE && rm -rf ios/build
	@cd Record-FE && rm -rf android/build
	@cd Record-FE && rm -rf android/app/build
	@echo "✅ Frontend 정리 완료"

# Frontend 캐시만 정리
fe-clean-cache:
	@echo "🧹 Frontend 캐시만 정리 중..."
	@rm -rf $$TMPDIR/react-*
	@rm -rf $$TMPDIR/metro-*
	@rm -rf $$TMPDIR/haste-*
	@echo "✅ Frontend 캐시 정리 완료"

# Frontend iOS Pods 재설치
fe-pod-install:
	@echo "📦 Frontend iOS Pods 재설치 중..."
	@if [ "$$(uname)" = "Darwin" ]; then \
		cd Record-FE/ios && pod deintegrate && pod install && cd ../..; \
	else \
		echo "❌ iOS Pods는 macOS에서만 설치할 수 있습니다."; \
	fi

# ============================================
# 전체 정리
# ============================================

# 전체 정리
clean: be-clean fe-clean
	@echo "✅ 전체 프로젝트 정리 완료"
