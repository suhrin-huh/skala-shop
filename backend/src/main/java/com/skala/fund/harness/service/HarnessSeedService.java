package com.skala.fund.harness.service;

import com.skala.fund.common.util.CategoryCatalog;
import com.skala.fund.domain.Category;
import com.skala.fund.domain.Customer;
import com.skala.fund.domain.Project;
import com.skala.fund.domain.ProjectStatus;
import com.skala.fund.repository.CategoryRepository;
import com.skala.fund.repository.CustomerRepository;
import com.skala.fund.repository.PledgeRepository;
import com.skala.fund.repository.ProjectLikeRepository;
import com.skala.fund.repository.ProjectRepository;
import com.skala.fund.repository.RefreshTokenRepository;
import com.skala.fund.service.PledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 하네스 시뮬레이션용 샘플 데이터 세딩.
 *
 * 정산 배치를 눈으로 검증하려면 "이미 마감됐지만 아직 정산되지 않은" 프로젝트가 필요하다.
 * 후원은 마감 전에만 가능하므로, 후원을 먼저 만들고 마감일을 과거로 되돌리는 순서로 만든다.
 */
@Slf4j
@Profile("dev")
@Service
@RequiredArgsConstructor
public class HarnessSeedService {

    private static final String SEED_PASSWORD = "skala123!";

    private final CategoryRepository categoryRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final PledgeRepository pledgeRepository;
    private final ProjectLikeRepository projectLikeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PledgeService pledgeService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String seed() {
        clearAll();

        List<Category> categories = seedCategories();
        Customer creator = saveCustomer("creator@skala.com", "기쁜 프로도", 100_000L);
        Customer supporter = saveCustomer("test@skala.com", "테스트 라이언", 1_000_000L);

        List<Project> ongoing = seedOngoingProjects(creator, categories);
        // 정산 대기 데모도 세라믹 스탠드 라이트(홈·리빙 소재)라 위 4번 프로젝트와 같은 카테고리를 쓴다.
        Project settlementTarget = seedSettlementTarget(creator, categories.get(9));

        // 정산 배치가 즉시 처리할 수 있도록 마감된 프로젝트에 목표 초과 후원을 붙여둔다.
        pledgeService.createPledge(supporter.getId(), settlementTarget.getId(), 300_000L);
        closeProject(settlementTarget);

        String message = String.format(
                "시딩 완료 - 카테고리 %d개, 계정 2개(creator@skala.com / test@skala.com, 비밀번호 %s), "
                        + "진행중 프로젝트 %d개, 정산 대기 프로젝트 1개",
                categories.size(), SEED_PASSWORD, ongoing.size());
        log.info(message);
        return message;
    }

    private void clearAll() {
        // FK 역순으로 지운다.
        refreshTokenRepository.deleteAllInBatch();
        projectLikeRepository.deleteAllInBatch();
        pledgeRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    private List<Category> seedCategories() {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < CategoryCatalog.NAMES.size(); i++) {
            categories.add(Category.builder()
                    .name(CategoryCatalog.NAMES.get(i))
                    .displayOrder(i + 1)
                    .build());
        }
        return categoryRepository.saveAll(categories);
    }

