package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.awt.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointControllerTest {

    @BeforeEach
    void setUp() {
    // 데이터 준비 : 0 으로 초기화
        UserPoint userPoint = UserPoint.builder()
            .id(0)
            .point(0)
            .updateMillis(System.currentTimeMillis()).build();
        ResponseEntity<ResponseDTO> responseDTO = new ResponseEntity<ResponseDTO>();

    }


    @Test
    @DisplayName("특정유저 포인트 조회")
    public UserPoint 특정유저의_포인트를_조회한다() {
        //given
        Long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 0L, System.currentTimeMillis());

        //when
        // 실제 테스트 로직이 들어갈 부분
        
        //then
        assertThat(userPoint).isNotNull(); //
        assertThat(userPoint.id()).isEqualTo(userId);
        return userPoint;
    }

    @Test
    @DisplayName("특정유저 포인트 충전 이용내역 조회")
    public List<UserPoint> 특정유저의_포인트_충전이용내역_조회() {
        //given
        Long userId = 1L;
        
        //when
        // 실제 테스트 로직이 들어갈 부분
        
        //then
        // 검증 로직이 들어갈 부분
        return null;
    }

    @Test
    @DisplayName("특정유저 포인트 충전")
    public ResponseEntity<ResponseDTO> 특정유저의_포인트를_충전한다 (Long id ) {
        // TODO LAST : Step01 기능추가및 테스트구현중
        // How to TDD By Example (1) > Step 1 : 실패하는 테스트작성부분 참고중 이었음

        return new ResponseEntity<ResponseDTO>();
    }

    @Test
    @DisplayName("특정유저 포인트 사용")
    public ResponseEntity<ResponseDTO>(Long id) {


        return null;
    }

}
