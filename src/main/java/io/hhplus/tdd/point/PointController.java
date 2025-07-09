package io.hhplus.tdd.point;

import io.hhplus.tdd.point.service.PointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TODO: @Controller vs @RestController 차이점 학습함
// @Controller: 뷰를 반환 (MVC 패턴)
// @RestController: @Controller + @ResponseBody, JSON 형태로 데이터 반환 (REST API)
// 결론: REST API를 만들 때는 @RestController 사용!
@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;

    // TODO: 생성자 주입 vs 필드 주입 vs 세터 주입 중에 고민
    // 생성자 주입을 선택한 이유: 1) 불변성 보장 2) 테스트 용이성 3) 필수 의존성 명시
    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    /**
     * 특정 유저의 포인트를 조회하는 기능
     * TODO: 캐싱 적용하면 성능 향상될 듯 (@Cacheable)
     * TODO: 응답 형태를 ResponseEntity로 감싸는 게 좋을까?
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        log.info("포인트 조회 요청: userId={}", id);
        
        try {
            UserPoint result = pointService.getPointById(id);
            log.info("포인트 조회 성공: userId={}, point={}", id, result.getPoint());
            return result;
        } catch (Exception e) {
            log.error("포인트 조회 실패: userId={}, error={}", id, e.getMessage());
            throw e; // TODO: 글로벌 예외 처리기에서 처리하도록 다시 던짐
        }
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
     * TODO: 페이징 처리 필요 (@PageableDefault, Pageable 파라미터)
     * TODO: 조회 조건 추가 (기간별, 유형별 필터링)
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        log.info("포인트 내역 조회 요청: userId={}", id);
        
        try {
            List<PointHistory> result = pointService.getPointHistoryById(id);
            log.info("포인트 내역 조회 성공: userId={}, count={}", id, result.size());
            return result;
        } catch (Exception e) {
            log.error("포인트 내역 조회 실패: userId={}, error={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * 특정 유저의 포인트를 충전하는 기능
     * TODO: 요청 DTO 클래스 만들어서 validation 추가 (@Valid, @NotNull 등)
     * TODO: 비동기 처리 고려 (@Async)
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount  // TODO: DTO로 변경 예정
    ) {
        log.info("포인트 충전 요청: userId={}, amount={}", id, amount);
        
        try {
            UserPoint result = pointService.chargePoint(id, amount);
            log.info("포인트 충전 성공: userId={}, beforePoint={}, afterPoint={}", 
                    id, result.getPoint() - amount, result.getPoint());
            return result;
        } catch (Exception e) {
            log.error("포인트 충전 실패: userId={}, amount={}, error={}", id, amount, e.getMessage());
            throw e;
        }
    }

    /**
     * 특정 유저의 포인트를 사용하는 기능
     * TODO: 사용 목적, 상품 정보 등 추가 정보 받아야 할 듯
     * TODO: 트랜잭션 처리 강화 (@Transactional)
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount  // TODO: 사용 목적 등 추가 정보 포함한 DTO로 변경
    ) {
        log.info("포인트 사용 요청: userId={}, amount={}", id, amount);
        
        try {
            UserPoint result = pointService.usePoint(id, amount);
            log.info("포인트 사용 성공: userId={}, beforePoint={}, afterPoint={}", 
                    id, result.getPoint() + amount, result.getPoint());
            return result;
        } catch (Exception e) {
            log.error("포인트 사용 실패: userId={}, amount={}, error={}", id, amount, e.getMessage());
            throw e;
        }
    }
}
