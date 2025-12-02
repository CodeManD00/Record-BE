#!/bin/bash
# sxxm과 seayun9845의 친구 추가 SQL 실행 스크립트

# 비밀번호를 환경 변수로 설정하거나, 아래 명령어 실행 시 비밀번호를 입력하세요
# PGPASSWORD=your_password psql -h record-db.czsw4i6quunb.ap-northeast-2.rds.amazonaws.com -U recorduser -d recorddb -f add-friends-for-users.sql

# 또는 직접 실행:
psql -h record-db.czsw4i6quunb.ap-northeast-2.rds.amazonaws.com \
     -U recorduser \
     -d recorddb \
     -f add-friends-for-users.sql

