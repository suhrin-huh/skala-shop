---
name: design-tokens
description: SKALA-FUND 디자인 시스템 준수 규칙 — 코랄/바이올렛 이중 액센트 분담, CSS 변수 토큰 목록, 타이포 위계, 스페이싱·라운드·엘리베이션, 컴포넌트 규격, 반응형 브레이크포인트. frontend/ 의 CSS 나 UI 컴포넌트를 작성·수정하기 전에 반드시 로드한다.
---

# 디자인 시스템 준수 규칙

정본은 [design-sheet.md](design-sheet.md), 구현 토큰은 [tokens.css](frontend/src/styles/tokens.css) 입니다.

---

## 0. 가장 중요한 두 가지

### ① 색 리터럴 금지

컴포넌트 CSS 에 `#ff5757`, `#212124` 같은 값을 직접 적지 않습니다. **반드시 `var(--color-*)`** 를 씁니다.
필요한 토큰이 없으면 → `tokens.css` 에 추가하고, 그 이유를 보고에 남깁니다. 컴포넌트에 몰래 리터럴을 넣지 않습니다.

```css
/* ❌ */ color: #ff5757;
/* ✅ */ color: var(--color-primary);
```

### ② 이중 액센트 분담 — 이 시스템의 핵심 규칙

| 색 | 토큰 | 오직 여기에만 |
|---|---|---|
| 코랄 #ff5757 | `--color-primary` | **펀딩 성과 신호**: 달성률 수치, 랭크 뱃지, N 뱃지, 활성 좋아요, 주 CTA |
| 바이올렛 #7a4df5 | `--color-violet` | **창작자 신뢰 인증 뱃지 전용** (`좋은창작자`) |

- 바이올렛을 CTA·링크·호버에 **절대** 쓰지 않습니다.
- 코랄을 "그냥 강조하고 싶어서" 쓰지 않습니다. 코랄은 **"돈이 모이고 있다"는 신호**입니다.
- 페이지의 90% 는 흰 배경 + 잉크 텍스트여야 합니다. 색이 많아지면 코랄의 전압이 죽습니다.

---

## 1. 컬러 토큰

| 용도 | 토큰 | 값 |
|---|---|---|
| 브랜드 | `--color-primary` | #ff5757 |
| 프레스 상태 | `--color-primary-strong` | #e84545 |
| 긴급 뱃지 배경 / 진행바 트랙 | `--color-primary-soft` | #ffeced |
| 비활성 CTA | `--color-primary-disabled` | #ffc9c9 |
| 창작자 인증 뱃지 | `--color-violet` | #7a4df5 |
| 기본 바닥 | `--color-canvas` | #ffffff |
| 검색 입력 / 카테고리 타일 / 호버 배경 | `--color-surface-soft` | #f5f5f5 |
| 이미지 플레이스홀더 / 회색 메타 뱃지 | `--color-surface-strong` | #ebebeb |
| 기본 1px 경계 | `--color-hairline` | #e5e5e5 |
| 리스트 아이템 구분선 | `--color-hairline-soft` | #f0f0f0 |
| 아웃라인 버튼 / 인풋 테두리 | `--color-border-strong` | #d4d4d4 |
| 지배적 텍스트 (순검정 아님) | `--color-ink` | #212124 |
| 장문 본문 | `--color-body` | #3d3d3d |
| 창작자명 / 기준 시각 / 전체보기 | `--color-muted` | #77777a |
| 플레이스홀더 / 푸터 법적 고지 | `--color-muted-soft` | #a5a5a8 |
| 코랄·바이올렛 위 텍스트 | `--color-on-primary` | #ffffff |
| 펀딩 성공 라벨 (빈도 낮음) | `--color-success` | #2ba471 |
| 마감임박 보조 | `--color-warning` | #f5a623 |
| 폼 검증 실패 (코랄과 구분되게 더 어둡다) | `--color-error` | #d92d20 |
| 광고 표기 | `--color-ad-label` | #a5a5a8 |
| 모달 백드롭 | `--color-scrim` | rgba(0,0,0,.55) |
| 히어로 그라디언트 | `--color-scrim-hero` | 좌→우 그라디언트 |

**다크 모드는 이 서비스에 존재하지 않습니다.** `prefers-color-scheme` 분기를 추가하지 않습니다.

---

## 2. 타이포 위계

`--font-*` 토큰은 `font` 단축 속성(weight/size/line-height/family)입니다. 자간은 `--tracking-*` 로 별도 지정합니다.

