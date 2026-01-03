**1단계: Feature 브랜치 생성**

1. develop 브랜치로 이동
   > `git checkout develop`

2. 최신 상태로 업데이트
   > `git pull origin develop`

3. feature 브랜치 생성 및 이동
   > `git checkout -b feature/기능명`

**2단계: 작업 진행**

1. 코드 작성 및 수정
   > 기능 개발 진행...

2. 변경사항 확인
   > `git status`

3. 변경사항 스테이징
   > `git add .`  
   > 또는 특정 파일만: `git add 파일명`

4. 커밋
   > `git commit -m "feat: 기능 설명"`

5. 작업 중 develop 최신 변경사항 반영 (선택사항)
   > `git pull origin develop`  
   > 충돌 발생 시 해결 후 다시 커밋

**3단계: 원격 저장소에 푸시**

1. feature 브랜치를 원격에 푸시
   > `git push origin feature/기능명`

2. 처음 푸시하는 경우
   > `git push -u origin feature/기능명`

**4단계: Pull Request 생성**

1. GitHub 저장소 접속

2. 'Pull requests' 탭 클릭

3. 'New pull request' 버튼 클릭

4. PR 설정
    - Base: `develop`
    - Compare: `feature/기능명`

5. PR 작성
    - 제목: 명확한 기능 설명
    - 설명: 변경 내용 등 상세 작성
    - 리뷰어 지정

6. 'Create pull request' 클릭

**5단계: 코드 리뷰 및 수정**

1. 리뷰어의 피드백 확인

2. 수정사항이 있는 경우
   > 코드 수정  
   > `git add .`  
   > `git commit -m "fix: 리뷰 반영 내용"`  
   > `git push origin feature/기능명`

3. 리뷰 승인 대기

**6단계: Merge 및 브랜치 정리**

1. PR이 승인되면 GitHub에서 'Merge pull request' 클릭

2. Merge 완료 확인

3. 로컬 develop 브랜치로 이동
   > `git checkout develop`

4. 최신 상태로 업데이트
   > `git pull origin develop`

5. 로컬 feature 브랜치 삭제
   > `git branch -d feature/기능명`

6. 원격 feature 브랜치 삭제
   > `git push origin --delete feature/기능명`  
  