    private Customer saveCustomer(String email, String nickname, long point) {
        return customerRepository.save(Customer.builder()
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .point(point)
                .build());
    }

private List<Project> seedOngoingProjects(Customer creator, List<Category> categories) {
    List<Project> projects = List.of(
            // 1. 테크/가전 (기계식 키보드 사진)
            buildProject(creator, categories.get(13), 
                    "[응답속도 0.1ms] 클래식 아날로그 타자기 감성, 스마트 무선 기계식 키보드 Retron-75",
                    "매일 반복되는 지루한 문서 작업과 타핑 시간을 완전히 새로운 감성으로 바꾸어 줄 프리미엄 키보드가 찾아왔습니다.\n" +
                    "1960년대 원형 타자기의 클래식한 실루엣에 현대적인 최첨단 무선 기술을 접목하여 시각적 만족감과 극상의 타건감을 동시에 제공합니다.\n" +
                    "단단한 샌드블라스트 알루미늄 하우징을 채택하여 묵직하고 흔들림 없는 타건 환경을 완성했습니다.\n" +
                    "내부에는 5중 흡음재와 가스켓 마운트 구조를 설계하여 통울림을 완벽히 잡았으며, 자갈을 굴리는 듯한 독보적인 타건음을 자랑합니다.\n" +
                    "핫스왑 커스텀 PCB를 적용해 사용자가 원하는 스위치로 언제든 손쉽게 교체할 수 있어 나만의 키보드를 만드는 재미를 선사합니다.\n" +
                    "최신 블루투스 5.2 및 2.4GHz 무선 연동을 통해 최대 3대의 기기를 동시에 페어링하고 1ms 미만의 빠른 응답 속도를 경험할 수 있습니다.\n" +
                    "손목의 부담을 최소화하는 인체공학적 스텝스컬쳐2 스위치 배열과 레트로 감성의 이중사출 PBT 키캡을 적용했습니다.\n" +
                    "4000mAh 대용량 배터리를 탑재하여 한 번의 완충만으로 최대 200시간 동안 선 없이 자유롭게 작업에 몰입할 수 있습니다.\n" +
                    "이번 크라우드펀딩에서는 전용 고급 가죽 손목 받침대와 클래식 코일 케이블이 포함된 풀패키지 리워드를 제공합니다.\n" +
                    "글을 쓰고 코딩을 하는 모든 순간이 즐거워지는 특별한 키보드 라이프를 지금 시작해보세요.\n" +
                    "여러분의 서재와 오피스 공간을 단숨에 영화 속 한 장면처럼 만들어 드리겠습니다.",

                    "https://images.unsplash.com/photo-1547394765-185e1e68f34e?auto=format&fit=crop&w=800&q=80",
                    5_000_000L, 5, 20),

            // 2. 문구/디자인 (다이어리/플래너 사진)
            buildProject(creator, categories.get(0), 
                    "내 삶의 흐름을 리셋하는 시스템 2027 모듈러 가죽 플래너 & 만년 다이어리",
                    "새해마다 거창한 계획을 세우지만 며칠 못 가 빈 페이지로 남겨지는 다이어리에 죄책감을 느끼신 적이 있으신가요?\n" +
                    "이 제품은 정해진 틀에 나를 맞추는 것이 아니라, 내 삶의 흐름에 맞춰 내지를 자유롭게 조합하는 시스템 모듈러 플래너입니다.\n" +
                    "목표 관리, 비전 보드, 데일리 하비 트래커, 아이디어 스케치북 등 12가지의 다양한 모듈 내지를 자유롭게 꼈다 뺐다 할 수 있습니다.\n" +
                    "커버는 이탈리아 토스카나 지방에서 전통 방식으로 가공된 친환경 베지터블 가죽만을 사용하여 제작되었습니다.\n" +
                    "시간이 지날수록 사용자의 손때와 습관에 따라 자연스럽게 에이징되어 세월이 흘러도 고유한 멋을 더해갑니다.\n" +
                    "내지는 100g 고급 만년필 전용지를 사용하여 잉크 번짐이나 뒷장 비침이 전혀 없고 부드러운 서각감을 제공합니다.\n" +
                    "180도 완전 펼침이 가능한 특수 바인딩 제본 방식을 적용하여 어떤 페이지를 펼쳐도 편안하게 기록할 수 있습니다.\n" +
                    "북마크 가죽 끈과 만년필 홀더, 그리고 중요한 영수증과 명함을 보관할 수 있는 포켓까지 실용적인 디테일을 놓치지 않았습니다.\n" +
                    "복잡한 일상을 명확하게 정돈하고 잊고 지내던 내 안의 창의성과 집중력을 다시 깨워보세요.\n" +
                    "나만의 일상을 더 체계적이고 아름답게 다듬어 나가는 기록의 여정에 함께하시길 바랍니다.\n" +
                    "오직 펀딩 후원자분들만을 위해 이름을 무료로 각인해 드리는 폰트 각인 이벤트를 진행합니다.",
                    "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80",
                    1_000_000L, 2, 15),

            // 3. 게임/보드게임 (TRPG/다이스 사진)
            buildProject(creator, categories.get(4), 
                    "고대 신화의 서막: 오리지널 TRPG 룰북 [신들의 몰락] & 핸드메이드 원석 주사위",
                    "평범한 일상에서 벗어나 잊혀진 고대 신들과 영웅들이 숨 쉬는 웅장한 환상의 세계로 당신을 초대합니다.\n" +
                    "본 프로젝트는 3년간의 세계관 구성과 입체적인 플레이 테스트를 거쳐 완성된 오리지널 스토리텔링 TRPG 시스템입니다.\n" +
                    "북유럽과 켈트 신화의 비극적 서사에서 영감을 얻어 몰입감 넘치는 50여 개의 시나리오와 몬스터 라이브러리를 구축했습니다.\n" +
                    "초보 마스터도 손쉽게 플레이를 진행할 수 있도록 입문자용 퀵스타트 가이드와 전용 마스터 스크린을 함께 구성했습니다.\n" +
                    "함께 제공되는 다이스 세트는 인조 수지가 아닌 실제 천연 라피스 라줄리 원석을 손으로 직접 깎아 만든 하이엔드 주사위입니다.\n" +
                    "주사위의 각 면에는 신비로운 고대 룬 문자 각인이 24K 금박으로 정교하게 새겨져 있어 압도적인 영롱함을 선사합니다.\n" +
                    "묵직한 손맛과 함께 주사위를 던지는 매 순간이 마치 진짜 마법 주문을 외우는 듯한 강렬한 몰입감을 전해줍니다.\n" +
                    "풍부한 풀컬러 일러스트가 수록된 하드커버 양장 룰북은 서가에 꽂아두는 것만으로도 훌륭한 소장 가치를 자랑합니다.\n" +
                    "친구들과 함께 모여 테이블 위에서 펼쳐지는 무한한 상상력과 긴장감 넘치는 모험을 경험해 보세요.\n" +
                    "당신의 선택 하나가 이 세계의 운명을 결정짓고 위대한 전설의 한 페이지를 기록하게 될 것입니다.\n" +
                    "펀딩 한정 플레이 매트와 캐릭터 시트 패드도 함께 준비되어 있으니 이번 기회를 절대로 놓치지 마세요.",
                    "https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&w=800&q=80",
                    3_000_000L, 10, 5),

            // 4. 홈·리빙 (조명 사진) — CategoryCatalog 에 "인테리어/조명" 전용 카테고리가 없어 가장 가까운 홈·리빙으로 분류
            buildProject(creator, categories.get(9),
                    "공간에 따스한 온기를 전하는 수제 빈티지 감성 워머 & 세라믹 오르골 조명",
                    "바쁜 하루를 마치고 집으로 돌아와 불을 껐을 때, 아늑하고 따뜻한 빛이 나를 맞아준다면 얼마나 큰 위로가 될까요?\n" +
                    "차가운 인공조명에서 벗어나 공간 전체를 따스한 분위기로 감싸주는 핸드메이드 세라믹 오르골 조명을 소개합니다.\n" +
                    "도자기 장인이 한 땀 한 땀 정성스럽게 흙을 빚어 구워낸 세라믹 갓은 세상에 단 하나뿐인 자연스러운 질감을 선사합니다.\n" +
                    "은은한 3000K 웜옐로우 컬러의 LED 코브 조명을 탑재하여 눈 부심 없이 아늑한 야간 등 역할을 완벽하게 수행합니다.\n" +
                    "하단 베이스를 살짝 돌리면 아날로그 오르골 특유의 맑고 청아한 멜로디가 퍼져 나와 지친 마음을 다정하게 달래줍니다.\n" +
                    "조명 상단에는 캔들 워머 기능이 내장되어 있어 좋아하는 아로마 오일이나 촛농을 올려두면 향기가 온 공간으로 은은하게 퍼집니다.\n" +
                    "무소음 디밍 조절 스위치를 적용해 아침 노을빛부터 한밤중 미등까지 원하는 밝기로 미세하게 조절할 수 있습니다.\n" +
                    "대용량 리튬 이온 배터리가 내장된 무선 타입으로 침대 곁, 거실, 야외 캠핑장 등 원하는 어디든 자유롭게 이동할 수 있습니다.\n" +
                    "소중한 사람에게 마음을 전하는 감성적인 선물이나, 나만의 비밀스러운 휴식 공간을 완성하는 오브제로 안성맞춤입니다.\n" +
                    "삭막했던 방 안 한구석을 감성적인 온기로 가득 채우고 지친 일상 속에서 나만을 위한 진짜 휴식을 선물하세요.\n" +
                    "후원해 주신 모든 분께 시그니처 숲 향 아로마 디퓨저 오일 10ml를 사은품으로 함께 보내드립니다.",
                    "https://images.unsplash.com/photo-1570823635306-250abb06d4b3?auto=format&fit=crop&w=800&q=80",
                    2_000_000L, 7, 10),

            // 5. 출판 (책과 커피 사진)
            buildProject(creator, categories.get(2),
                    "조용한 몰입의 시간: 현대인을 위한 문학 에세이집 [어둠 속에서 빛나는 조각들]",
                    "끝없는 알림과 끊임없이 쏟아지는 디지털 피드 속에서 잠시 스마트폰을 내려놓고 완전한 정적을 느껴본 적이 언제이신가요?\n" +
                    "이 책은 속도에 쫓겨 자기 자신을 잃어버린 현대인들에게 깊은 숨을 쉬어갈 수 있는 진정한 휴식의 시간을 선사합니다.\n" +
                    "10년간 독자들의 깊은 사랑을 받아온 문학 작가가 도심 속 개별의 삶에서 건져 올린 30편의 감성 에세이를 담았습니다.\n" +
                    "페이지마다 실린 수묵 화풍의 독창적인 일러스트는 글이 주는 여운을 더욱 깊게 만들어주며 감성을 자극합니다.\n" +
                    "한 장 한 장 넘길 때마다 마음속 깊이 묻어두었던 감정들을 다정하게 다독여주는 따뜻한 문장들로 채워져 있습니다.\n" +
                    "책의 만듦새 역시 소장 가치를 높이기 위해 최고급 사간 스웨이드 양장 제본과 은박 후가공으로 제작되었습니다.\n" +
                    "눈이 피로하지 않은 눈부심 방지 고급 서양지 종이를 사용하여 오랫동안 책을 읽어도 눈의 부담이 거의 없습니다.\n" +
                    "독서의 몰입을 돕기 위해 작가가 직접 큐레이션한 클래식 플레이리스트 QR코드가 본문 곳곳에 함께 수록되어 있습니다.\n" +
                    "따뜻한 커피 한 잔과 함께 이 책을 펼치는 순간, 오롯이 나와 마주하는 가장 평온하고 밀도 높은 시간이 시작됩니다.\n" +
                    "지친 친구나 소중한 이에게 따스한 위로의 마음을 건넬 수 있는 가장 다정하고 고급스러운 선물이 될 것입니다.\n" +
                    "펀딩 한정 리워드로 작가의 친필 서명이 담긴 오프닝 에디션과 아크릴 북마크 굿즈를 제공합니다.",
                    "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80",
                    1_500_000L, 3, 12)
    );
        List<Project> saved = projectRepository.saveAll(projects);

        // 인기 정렬과 달성률 표시를 확인하기 위한 모금액. 실제 Pledge 없이 비정규화 값만
        // 올리면 하네스 불변식(I3/I4)이 깨지므로, 데모용 수치는 여기서 조작하지 않는다.
        return saved;
    }

private Project seedSettlementTarget(Customer creator, Category category) {
    return projectRepository.save(buildProject(creator, category, 
"[마감/정산대기] 공간의 분위기를 바꾸는 오리지널 핸드메이드 도자 스탠드 라이트",
        "지친 하루의 끝, 불을 끄고 나만의 공간에 들어섰을 때 온기를 더해줄 특별한 오브제 조명을 소개합니다.\n" +
        "이 스탠드 라이트는 도자기 장인이 오랜 시간 흙을 빚고 가마에서 구워내어 세상에 단 하나뿐인 질감을 자랑합니다.\n" +
        "자연스러운 도자기 갓을 통해 퍼져 나오는 은은한 3000K 웜옐로우 조명은 눈의 피로를 덜어주고 아늑함을 선사합니다.\n" +
        "조명 본체는 클래식한 아날로그 감성의 무소음 다이얼 스위치를 적용하여 원하시는 밝기로 미세하게 조절이 가능합니다.\n" +
        "침대 옆 협탁이나 거실 테이블, 혹은 서재 책상 등 어느 곳에 놓아도 오브제로서 훌륭한 인테리어 포인트가 됩니다.\n" +
        "열에 강한 고품질 세라믹 재질로 제작되어 오랜 시간 켜두어도 변색이나 변형 없이 안전하게 사용하실 수 있습니다.\n" +
        "하단에는 슬립 방지 패드가 장착되어 있어 테이블 위에서 미끄러지지 않고 안정적으로 자리를 지켜줍니다.\n" +
        "이번 프로젝트는 펀딩 목표 달성률 500%를 돌파하며 많은 서포터분들의 뜨거운 사랑 속에 성공적으로 마감되었습니다.\n" +
        "오랫동안 기다려주신 서포터분들을 위해 정성스러운 꼼꼼한 검수와 2중 안전 폼 패키징 작업을 거쳐 즉시 발송을 준비 중입니다.\n" +
        "공간을 가득 채우는 따뜻한 빛과 함께 당신의 일상이 매일 밤 조금 더 다정하고 평온해지기를 진심으로 바랍니다.\n" +
        "후원해 주신 모든 분께 감사의 마음을 담아 시그니처 도자기 코스터를 한정 사은품으로 함께 동봉해 드립니다.",
        "https://images.unsplash.com/photo-1606170033648-5d55a3edf314?auto=format&fit=crop&w=800&q=80",
        200_000L, 10, 0));
}

    private Project buildProject(Customer creator, Category category, String title, String description,
                                 String mainImage, long targetAmount, int startedDaysAgo, int endsInDays) {
        return Project.builder()
                .creator(creator)
                .category(category)
                .title(title)
                .description(description)
                .mainImage(mainImage)
                .targetAmount(targetAmount)
                .startDate(LocalDate.now().minusDays(startedDaysAgo))
                .endDate(LocalDate.now().plusDays(endsInDays))
                .status(ProjectStatus.ONGOING)
                .build();
    }

    /** 후원을 받은 뒤 마감일을 어제로 되돌려 정산 대상으로 만든다. */
    private void closeProject(Project project) {
        project.update(project.getCategory(), project.getTitle(), project.getDescription(),
                project.getMainImage(), project.getTargetAmount(),
                project.getStartDate(), LocalDate.now().minusDays(1));
    }
}
