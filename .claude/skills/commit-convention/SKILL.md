---
name: commit-convention
description: SKALA-FUND 커밋 규칙 — '{feature}: {작업 내용, 명사형 종결어미}' 형식, 작업 단위 분할 기준, 커밋 금지 대상(Gradle 배포판·node_modules·빌드 산출물·시크릿) 체크. 기능 개발을 마치고 커밋하기 전에 로드한다.
---

# 커밋 규칙

## 1. 메시지 형식

```
{feature}: {작업 내용, 명사형 종결어미}
```

- 제목은 **한국어 명사형으로 종결**합니다. `~구현`, `~추가`, `~설정`, `~정비`, `~수정`, `~보강`.
- `~했다`, `~하기`, `~함`, 영어 동사 원형(`Add`, `Fix`)을 쓰지 않습니다.
- 제목 끝에 마침표를 찍지 않습니다.
- 본문은 필요할 때만 씁니다. 쓴다면 제목과 빈 줄로 분리하고 `-` 불릿으로 **무엇을 왜** 바꿨는지 적습니다. 파일 목록을 나열하지 않습니다(diff 가 이미 보여줍니다).

### feature 프리픽스

| 프리픽스 | 쓰는 경우 |
|---|---|
| `feat` | 사용자에게 보이는 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `test` | 테스트/하네스 추가·수정 |
| `chore` | 빌드 설정, 의존성, gitignore, 도구 설정 |
| `docs` | 문서 |
| `style` | CSS/포맷팅 등 로직 무관 변경 |

### 예시

```
✅ feat: 동시성 락 후원 처리 및 정산 배치 서비스 구현
✅ feat: Spring Security JWT 인증 필터 및 인증 API 구축
✅ fix: 후원 취소 시 프로젝트 모금액 미차감 문제 수정
✅ test: 정산 포인트 총량 보존 불변식 하네스 추가
✅ chore: Gradle Wrapper 정상화 및 저장소 위생 정비

❌ feat: 후원 기능을 추가했다          (종결어미 위반)
❌ feat: add pledge API                (영어 동사)
❌ update                              (프리픽스·내용 없음)
❌ feat: 이것저것 수정                  (내용 불명)
```

---

## 2. 작업 단위 분할

**한 커밋 = 한 작업 단위.** 리뷰어가 제목만 보고 무엇이 들어있는지 예측할 수 있어야 합니다.

분할 기준:

- 명세 항목(`BE-07`, `FE-06`, `INF-04`) 하나가 대체로 커밋 하나입니다.
- 서로 다른 파트(BE / FE / INF)를 한 커밋에 섞지 않습니다.
- 리팩터링과 기능 추가를 한 커밋에 섞지 않습니다. 순서를 나눠 `refactor` → `feat` 로 커밋합니다.
- 반대로 **컴파일이 깨진 상태로 커밋하지 않습니다.** 엔티티 변경과 그에 딸린 서비스 수정처럼 함께여야 빌드가 되는 것은 한 커밋입니다.

---

## 3. 커밋 전 필수 확인

### ① 빌드 통과

```bash
cd backend && ./gradlew build     # 백엔드를 건드렸으면
cd frontend && npm run build      # 프론트를 건드렸으면
```

빌드가 깨진 상태로 커밋하지 않습니다.

### ② 스테이징 대상 점검

```bash
git status --short
git diff --cached --stat
```

**`git add .` / `git add -A` 를 습관적으로 쓰지 마세요.** 이 저장소에는 커밋되면 안 되는 것들이 워킹트리에 존재합니다.

절대 커밋 금지:

| 대상 | 이유 |
|---|---|
| `gradle/gradle-*/` | 로컬에 풀어둔 Gradle 배포판 전체(수백 MB). 빌드는 `backend/gradlew` Wrapper 로만 한다 |
| `node_modules/`, `dist/` | 의존성·빌드 산출물 |
| `backend/build/`, `backend/bin/`, `.gradle/` | 빌드 산출물 |
| `uploads/` | 런타임 업로드 이미지 |
| `.env`, 실제 시크릿 값 | DB 비밀번호·JWT 시크릿·AWS 자격증명. `.env.example` 만 키 이름으로 커밋 |
| `*.log` | 로그 |

**반드시 커밋해야 하는 예외**: `backend/gradle/wrapper/gradle-wrapper.jar` — `*.jar` 무시 규칙의 예외이며, 이게 없으면 다른 사람이 빌드할 수 없습니다.

빠뜨리기 쉬운 것: 새로 만든 디렉터리 전체가 untracked 로 남아 있는 경우(`git status` 가 디렉터리 하나만 `??` 로 보여줍니다). 커밋 전에 그 안의 파일 수를 확인하세요.

---

## 4. 실행 방법

Bash 툴에서는 heredoc 을 씁니다. PowerShell here-string(`@'...'@`)을 Bash 에 쓰면 `@` 문자가 메시지에 섞여 들어갑니다.

```bash
git commit -F - <<'EOF'
feat: 프로젝트 등록 및 Soft Delete API 구현

- 창작자 본인 검증을 서비스 계층에 배치해 인가 로직을 한 곳으로 모음
- 삭제 시 PLEDGED 후원을 CANCELLED 로 정리하고 예약 포인트를 해제

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
```

PowerShell 툴을 쓴다면 `@'` / `'@` 를 쓰되 닫는 `'@` 는 반드시 열 0 에 둡니다.

- 모든 커밋 본문 끝에 `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>` 를 붙입니다.
- **`push` 는 사용자가 명시적으로 요청할 때만** 합니다.
- 커밋 후 `git log --oneline -1` 로 제목이 의도대로 들어갔는지 확인합니다.

---

## 5. 커밋 후 자가 점검

- [ ] 제목이 `{feature}: {명사형 종결}` 형식인가
- [ ] 한 커밋에 한 작업 단위만 들어갔는가
- [ ] 빌드가 통과하는 상태인가
- [ ] 금지 대상이 섞이지 않았는가 (`git show --stat` 으로 확인)
- [ ] 새로 만든 파일이 빠짐없이 포함됐는가
