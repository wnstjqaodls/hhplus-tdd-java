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

@ExtendWith(MockitoExtension.class)
class PointControllerTest {

    private static final Logger log = LoggerFactory.getLogger(PointControllerTest.class);

    @Mock
    private PointService pointService;

    private PointController pointController;

    @BeforeEach
    void setUp() {
        pointController = new PointController(pointService);
    }

    @Test
    @DisplayName("특정유저 포인트 조회")
    public void 특정유저의_포인트를_조회한다() {
        //given
        Long userId = 1L;
        UserPoint mockUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(pointService.getPointById(userId)).thenReturn(mockUserPoint);

        //when
        UserPoint result = pointController.point(userId);

        //then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
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

    @Test
    @DisplayName("PLC_PNT_008: 충전 한도 초과 방지")
    public void 충전_한도_초과시_예외발생() {
        //given
        Long userId = 1L;
        Long amount = 1_500_000L; // 150만원 (한도 초과)
        when(pointService.chargePoint(userId, amount))
            .thenThrow(new IllegalArgumentException("한번에 충전할 수 있는 최대 금액은 100만원입니다."));

        //when & then
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
            .thenThrow(new IllegalArgumentException("포인트 잔액이 부족합니다."));

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
}
