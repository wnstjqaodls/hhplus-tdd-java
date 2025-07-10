package io.hhplus.tdd.point;

import io.hhplus.tdd.helper.TestLogger;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// TODO: @SpringBootTest vs @ExtendWith(MockitoExtension.class) 차이점 학습함
// @SpringBootTest: 스프링 컨텍스트 전체를 로드 (무겁고 느림) , 통테용임
// @ExtendWith(MockitoExtension.class): 단위 테스트용, Mock 객체만 사용 (가볍고 빠름)
// @ExtendWith : Junit4 에서 @RunWith로 사용되던거임, @ExtendWith는 메인으로 실행될 Class를 지정할수있음
// 결론: 단위 테스트에서는 MockitoExtension을 사용하는 게 좋음!
@ExtendWith({MockitoExtension.class, TestLogger.class})
class PointControllerTest {

    private static final Logger log = LoggerFactory.getLogger(PointControllerTest.class);

    @Mock
    private PointService pointService;

    //@InjectMocks // inject Mocks 는 동일클래스안에 @Mock 또는 @spy 로 선언된 객체를 주입해주는데
    //생성자,세터,필드 순으로 가능한 주입점을 찾아 리플렉션으로 삽입해준다. >
    //@Autowired 주입 불가능 : @ExtendWith 만 사용시, 컨테이너가 없기때문에 주입이 되지않음!
    private PointController pointController;

    // 테스트id 생성용
    private static final Random random = new Random();
    
    // 테스트에서 공통으로 사용할 랜덤 userId
    private Long userId;

    @BeforeEach
    void setUp() {
        // XXX : 처음에는 @InjectMocks를 쓰려고 했는데, 직접 생성자 호출방식으로 변경함
        // XXX : 이유 :  InjectMocks 보다 명시적이고, 에러시 컴파일타임에 에러를 확인가능함,
        // XXX : 이유2 : 현재 해당 컨트롤러객체의 의존성이 단순함 postService 한개뿐이어서 리플렉션을 사용하는 injectMocks 보다 가벼움
        pointController = new PointController(pointService);
        userId = random.nextLong(1_000); // 0-999 중 랜덤값을 인스턴스 필드에 할당
    }

    // ==================== 기본 기능 테스트 ====================
    @Test
    @DisplayName("특정유저 포인트 충전 이용내역 조회_성공")
    public void Point_ProductionOfCertainUsers() {
        //given
        // userId는 @BeforeEach에서 랜덤으로 생성됨 (더 이상 선언할 필요 없음)
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
    @DisplayName("특정유저 포인트 조회_성공")
    public void searchOfThePointOfASpecificUser() {
        //given
        // userId는 @BeforeEach에서 랜덤으로 생성됨
        UserPoint mockUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(pointService.getPointById(userId)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.point(userId);

        //then
        // TODO: assertEquals vs assertThat 중에 고민했는데, AssertJ가 더 읽기 쉬워서 선택
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.id()).isEqualTo(1000L);
        
        // TODO: 로그도 확인해야 할까? 일단 보류
        log.info("포인트 조회 테스트 완료: {}", result);
    }



    @Test
    @DisplayName("특정유저의_포인트를_충전한다")
    public void completeThePointOfASpecificUser() {
        //given
        // userId는 @BeforeEach에서 랜덤으로 생성됨  
        Long amount = 1000L;
        UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(pointService.chargePoint(userId, amount)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.charge(userId, amount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.id()).isEqualTo(amount);
        
        // TODO: 충전 후 실제로 포인트가 증가했는지 확인하는 테스트도 필요할 듯
    }

    @Test
    @DisplayName("특정유저 포인트 사용_성공")
    public void use_pointOfASpecificUser() {
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
    public void charging_does_inAdditionOfExceptions() {
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
    public void use_remain_ifOverExpansion() {
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
    public void highAmount_WhenUsingIt() {
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
    public void balance_promaxy_postFailure() {
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
    public void neglection_hill_broading_test() {
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
    public void maximum_landing_prayer_process() {
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
    public void negative_prance_onAlsoOccurredWhenCharging() {
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
    public void negative_prance() {
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
    public void zeroAmount_WhenCharging_ExceptionsOccur() {
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

    // ==================== 정상 경계값 테스트 ====================

    @Test
    @DisplayName("충전 한도 경계값 테스트 - 정확히 100만원")
    public void chargeAmountLimitBoundary() {
        //given
        Long userId = 1L;
        Long amount = 1_000_000L;
        UserPoint mockUserPoint = new UserPoint(userId,amount,System.currentTimeMillis());



        //when
        when(pointService.chargePoint(userId,amount)).thenReturn(mockUserPoint);

        //then




        //given
        /*Long userId = 1L;
        Long amount = 1_000_000L; // 정확히 100만원
        UserPoint mockUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        when(pointService.chargePoint(userId, amount)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.charge(userId, amount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getPoint()).isEqualTo(amount);
*/
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
    }


}
