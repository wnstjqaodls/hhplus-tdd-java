package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class PointControllerIntegrationTest {

    @MockBean // Spring Context 의 Bean 을 Mock 으로 교체 .. > 뭔말이야
    private PointService pointService;

    @Spy
    private PointService pointServiceMock;


    @Test
    @DisplayName("Mock 과 Stub , Spy 의 차이를 비교해보자")
    void customStubVsOthers() {

        // 직접 구현한 Stub
        UserPoint userPoint = new UserPoint(1L, 7000L,System.currentTimeMillis());
        MyStub customStub = new MyStub(userPoint);

        // 커스텀 stub 은 항상 미리 정의된 값 반환함
        UserPoint stubResult1 = customStub.getUserPoint(1L);
        // 미리 정의된 유저만 업데이트함
        customStub.updatePoint(1L);

        // 왜 @SpringBootTest 에선 assertThat 못쓰는지..? >
        assert stubResult1.equals(userPoint);
        // TODO : 여기부터 해야함.


    }

    static class MyStub {

        private final UserPoint userPoint;

        public MyStub (UserPoint userPoint) {
            this.userPoint = userPoint;
        }

        public UserPoint getUserPoint(Long id) {
            // 미리 정의된 항상 같은 값을 반환함.
            return userPoint;
        }

        public void updatePoint (Long id) {
            // XXX : 이렇게 리턴값이 없는 경우, 반환값이없는것도 Stub 이라고 할수있는지
            // 포인트를 업데이트함

        }


    }







}
