package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

// TODO: @SpringBootTest vs @ExtendWith(MockitoExtension.class) 차이점 학습함
// @SpringBootTest: 스프링 컨텍스트 전체를 로드 (무겁고 느림)
// @ExtendWith(MockitoExtension.class): 단위 테스트용, Mock 객체만 사용 (가볍고 빠름)
// 결론: 단위 테스트에서는 MockitoExtension을 사용하는 게 좋음!
@ExtendWith(MockitoExtension.class)
class PointControllerTest {

    private static final Logger log = LoggerFactory.getLogger(PointControllerTest.class);

    @Mock
    private PointService pointService;

    private PointController pointController;

    @BeforeEach
    void setUp() {
        // TODO: 처음에는 @InjectMocks를 쓰려고 했는데, 직접 생성자 호출이 더 명확함
        pointController = new PointController(pointService);
    }

    // ==================== 기본 기능 테스트 ====================

    @Test
    @DisplayName("특정유저 포인트 조회_성공")
    public void 특정유저의_포인트를_조회한다() {
        //given
        Long userId = 1L;
        UserPoint mockUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(pointService.getPointById(userId)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.point(userId);

        //then
        // TODO: assertEquals vs assertThat 중에 고민했는데, AssertJ가 더 읽기 쉬워서 선택
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
        
        // TODO: 로그도 확인해야 할까? 일단 보류
        log.info("포인트 조회 테스트 완료: {}", result);
    }

    @Test
    @DisplayName("특정유저 포인트 충전 이용내역 조회_성공")
    public void 특정유저의_포인트_충전이용내역_조회_성공() {
        //given
        Long userId = 1L;
        List<PointHistory> mockHistories = List.of(
            new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
            new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointService.getPointHistoryById(userId)).thenReturn(mockHistories);
        
        //when
        List<PointHistory> result = pointController.history(userId);
        
        //then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).isNotEmpty();
        
        // TODO: 더 상세한 검증을 해야 할까? 각 히스토리의 내용도 확인?
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
    }

    @Test
    @DisplayName("특정유저 포인트 충전_성공")
    public void 특정유저의_포인트를_충전한다() {
        //given
        Long userId = 1L;
        Long amount = 1000L;
        UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(pointService.chargePoint(userId, amount)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.charge(userId, amount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(amount);
        
        // TODO: 충전 후 실제로 포인트가 증가했는지 확인하는 테스트도 필요할 듯
    }

    @Test
    @DisplayName("특정유저 포인트 사용_성공")
    public void 특정유저의_포인트를_사용한다() {
        //given
        Long userId = 1L;
        Long amount = 500L;
        UserPoint mockUserPoint = new UserPoint(userId, 500L, System.currentTimeMillis());
        when(pointService.usePoint(userId, amount)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.use(userId, amount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(500L);
    }

    // ==================== 정책별 테스트 ====================

    @Test
    @DisplayName("PLC_PNT_008: 충전 한도 초과 방지")
    public void 충전_한도_초과시_예외발생() {
        //given
        Long userId = 1L;
        Long amount = 1_500_000L; // 150만원 (한도 초과)
        when(pointService.chargePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("한번에 충전할 수 있는 최대 금액은 100만원입니다."));

        //when & then
        // TODO: assertThrows vs assertThatThrownBy 중에 고민했는데, 
        // assertThatThrownBy가 더 fluent하고 메시지 검증이 쉬워서 선택
        assertThatThrownBy(() -> pointController.charge(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("한번에 충전할 수 있는 최대 금액은 100만원입니다.");
    }

    @Test
    @DisplayName("PLC_PNT_004: 1회 사용 한도 제한")
    public void 사용_한도_초과시_예외발생() {
        //given
        Long userId = 1L;
        Long amount = 1_500_000L; // 150만원 (한도 초과)
        when(pointService.usePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("한번에 사용할 수 있는 최대 금액은 100만원입니다."));

        //when & then
        assertThatThrownBy(() -> pointController.use(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("한번에 사용할 수 있는 최대 금액은 100만원입니다.");
    }

    @Test
    @DisplayName("PLC_PNT_003: 고액 사용 시 본인 인증 요구")
    public void 고액_사용시_본인인증_요구() {
        //given
        Long userId = 1L;
        Long amount = 60_000L; // 6만원 (5만원 이상)
        when(pointService.usePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("5만원 이상 사용시 본인 인증이 필요합니다."));

        //when & then
        assertThatThrownBy(() -> pointController.use(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("5만원 이상 사용시 본인 인증이 필요합니다.");
    }

    @Test
    @DisplayName("잔액 부족시 사용 실패")
    public void 잔액_부족시_사용_실패() {
        //given
        Long userId = 1L;
        Long amount = 5000L;
        when(pointService.usePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("포인트 잔액이 부족합니다. 현재 잔액: 1000원"));

        //when & then
        assertThatThrownBy(() -> pointController.use(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("포인트 잔액이 부족합니다.");
    }

    @Test
    @DisplayName("PLC_PNT_001: 부정 충전 차단 테스트")
    public void 부정_충전_차단_테스트() {
        //given
        Long userId = 1L;
        Long amount = 1000L;
        
        // 부정 충전 시나리오: 동시에 여러 건의 충전 요청
        when(pointService.chargePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("부정 충전이 감지되었습니다."));

        //when & then
        assertThatThrownBy(() -> pointController.charge(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("부정 충전이 감지되었습니다.");
    }

    @Test
    @DisplayName("최대 잔고 초과시 충전 실패")
    public void 최대_잔고_초과시_충전_실패() {
        //given
        Long userId = 1L;
        Long amount = 500_000L; // 50만원 충전
        when(pointService.chargePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("최대 보유 가능한 포인트는 5000000원입니다."));

        //when & then
        assertThatThrownBy(() -> pointController.charge(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("최대 보유 가능한 포인트는 5000000원입니다.");
    }

    // ==================== 경계값 테스트 ====================

    @Test
    @DisplayName("음수 금액 충전 시 예외 발생")
    public void 음수_금액_충전시_예외발생() {
        //given
        Long userId = 1L;
        Long amount = -1000L;
        when(pointService.chargePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."));

        //when & then
        assertThatThrownBy(() -> pointController.charge(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("음수 금액 사용 시 예외 발생")
    public void 음수_금액_사용시_예외발생() {
        //given
        Long userId = 1L;
        Long amount = -500L;
        when(pointService.usePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("사용 금액은 0보다 커야 합니다."));

        //when & then
        assertThatThrownBy(() -> pointController.use(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("0원 충전 시 예외 발생")
    public void 영원_충전시_예외발생() {
        //given
        Long userId = 1L;
        Long amount = 0L;
        when(pointService.chargePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."));

        //when & then
        assertThatThrownBy(() -> pointController.charge(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("null 사용자 ID로 조회 시 예외 발생")
    public void null_사용자ID_조회시_예외발생() {
        //given
        Long userId = null;
        when(pointService.getPointById(userId))
            .thenThrow(new IllegalArgumentException("올바른 사용자 ID를 입력해주세요."));

        //when & then
        assertThatThrownBy(() -> pointController.point(userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("올바른 사용자 ID를 입력해주세요.");
    }

    @Test
    @DisplayName("음수 사용자 ID로 조회 시 예외 발생")
    public void 음수_사용자ID_조회시_예외발생() {
        //given
        Long userId = -1L;
        when(pointService.getPointById(userId))
            .thenThrow(new IllegalArgumentException("올바른 사용자 ID를 입력해주세요."));

        //when & then
        assertThatThrownBy(() -> pointController.point(userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("올바른 사용자 ID를 입력해주세요.");
    }

    // ==================== 정상 경계값 테스트 ====================

    @Test
    @DisplayName("충전 한도 경계값 테스트 - 정확히 100만원")
    public void 충전_한도_경계값_테스트_정확히_100만원() {
        //given
        Long userId = 1L;
        Long amount = 1_000_000L; // 정확히 100만원
        UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(pointService.chargePoint(userId, amount)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.charge(userId, amount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.point()).isEqualTo(amount);
        
        // TODO: 경계값 테스트는 중요한데, 더 많은 케이스가 필요할 것 같음
        // 예: 999,999원, 1,000,001원 등
    }

    @Test
    @DisplayName("고액 사용 경계값 테스트 - 정확히 5만원")
    public void 고액_사용_경계값_테스트_정확히_5만원() {
        //given
        Long userId = 1L;
        Long amount = 50_000L; // 정확히 5만원
        when(pointService.usePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("5만원 이상 사용시 본인 인증이 필요합니다."));

        //when & then
        assertThatThrownBy(() -> pointController.use(userId, amount))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("5만원 이상 사용시 본인 인증이 필요합니다.");
    }

    @Test
    @DisplayName("고액 사용 경계값 테스트 - 49999원 (정상)")
    public void 고액_사용_경계값_테스트_49999원_정상() {
        //given
        Long userId = 1L;
        Long amount = 49_999L; // 49,999원 (5만원 미만)
        UserPoint mockUserPoint = new UserPoint(userId, 0L, System.currentTimeMillis());
        when(pointService.usePoint(userId, amount)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.use(userId, amount);

        //then
        assertThat(result).isNotNull();
        // TODO: 49,999원은 정상 처리되어야 하는데, 실제로는 어떻게 될까?
    }

    /*
     * TODO: 추가로 테스트하면 좋을 것들
     * 1. 동시성 테스트 (여러 스레드가 동시에 충전/사용)
     * 2. 성능 테스트 (대용량 데이터에서의 응답 시간)
     * 3. 통합 테스트 (실제 DB와 연동)
     * 4. 파라미터화된 테스트 (JUnit5의 @ParameterizedTest)
     * 5. 테스트 데이터 빌더 패턴 적용
     * 6. 커스텀 매처 생성 (AssertJ 확장)
     * 7. 테스트 그룹핑 (@Nested 클래스 활용)
     * 8. 테스트 시나리오 기반 테스트
     */
}