| 토큰 | 규격 | 용도 |
|---|---|---|
| `--font-hero-title` | 40px/800 | 히어로 헤드라인 (모바일 26px) |
| `--font-hero-sub` | 17px/600 | 히어로 서브카피 |
| `--font-section-title` | 22px/700 | 섹션 헤드 |
| `--font-nav-link` | 17px/600 | 내비 탭 라벨 |
| `--font-util-link` | 15px/700 | 헤더 우측 유틸리티 |
| `--font-percent` | 17px/800 | **달성률 — 코랄** |
| `--font-card-title` | 15px/600 | 카드 제목 (2줄 클램프) |
| `--font-card-title-lg` | 16px/600 | 랭킹 리스트 제목 |
| `--font-creator` | 13px/400 | 창작자명 (muted) |
| `--font-body-md` | 15px/400 | 상세 본문 |
| `--font-body-sm` | 14px/400 | 카테고리 라벨, 푸터 링크 |
| `--font-badge` | 12px/600 | 뱃지 |
| `--font-caption` | 13px/400 | 기준 시각, 전체보기 |
| `--font-legal` | 12px/400 | 푸터 법적 고지 |
| `--font-micro-n` | 10px/700 | N 뱃지 |
| `--font-rank` | 13px/700 | 랭크 뱃지 숫자 |
| `--font-button-md` | 15px/600 | 버튼 라벨 |

**원칙**: 한글 UI 라 본문은 작게, 성과 수치는 크게. 카드 제목(15/600)과 창작자명(13/400)은 크기 차이가 2px 뿐이고 **굵기로 위계를 만듭니다.** 자간은 전 스케일 음수(−0.2 ~ −1.0px)입니다.

타입만으로 위계를 만드는 유일한 지점이 **달성률**입니다. 17px 밖에 안 되지만 800 웨이트 + 코랄로 카드에서 가장 먼저 읽혀야 합니다.

---

## 3. 스페이싱 · 셰이프 · 엘리베이션

베이스 4px. `--spacing-xxs`(2) / `xs`(4) / `sm`(8) / `md`(12) / `base`(16) / `lg`(24) / `xl`(32) / `xxl`(40) / `section`(56).

- 섹션 수직 여백 `--spacing-section` (56px). 일반 SaaS(80–96px)보다 **의도적으로 좁습니다** — 스크롤당 카드 수가 중요한 마켓플레이스이기 때문.
- 카드 내부: 이미지↔메타 12px, 메타 줄 간격 8px.
- 거터: 프로젝트 그리드 24px, 랭킹 리스트 16px, 카테고리 타일 16px.

라운드 — **검색바는 사각 라운드, 버튼은 알약**으로 나뉘는 것이 이 시스템의 시각적 특징입니다.

| 토큰 | 값 | 적용 |
|---|---|---|
| `--rounded-sm` | 6px | 뱃지, 주 CTA(8px 계열) |
| `--rounded-sm-lg` | 10px | 검색창 |
| `--rounded-md` | 12px | 썸네일, 히어로 배너 |
| `--rounded-lg` | 22px | 카테고리 아이콘 타일 |
| `--rounded-full` | 9999px | N 뱃지, 좋아요, 창작자센터 버튼 |

엘리베이션은 **사실상 1단**입니다. 새 depth 티어를 만들지 마세요.

- `--elevation-floating` (`0 2px 8px rgba(0,0,0,.08)`) — 드롭다운, 카드 호버 리프트, 이미지 위 좋아요 버튼
- `--elevation-overlay-control` (`rgba(0,0,0,.35)` 배경) — 히어로 캐러셀 화살표. 그림자 대신 반투명 배경으로 분리한다.

깊이는 그림자가 아니라 **흰 배경 위 이미지 블록의 라운드 클리핑**으로 만듭니다.

---

## 4. 컴포넌트 규격

