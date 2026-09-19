#!/usr/bin/env bash
set -e

echo "=========================================="
echo "DongBang EC2 초기 인프라 설정 시작"
echo "=========================================="

# 1. 시스템 패키지 업데이트
echo "[1/4] 시스템 패키지 업데이트 중..."
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# 2. 2GB Swap 메모리 세팅 (t4g.small OOM 방지 필수)
echo "[2/4] 2GB Swap 메모리 구성 중..."
if [ ! -f /swapfile ]; then
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
    echo "Swap 2GB 설정 완료."
else
    echo "Swapfile이 이미 존재합니다. 건너뜁니다."
fi

# 3. Docker 및 Docker Compose V2 설치
echo "[3/4] Docker Engine 및 Docker Compose 설치 중..."
sudo apt-get install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu

# 4. 배포 디렉터리 생성
echo "[4/4] 배포 디렉터리 생성..."
sudo install -d -o ubuntu -g ubuntu /home/ubuntu/dongbang

echo "=========================================="
echo "설정 완료! 아래 정보를 확인하세요:"
echo "------------------------------------------"
free -h
echo "------------------------------------------"
docker --version
docker compose version
echo "=========================================="
echo "※ 도커 그룹 권한 적용을 위해 터미널을 다시 접속(재로그인)하거나 'newgrp docker'를 실행하세요."
echo "※ EC2에 S3 PutObject/GetObject/DeleteObject 권한이 있는 IAM Role을 연결하세요."
echo "※ 첫 CD 실행 후 /home/ubuntu/dongbang/.env.prod.example을 .env로 복사하고 실제 값을 입력하세요."
