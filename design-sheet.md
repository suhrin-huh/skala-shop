# Design System

## Overview

**창작자 중심 크라우드펀딩 마켓플레이스**를 위한 디자인 시트이다. 베이스 캔버스는 **순백**(`{colors.canvas}` — #fcfcfc), 텍스트는 순검정이 아닌 짙은 잉크(`{colors.ink}` — #212124), 그리고 단 하나의 전압으로 **Tumblbug Coral**(`{colors.primary}` — #ff5757)이 달성률 수치 · N 뱃지 · 랭크 뱃지 · 좋아요 활성 상태 · 주요 CTA를 전부 담당한다. 코랄은 "돈이 모이고 있다"는 신호에만 쓰이며, 그 외 영역은 흑백으로 남긴다.

여기에 **Creator Violet**(`{colors.violet}` — #7a4df5)이 유일한 보조 액센트로 존재한다. "좋은 창작자" 같은 **신뢰 인증 뱃지**에만 붙고, CTA나 링크에는 절대 쓰지 않는다. 코랄이 "펀딩 성과", 바이올렛이 "창작자 검증"이라는 의미 분담이 이 시스템의 핵심 규칙이다.

타이포는 **Pretendard Variable**을 전 영역에 사용한다. 한글 UI 특성상 본문은 14–15px / 400, 카드 제목은 15px / 600으로 낮게 깔리고, 히어로 배너 헤드라인(40px / 800)과 섹션 헤드(22px / 700)만 크게 튄다. 영문 시스템 대비 자간을 −0.2px ~ −0.4px 조여야 한글 낱글자가 벌어져 보이지 않는다.

셰이프 언어는 **부드럽지만 카드형**이다. 프로젝트 썸네일과 히어로 배너는 12px(`{rounded.md}`), 카테고리 아이콘 타일은 22px(`{rounded.lg}`), 검색창은 10px(`{rounded.sm-lg}`), N 뱃지 · 좋아요 버튼 · 창작자센터 버튼은 완전 원형/알약(`{rounded.full}`). Airbnb처럼 전면 필(pill)이 아니라, **검색바는 사각 라운드 / 버튼은 알약**으로 나뉘는 것이 시각적 차이점이다.

**Key Characteristics:**

- 단일 성과 컬러: `{colors.primary}` (#ff5757). 달성률(`432% 달성`), 랭크 뱃지, N 뱃지, 활성 좋아요에만 사용. 페이지의 90%는 흰색 + 잉크.
- 이중 액센트 규칙: `{colors.violet}` (#7a4df5)은 창작자 신뢰 뱃지 전용. 코랄과 절대 혼용하지 않는다.
- 2단 헤더: 상단은 워드마크 + 검색 + 계정 유틸리티, 하단은 카테고리/탭 내비게이션. 활성 탭은 3px 잉크 언더라인(`{component.nav-tab-active}`).
- 랭킹 사이드바(`{component.rank-list}`)가 홈의 우측 고정 컬럼을 차지한다. 순위 숫자가 썸네일 좌상단에 코랄 뱃지로 겹쳐 붙는 것이 이 서비스의 최대 식별 요소.
- 카테고리 아이콘 스트립: 8개 대형 라운드 타일(72px, `{rounded.lg}`) + 하단 라벨. 일러스트 썸네일이 시각적 무게를 담당한다.
- 프로젝트 카드는 이미지 우선 + 3–4줄 메타(창작자 · 제목 2줄 클램프 · 뱃지 · 달성률). 좋아요 하트는 이미지 우하단에 떠 있다.
- 그림자 티어는 사실상 1단(`{elevation.floating}`). 깊이는 그림자가 아니라 흰 배경 위 이미지 블록의 라운드 클리핑으로 만든다.
- 4px 베이스 스페이싱. 섹션 간격은 `{spacing.section}`(56px)로 SaaS(80–96px)보다 조밀하다 — 스크롤당 카드 수가 중요한 마켓플레이스이기 때문.

## Colors

### Brand & Accent

- **Tumblbug Coral** (`{colors.primary}` — #ff5757): 유일한 브랜드 컬러. 달성률 텍스트, 랭크 뱃지, N 뱃지, 활성 좋아요 아이콘, 주 CTA 배경.
- **Coral Strong** (`{colors.primary-strong}` — #e84545): 프레스 / 포인터 다운 상태. `{component.button-primary-active}`.
- **Coral Soft** (`{colors.primary-soft}` — #ffeced): 옅은 코랄 틴트. "오늘 마감" 같은 긴급 뱃지 배경, 진행바 트랙.
- **Coral Disabled** (`{colors.primary-disabled}` — #ffc9c9): 비활성 CTA 배경.
- **Creator Violet** (`{colors.violet}` — #7a4df5): 창작자 인증 뱃지(`{component.badge-creator}`) 배경 전용. 링크/CTA 사용 금지.

### Surface

- **Canvas** (`{colors.canvas}` — #ffffff): 모든 공개 페이지의 기본 바닥. 다크 모드 없음.
- **Surface Soft** (`{colors.surface-soft}` — #f5f5f5): 검색 입력 배경, 카테고리 타일 배경, 호버 배경.
- **Surface Strong** (`{colors.surface-strong}` — #ebebeb): 이미지 로딩 플레이스홀더, 회색 메타 뱃지(`{component.badge-meta}`) 배경.

### Hairlines & Borders

- **Hairline** (`{colors.hairline}` — #e5e5e5): 기본 1px 경계. 헤더 하단, 내비 하단, 푸터 컬럼 구분, 카드 테두리.
- **Hairline Soft** (`{colors.hairline-soft}` — #f0f0f0): 리스트 아이템 사이의 더 옅은 구분선.
- **Border Strong** (`{colors.border-strong}` — #d4d4d4): 아웃라인 버튼(창작자센터) 테두리, 폼 인풋 아웃라인.

### Text

- **Ink** (`{colors.ink}` — #212124): 지배적 텍스트 색. 헤드라인, 카드 제목, 내비 링크. 순검정 아님.
- **Body** (`{colors.body}` — #3d3d3d): 프로젝트 상세 본문 등 장문 러닝 텍스트.
- **Muted** (`{colors.muted}` — #77777a): 창작자명, 기준 시각("26.08.07 23:36 기준"), "전체보기" 링크, 카테고리 라벨.
- **Muted Soft** (`{colors.muted-soft}` — #a5a5a8): 플레이스홀더, 비활성 링크, 푸터 법적 고지.
- **On Primary** (`{colors.on-primary}` — #ffffff): 코랄/바이올렛 위 텍스트.

### Semantic

- **Success** (`{colors.success}` — #2ba471): 펀딩 성공 상태 라벨. 사용 빈도 매우 낮음.
- **Warning** (`{colors.warning}` — #f5a623): "마감임박" 보조 표기.
- **Error** (`{colors.error}` — #d92d20): 폼 검증 실패 텍스트. 코랄과 구분되도록 더 어둡고 채도가 높다.
- **AD Label** (`{colors.ad-label}` — #a5a5a8): 광고 노출 아이템 우상단 "AD" 표기.

### Scrim

- **Scrim** (`{colors.scrim}` — #000000 at 55%): 모달 백드롭. 로그인 다이얼로그, 카테고리 시트.
- **Hero Scrim** (`{colors.scrim-hero}` — linear-gradient(90deg, rgba(0,0,0,.72), rgba(0,0,0,0) 62%)): 히어로 배너 좌측 텍스트 가독성용 그라디언트. 배너 이미지 위에 항상 깔린다.

## Typography

### Font Family

전 영역 **Pretendard Variable**. 폴백은 `Pretendard, -apple-system, "Apple SD Gothic Neo", "Noto Sans KR", "Malgun Gothic", system-ui, sans-serif`.

별도 디스플레이 패밀리는 없다. 워드마크(`tumblbug`)만 커스텀 이탤릭 레터링이며 폰트가 아닌 로고 에셋으로 취급한다.

### Hierarchy

| Token                        | Size | Weight | Line Height | Letter Spacing | Use                                                       |
| ---------------------------- | ---- | ------ | ----------- | -------------- | --------------------------------------------------------- |
| `{typography.hero-title}`    | 40px | 800    | 1.32        | -1.0px         | 히어로 배너 헤드라인 ("다섯 개의 문양")                   |
| `{typography.hero-sub}`      | 17px | 600    | 1.5         | -0.3px         | 히어로 배너 서브카피                                      |
| `{typography.section-title}` | 22px | 700    | 1.36        | -0.5px         | 섹션 헤드 ("주목할 만한 프로젝트", "인기 프로젝트")       |
| `{typography.nav-link}`      | 17px | 600    | 1.4         | -0.3px         | 내비게이션 탭 라벨 (홈, 인기, 신규)                       |
| `{typography.util-link}`     | 15px | 700    | 1.4         | -0.3px         | 헤더 우측 유틸리티 ("프로젝트 올리기", "로그인/회원가입") |
| `{typography.percent}`       | 17px | 800    | 1.3         | -0.4px         | 달성률 ("432% 달성") — 코랄                               |
| `{typography.card-title}`    | 15px | 600    | 1.45        | -0.3px         | 프로젝트 카드 제목 (2줄 클램프)                           |
| `{typography.card-title-lg}` | 16px | 600    | 1.45        | -0.3px         | 랭킹 리스트 제목 (사이드바)                               |
| `{typography.creator}`       | 13px | 400    | 1.4         | -0.2px         | 창작자명 — muted                                          |
| `{typography.body-md}`       | 15px | 400    | 1.6         | -0.2px         | 프로젝트 상세 본문                                        |
| `{typography.body-sm}`       | 14px | 400    | 1.5         | -0.2px         | 카테고리 라벨, 푸터 링크                                  |
| `{typography.badge}`         | 12px | 600    | 1.2         | -0.2px         | "좋은창작자", "8.6천만 원+", "오늘 마감"                  |
| `{typography.caption}`       | 13px | 400    | 1.4         | -0.2px         | 기준 시각, "전체보기"                                     |
| `{typography.legal}`         | 12px | 400    | 1.6         | 0              | 푸터 사업자 정보 / 저작권                                 |
| `{typography.micro-n}`       | 10px | 700    | 1           | 0              | 내비 "N" 뱃지                                             |
| `{typography.rank}`          | 13px | 700    | 1           | -0.2px         | 랭크 뱃지 숫자                                            |
| `{typography.button-md}`     | 15px | 600    | 1.2         | -0.3px         | 버튼 라벨                                                 |

### Principles

한글 UI 특성상 **본문은 작게, 성과 수치는 크게**. 카드 제목(15px/600)이 창작자명(13px/400)보다 겨우 2px 크지만 굵기 차이로 위계가 잡힌다. 자간은 전 스케일에서 음수(−0.2 ~ −1.0px)로 조인다 — 한글은 기본 자간이 라틴보다 넓게 보인다.

시스템에서 타입만으로 위계를 만드는 유일한 지점은 **달성률**(`{typography.percent}`)이다. 17px밖에 안 되지만 800 웨이트 + 코랄로 카드에서 가장 먼저 읽힌다. 크라우드펀딩의 핵심 신뢰 신호이므로 가장 강한 처리를 받는다.

### Note on Font Substitutes

Pretendard 미탑재 환경에서는 **Noto Sans KR**이 가장 가깝다. 다만 Noto는 Pretendard보다 x-height가 낮으므로 본문 줄 높이를 약 3% 줄여야 동일한 밀도가 나온다.

## Layout

### Spacing System

- **Base unit:** 4px.
- **Tokens:** `{spacing.xxs}` 2px · `{spacing.xs}` 4px · `{spacing.sm}` 8px · `{spacing.md}` 12px · `{spacing.base}` 16px · `{spacing.lg}` 24px · `{spacing.xl}` 32px · `{spacing.xxl}` 40px · `{spacing.section}` 56px.
- **섹션 수직 여백:** `{spacing.section}`(56px). 마켓플레이스 밀도를 위해 일반 SaaS보다 좁다.
- **카드 내부:** 이미지와 메타 블록 사이 `{spacing.md}`(12px), 메타 줄 간격 `{spacing.sm}`(8px).
- **거터:** 프로젝트 그리드 카드 사이 `{spacing.lg}`(24px), 랭킹 리스트 아이템 사이 `{spacing.base}`(16px), 카테고리 타일 사이 `{spacing.base}`(16px).

### Grid & Container

- **최대 콘텐츠 폭:** 1240px 중앙 정렬. 헤더 · 본문 · 푸터 모두 동일 컨테이너를 쓴다.
- **홈 2단 구조:** 좌측 메인 컬럼(`minmax(0, 1fr)`) + 우측 랭킹 사이드바 356px 고정, 거터 40px. 사이드바는 `position: sticky`가 아니라 자연 스크롤.
- **프로젝트 그리드:** 데스크톱 4열, 태블릿 3열, 모바일 2열. 행 리플로우 없이 **열 수만 줄인다.**
- **카테고리 스트립:** 8열 균등 그리드, 좁아지면 가로 스크롤(`overflow-x: auto`)로 전환.
- **히어로 배너:** 16:7 비율, `{rounded.md}` 클리핑, 좌측 정렬 텍스트 + 우하단 컨트롤(카운터 · 이전 · 다음).
- **푸터:** 4열 링크 리스트 + 하단 법적 고지 밴드.

### Whitespace Philosophy

히어로와 섹션 사이에는 56px을 주지만 카드 그리드는 24px로 조인다. "위는 열리고 아래는 빽빽한" 대비가 마켓플레이스 감각을 만든다. 사이드바 랭킹 리스트는 16px까지 더 조여 한 화면에 최소 5개 순위가 들어오게 한다.

## Elevation

- **Flat (그림자 없음):** 본문, 카드, 배너, 푸터 — 표면의 95%.
- **`{elevation.floating}`** — `0 2px 8px rgba(0, 0, 0, 0.08)`: 드롭다운(앱 다운로드, 카테고리 시트), 카드 호버 리프트, 이미지 위에 떠 있는 좋아요 버튼.
- **`{elevation.overlay-control}`** — `rgba(0,0,0,0.35)` 배경의 원형 컨트롤: 히어로 캐러셀 화살표. 그림자 대신 반투명 배경으로 이미지 위에서 분리된다.
- **Modal scrim:** `{colors.scrim}` 55%.

깊이 티어는 늘리지 않는다. 이미지 라운드 클리핑과 흰 배경 대비만으로 층을 구분하는 것이 원칙.

## Components

### Buttons

**`button-primary`** — 코랄 채움, 흰 텍스트, `{rounded.sm}` 8px, 12×24px 패딩, 48px 높이, 600 웨이트. "이 프로젝트 후원하기", "선물 선택하기".

**`button-primary-active`** — 배경이 `{colors.primary-strong}`로 전환. 트랜스폼 없음.

**`button-outline-pill`** — 흰 배경 + `{colors.border-strong}` 1px + 잉크 텍스트, `{rounded.full}`, 10×22px 패딩. 헤더의 "창작자센터"가 대표 사례.

**`button-ghost-text`** — 배경/테두리 없는 muted 텍스트 링크. "전체보기", "더보기".

**`icon-button-heart`** — 28×28px 원형. 이미지 위에서는 흰색 외곽선 하트(그림자 `{elevation.floating}`), 저장 시 `{colors.primary}` 채움 + 스케일 팝 애니메이션(140ms).

### Header & Navigation

**`utility-bar`** — 헤더 최상단 얇은 줄. 우측 정렬로 스토어 아이콘 2개(Google Play, App Store)와 "오직 앱에서만" 드롭다운. `{typography.caption}` muted.

**`header-main`** — 흰 배경, 88px 높이. 좌측 워드마크, 그 옆 검색창, 우측에 "프로젝트 올리기 | 로그인/회원가입".

**`search-field`** — `{colors.surface-soft}` 배경, 테두리 없음, `{rounded.sm-lg}` 10px, 48px 높이, 우측 끝에 잉크 돋보기 아이콘. 포커스 시에만 1px `{colors.ink}` 테두리가 생긴다 — 글로우 없음.

**`nav-bar`** — 헤더 하단, 1px 하단 하이라인. 좌측부터 햄버거 "카테고리" → 탭 목록, 우측 끝에 `{component.button-outline-pill}`.

**`nav-tab-active`** — 잉크 라벨 + 라벨 아래 3px 잉크 언더라인(글자 폭보다 짧게, 중앙 정렬 16px).

**`nav-tab-inactive`** — 잉크 라벨(muted 아님), 언더라인 없음. 호버 시 언더라인이 40% 투명도로 미리 나타난다.

**`badge-n`** — 탭 라벨 앞에 붙는 16px 코랄 원형 뱃지, 흰 "N" `{typography.micro-n}`. 신규 탭 표시 전용.

### Category Strip

**`category-tile`** — 72×72px, `{colors.surface-soft}` 배경, `{rounded.lg}` 22px, 안에 일러스트/사진 썸네일. 아래 `{typography.body-sm}` 라벨. 호버 시 2px 위로 리프트.

**`category-tile-new-dot`** — 타일 우상단에 겹치는 18px 코랄 원형 "N" 뱃지.

### Hero

**`hero-banner`** — 16:7 이미지, `{rounded.md}` 클리핑, `{colors.scrim-hero}` 오버레이. 좌측 32–56px 인셋에 `{typography.hero-title}` 헤드라인 + `{typography.hero-sub}` 서브카피.

**`hero-counter`** — 우하단 알약. `rgba(0,0,0,0.45)` 배경, "2 / 3" 표기 — 현재 인덱스만 700 웨이트 흰색, 전체 수는 70% 투명도.

**`hero-arrow`** — 40px 원형, `{elevation.overlay-control}`, 흰 셰브론. 좌/우 2개가 카운터 우측에 나란히 붙는다.

### Project Cards

**`project-card`** — 4:3 이미지(`{rounded.md}` 클리핑) + 우하단 `{component.icon-button-heart}`. 이미지 아래: 창작자명(`{typography.creator}` muted) → 제목 2줄 클램프(`{typography.card-title}`) → 뱃지 행 → 달성률(`{typography.percent}` 코랄).

**`project-card-progress`** — 카드 하단 3px 진행바. 트랙 `{colors.primary-soft}`, 채움 `{colors.primary}`, 100% 초과분은 클램프하되 수치 텍스트는 실제값 표기.

**`badge-creator`** — `{colors.violet}` 채움, 흰 텍스트, `{rounded.sm}` 6px, 4×8px 패딩, 앞에 ◆ 글리프. "좋은창작자" 전용.

**`badge-meta`** — `{colors.surface-strong}` 배경, `{colors.body}` 텍스트, `{rounded.sm}` 6px. 모금액("8.6천만 원+"), 남은 기간("4일 남음").

**`badge-urgent`** — `{colors.primary-soft}` 배경 + `{colors.primary}` 텍스트. "오늘 마감" 전용.

**`badge-ad`** — 카드 우상단 `{typography.badge}` `{colors.ad-label}` 텍스트 "AD". 배경 없음.

### Ranking Sidebar

**`rank-list`** — 홈 우측 컬럼. 헤드에 `{typography.section-title}` "인기 프로젝트" + 우측 "전체보기", 그 아래 `{typography.caption}` 기준 시각.

**`rank-item`** — 좌측 96×96px 썸네일(`{rounded.md}`) + 우측 메타 스택. 아이템 간 16px, 구분선 없음.

**`rank-badge`** — 썸네일 좌상단에 겹치는 24×24px 코랄 사각형(우하단만 8px 라운드), 흰 숫자 `{typography.rank}`. **이 시스템의 시그니처 요소** — 다른 컴포넌트에 재사용하지 않는다.

### Footer

**`footer-light`** — 캔버스와 동일한 흰 배경(대비 푸터 없음), 상단 1px 하이라인, 56×0px 패딩. 4열 링크 리스트(텀블벅 / 고객지원 / 창작자 / 팔로우).

**`legal-band`** — 푸터 하단 `{typography.legal}` muted-soft 블록. 사업자 정보, 통신판매업 신고, 저작권, 그리고 "텀블벅은 통신판매중개자이며 통신판매의 당사자가 아닙니다" 고지.

## Responsive Behavior

| Name    | Width       | Key Changes                                                                                                                                                                       |
| ------- | ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Mobile  | < 744px     | 유틸리티 바 숨김; 내비 탭 가로 스크롤; 검색창이 헤더 아래 전체 폭으로 내려감; 프로젝트 그리드 2열; 랭킹 사이드바가 메인 컬럼 아래로 내려가 1열 리스트가 됨; 히어로 헤드라인 26px. |
| Tablet  | 744–1128px  | 프로젝트 그리드 3열; 랭킹 사이드바가 메인 하단으로 이동해 2열 그리드로 재배치; 카테고리 스트립 가로 스크롤.                                                                       |
| Desktop | 1128–1440px | 2단 레이아웃(메인 + 356px 사이드바); 프로젝트 그리드 4열; 카테고리 8열 균등.                                                                                                      |
| Wide    | > 1440px    | 콘텐츠 폭 1240px에서 고정, 나머지는 거터가 흡수.                                                                                                                                  |

### Touch Targets

- 주 CTA 최소 48×48px.
- 내비 탭 세로 히트 영역 48px (라벨은 17px지만 패딩으로 확보).
- 좋아요 하트 28px 시각 크기 + 8px 인비저블 패딩으로 44px 확보.
- 히어로 화살표 40px — 데스크톱 전용 컨트롤이므로 모바일에서는 스와이프로 대체하고 숨긴다.

### Collapsing Strategy

- 카테고리 스트립과 내비 탭은 **줄바꿈하지 않고 가로 스크롤**로 전환한다(스크롤바 숨김).
- 그리드는 열 수만 감소, 행 리플로우 금지.
- 랭킹 사이드바는 축소되지 않고 위치만 이동한다 — 순위 정보는 잘라내지 않는다.
- 히어로는 비율이 16:7 → 4:3으로 깊어지고 스크림이 좌→우가 아닌 아래→위 방향으로 바뀐다.

## Motion

- **표준 이징:** `cubic-bezier(0.2, 0, 0, 1)`, 기본 지속 시간 200ms.
- **캐러셀 전환:** 480ms 크로스 슬라이드, 자동 진행 6초.
- **하트 토글:** 140ms 스케일 팝(1 → 1.25 → 1).
- **카드 호버:** `translateY(-2px)` + `{elevation.floating}`, 180ms.
- `prefers-reduced-motion: reduce` 환경에서는 자동 캐러셀 정지, 모든 트랜지션 0ms.

## Known Gaps

- **워드마크:** `tumblbug` 로고는 커스텀 이탤릭 레터링이며 폰트로 재현 불가. 구현에서는 이탤릭 근사치를 쓰되 실제 배포 시 SVG 에셋으로 교체할 것.
- **정확한 브랜드 코랄 값:** 스크린샷 추출 기반 근사치(#ff5757). 공식 브랜드 가이드의 값과 미세한 차이가 있을 수 있다.
- **프로젝트 상세 페이지:** 보상(리워드) 선택 사이드바, 후원자 목록, 업데이트 탭 구조는 캡처되지 않아 문서화하지 않았다.
- **다크 모드:** 공개 웹에 존재하지 않는다. 정의된 토큰 없음.
- **폼 에러 상태:** `{colors.error}` 토큰은 정의했으나 인풋 아웃라인 + 헬퍼 텍스트 조합은 캡처되지 않았다.
- **결제 플로우 / 로그인 모달:** 스크림 톤 외 내부 구조 미확인.