| 컴포넌트 | 규격 |
|---|---|
| `btn-primary` | 코랄 채움, 흰 텍스트, 48px 높이, `--rounded-sm`. 프레스 시 `--color-primary-strong`, **트랜스폼 없음** |
| `btn-outline-pill` | 흰 배경 + `--color-border-strong` 1px + 잉크 텍스트, `--rounded-full` |
| `icon-button-heart` | 28×28 원형. 이미지 위에서는 흰 외곽선 + `--elevation-floating`. 저장 시 코랄 채움 + 스케일 팝 140ms |
| `header-main` | 흰 배경 88px. 워드마크 + 검색창 + "프로젝트 올리기 / 로그인" |
| `search-field` | `--color-surface-soft` 배경, 테두리 없음, `--rounded-sm-lg`, 48px. **포커스 시에만 1px 잉크 테두리 — 글로우 없음** |
| `nav-tab-active` | 잉크 라벨 + 아래 **3px 잉크 언더라인** (글자 폭보다 짧게, 중앙 16px) |
| `nav-tab-inactive` | 잉크 라벨(muted 아님). 호버 시 언더라인이 40% 투명도로 미리 나타남 |
| `badge-n` | 16px 코랄 원형 + 흰 "N" |
| `category-tile` | 72×72, `--color-surface-soft`, `--rounded-lg`. 호버 시 2px 리프트 |
| `hero-banner` | **16:7** 비율, `--rounded-md` 클리핑, `--color-scrim-hero` 오버레이, 좌측 32–56px 인셋 |
| `hero-counter` | 우하단 알약, `rgba(0,0,0,.45)`. 현재 인덱스만 700 흰색, 전체 수는 70% 투명도 |
| `project-card` | **4:3** 이미지 + 우하단 하트. 아래로 창작자명 → 제목 2줄 클램프 → 뱃지 행 → 달성률 |
| `project-card-progress` | 3px 진행바. 트랙 `--color-primary-soft`, 채움 `--color-primary`. **100% 초과분은 바에서 클램프하되 수치 텍스트는 실제값 표기** |
| `rank-badge` | 썸네일 좌상단 24×24 코랄 사각형(**우하단만 8px 라운드**), 흰 숫자. **이 시스템의 시그니처 — 다른 컴포넌트에 재사용 금지** |
| `rank-item` | 좌측 96×96 썸네일 + 우측 메타. 아이템 간 16px, **구분선 없음** |
| `footer-light` | 캔버스와 같은 흰 배경(대비 푸터 아님), 상단 1px 하이라인, 4열 링크 |

---

## 5. 레이아웃 · 반응형

- 최대 콘텐츠 폭 1240px 중앙 정렬 (`--max-content-width`). 헤더·본문·푸터가 같은 컨테이너를 씁니다.
- 홈 2단: 좌측 `minmax(0, 1fr)` + 우측 랭킹 사이드바 356px(`--sidebar-width`) 고정, 거터 40px. 사이드바는 `sticky` 가 아니라 자연 스크롤.

| 이름 | 폭 | 변화 |
|---|---|---|
| Mobile | < 744px | 유틸리티 바 숨김, 내비 탭 가로 스크롤, 검색창이 헤더 아래 전체 폭, 그리드 2열, 랭킹 사이드바가 아래로 내려가 1열, 히어로 26px |
| Tablet | 744–1128px | 그리드 3열, 랭킹이 메인 하단 2열 그리드, 카테고리 가로 스크롤 |
| Desktop | 1128–1440px | 2단 레이아웃, 그리드 4열, 카테고리 8열 |
| Wide | > 1440px | 콘텐츠 1240px 고정, 나머지는 거터가 흡수 |

**축소 전략 — 반드시 지킬 것**

- 카테고리 스트립과 내비 탭은 **줄바꿈하지 않고 가로 스크롤**로 전환 (스크롤바 숨김).
- 그리드는 **열 수만 감소**. 행 리플로우 금지.
- 랭킹 사이드바는 축소하지 않고 **위치만 이동**. 순위 정보를 잘라내지 않습니다.
- 히어로는 16:7 → 4:3 으로 깊어지고 스크림 방향이 좌→우 에서 아래→위 로 바뀝니다.

**터치 타깃**: 주 CTA 최소 48×48, 내비 탭 세로 히트 48px, 좋아요 28px 시각 + 패딩으로 44px 확보. 히어로 화살표는 데스크톱 전용이므로 모바일에서 숨기고 스와이프로 대체합니다.

---

## 6. 모션

- 표준 이징 `--ease-standard` (`cubic-bezier(0.2, 0, 0, 1)`), 기본 `--duration-base` 200ms
- 캐러셀 전환 480ms 크로스 슬라이드, 자동 진행 6초
- 하트 토글 140ms 스케일 팝 (1 → 1.25 → 1)
- 카드 호버 `translateY(-2px)` + `--elevation-floating`, 180ms
- `prefers-reduced-motion: reduce` 에서 자동 캐러셀 정지 + 모든 트랜지션 0ms → **토큰에 이미 반영되어 있으니 duration 토큰만 쓰면 자동으로 대응됩니다.** 하드코딩한 ms 값을 쓰면 이 대응이 깨집니다.

---

## 7. 셀프 체크리스트

UI 작업을 끝내기 전에 확인합니다.

- [ ] 컴포넌트 CSS 에 색상 리터럴(`#`, `rgb(`)이 없다
- [ ] 바이올렛이 창작자 뱃지 외의 곳에 쓰이지 않았다
- [ ] 코랄이 성과 신호(달성률·랭크·N·좋아요·주CTA) 외에 쓰이지 않았다
- [ ] 하드코딩된 px 간격 대신 `--spacing-*` 를 썼다
- [ ] 트랜지션 duration 이 토큰 기반이다 (reduced-motion 대응)
- [ ] 새 그림자 티어를 만들지 않았다
- [ ] 다크 모드 분기를 넣지 않았다
- [ ] 좁은 화면에서 그리드가 열 수만 줄고 행이 리플로우되지 않는다
