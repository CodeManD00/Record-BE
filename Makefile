# Record Project - 통합 Makefile

.PHONY: help be-install be-build be-run be-test be-clean be-jar fe-install fe-install-ios fe-start fe-android fe-ios fe-test fe-lint fe-clean fe-clean-cache fe-pod-install clean

help:
	@echo "Record Project Makefile"
	@echo ""
	@echo "=== 설치하기 ==="
	@echo "Backend:"
	@echo "  make be-install     - Backend 의존성 설치 및 빌드 준비"
	@echo ""
	@echo "Frontend:"
	@echo "  make fe-install     - Frontend 의존성 설치 (npm install)"
	@echo "  make fe-install-ios - iOS Pods 설치 (macOS만)"
	@echo ""
	@echo "=== 빌드하기 ==="
	@echo "Backend:"
	@echo "  make be-build       - Backend 빌드"
	@echo ""
	@echo "=== 실행하기 ==="
	@echo "Backend:"
	@echo "  make be-run         - Backend 실행"
	@echo ""
	@echo "Frontend:"
	@echo "  make fe-start       - Metro 개발 서버 실행"
	@echo "  make fe-ios         - iOS 앱 실행 (빌드 포함, macOS만)"
	@echo "  make fe-android     - Android 앱 실행"
	@echo ""
	@echo "=== 테스트하기 ==="
	@echo "Backend:"
	@echo "  make be-test        - Backend 테스트"
	@echo ""
	@echo "Frontend:"
	@echo "  make fe-test        - Frontend 테스트"
	@echo ""
	@echo "=== 기타 ==="
	@echo "Backend:"
	@echo "  make be-clean       - Backend 빌드 산출물 정리"
	@echo "  make be-jar         - JAR 파일 생성"
	@echo ""
	@echo "Frontend:"
	@echo "  make fe-lint        - Frontend 린트 검사"
	@echo "  make fe-clean       - Frontend 정리"
	@echo "  make fe-clean-cache - Frontend 캐시만 정리"
	@echo "  make fe-pod-install - iOS Pods 재설치 (macOS만)"
	@echo ""
	@echo "전체:"
	@echo "  make clean          - 전체 프로젝트 정리"

# Backend 명령어
be-install:
	@echo "📦 Backend 의존성 캐시 준비 중..."
	@cd Record-BE/Record-BE && ./gradlew build -x test

be-build:
	@echo "🔨 Backend 빌드 중..."
	@cd Record-BE/Record-BE && ./gradlew build

be-run:
	@echo "🚀 Backend 실행 중..."
	@cd Record-BE/Record-BE && ./gradlew bootRun

be-test:
	@echo "🧪 Backend 테스트 실행 중..."
	@cd Record-BE/Record-BE && ./gradlew test

be-clean:
	@echo "🧹 Backend 빌드 산출물 정리 중..."
	@cd Record-BE/Record-BE && ./gradlew clean

be-jar: be-build
	@echo "📦 Backend JAR 파일:"
	@ls -lh Record-BE/Record-BE/build/libs/*.jar 2>/dev/null || echo "JAR 파일을 찾을 수 없습니다."

# Frontend 명령어
fe-install:
	@echo "📦 Frontend 의존성 설치 중..."
	@cd Record-FE && npm install
	@echo "✅ Frontend 의존성 설치 완료"

fe-install-ios:
	@echo "📦 Frontend iOS Pods 설치 중..."
	@if [ "$$(uname)" != "Darwin" ]; then \
		echo "❌ iOS Pods는 macOS에서만 설치할 수 있습니다."; \
		exit 1; \
	fi
	@cd Record-FE/ios && bundle exec pod install && cd ../..
	@echo "✅ Frontend iOS Pods 설치 완료"

fe-start:
	@echo "🚀 Frontend Metro 번들러 시작 중..."
	@cd Record-FE && npm start

fe-android:
	@echo "🤖 Frontend Android 앱 빌드 및 실행 중..."
	@cd Record-FE && npm run android

fe-ios:
	@echo "🍎 Frontend iOS 앱 빌드 및 실행 중..."
	@if [ "$$(uname)" != "Darwin" ]; then \
		echo "❌ iOS 빌드는 macOS에서만 가능합니다."; \
		exit 1; \
	fi
	@cd Record-FE && npm run ios

fe-test:
	@echo "🧪 Frontend 테스트 실행 중..."
	@cd Record-FE && npm test

fe-lint:
	@echo "🔍 Frontend ESLint 검사 중..."
	@cd Record-FE && npm run lint

fe-clean:
	@echo "🧹 Frontend 빌드 산출물 및 캐시 정리 중..."
	@cd Record-FE && rm -rf node_modules ios/Pods ios/build android/build android/app/build
	@echo "✅ Frontend 정리 완료"

fe-clean-cache:
	@echo "🧹 Frontend 캐시만 정리 중..."
	@rm -rf $$TMPDIR/react-* $$TMPDIR/metro-* $$TMPDIR/haste-*
	@echo "✅ Frontend 캐시 정리 완료"

fe-pod-install:
	@echo "📦 Frontend iOS Pods 재설치 중..."
	@if [ "$$(uname)" = "Darwin" ]; then \
		cd Record-FE/ios && pod deintegrate && pod install && cd ../..; \
		echo "✅ Frontend iOS Pods 재설치 완료"; \
	else \
		echo "❌ iOS Pods는 macOS에서만 설치할 수 있습니다."; \
	fi

# 전체 정리
clean: be-clean fe-clean
	@echo "✅ 전체 프로젝트 정리 완료"
