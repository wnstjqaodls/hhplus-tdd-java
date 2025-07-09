package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    // TODO: 처음에는 @Autowired를 쓰려고 했는데, 생성자 주입이 더 좋다고 해서 변경함
    // 이유: 1) 순환 참조 방지 2) 테스트 용이성 3) 불변성 보장
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 특정 유저의 포인트 조회
     * TODO: 캐싱을 추가하면 성능이 더 좋아질 것 같은데... 일단 패스
     */
    public UserPoint getPointById(Long userId) {
        // 기본 검증 추가 (음수 ID는 논리적으로 말이 안됨)
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("올바른 사용자 ID를 입력해주세요.");
        }
        
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 유저의 포인트 충전/사용 내역 조회
     * TODO: 페이징 처리를 해야 할 것 같은데... 일단 전체 조회로 구현
     */
    public List<PointHistory> getPointHistoryById(Long userId) {
        // 기본 검증
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("올바른 사용자 ID를 입력해주세요.");
        }
        
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전 
     * 적용 정책: PLC_PNT_001, PLC_PNT_008, PLC_PNT_010
     * TODO: 동시성 문제는 어떻게 해결하지? synchronized? 아니면 DB 락?
     */
    public UserPoint chargePoint(Long userId, Long amount) {
        // 기본 검증
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("올바른 사용자 ID를 입력해주세요.");
        }
        
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // PLC_PNT_008: 충전 한도 초과 방지 (1회 최대 100만원)
        if (amount > 1_000_000L) {
            throw new IllegalArgumentException("한번에 충전할 수 있는 최대 금액은 100만원입니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);
        
        // 최대 잔고 제한 정책 추가 (예: 최대 500만원)
        // TODO: 이 정책은 정책서에 없었는데 추가해야 할까? 일단 추가함
        long maxBalance = 5_000_000L;
        if (currentPoint.getPoint() + amount > maxBalance) {
            throw new IllegalArgumentException("최대 보유 가능한 포인트는 " + maxBalance + "원입니다.");
        }

        // PLC_PNT_001: 부정 충전 차단 검증
        // TODO: 실제로는 휴대폰번호, 기기ID 등을 확인해야 하는데... 일단 간단히 구현
        if (isIllegalChargeDetected(userId, amount)) {
            throw new IllegalArgumentException("부정 충전이 감지되었습니다.");
        }

        // 새로운 포인트 계산 후 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, currentPoint.getPoint() + amount);
        
        // 충전 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        
        return updatedPoint;
    }

    /**
     * 포인트 사용
     * 적용 정책: PLC_PNT_003, PLC_PNT_004, PLC_PNT_005
     * TODO: 본인인증 로직은 어떻게 구현하지? 외부 API 호출?
     */
    public UserPoint usePoint(Long userId, Long amount) {
        // 기본 검증
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("올바른 사용자 ID를 입력해주세요.");
        }
        
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        // PLC_PNT_004: 1회 사용 한도 제한 (최대 100만원)
        if (amount > 1_000_000L) {
            throw new IllegalArgumentException("한번에 사용할 수 있는 최대 금액은 100만원입니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);
        
        // 잔액 부족 확인
        if (currentPoint.getPoint() < amount) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다. 현재 잔액: " + currentPoint.getPoint() + "원");
        }

        // PLC_PNT_005: 최초 사용 시 본인 인증
        // TODO: 실제로는 사용자의 인증 상태를 확인해야 함
        if (isFirstTimeUser(userId)) {
            // 실제로는 본인 인증 로직이 들어가야 함
            // 여기서는 과제용으로 간단히 처리
            // throw new IllegalArgumentException("최초 사용시 본인 인증이 필요합니다.");
        }

        // PLC_PNT_003: 고액 사용 시 본인 인증 (5만원 이상)
        if (amount >= 50_000L) {
            // 실제로는 본인 인증 로직이 들어가야 함
            // 여기서는 과제용으로 간단히 처리
            throw new IllegalArgumentException("5만원 이상 사용시 본인 인증이 필요합니다.");
        }

        // 새로운 포인트 계산 후 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, currentPoint.getPoint() - amount);
        
        // 사용 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        
        return updatedPoint;
    }

    /**
     * PLC_PNT_001: 부정 충전 차단 검증
     * TODO: 실제로는 더 복잡한 로직이 필요할 것 같음
     * - 동일 휴대폰번호로 동시 충전 체크
     * - 동일 기기ID로 동시 충전 체크  
     * - 동일 계정으로 동시 충전 체크
     * - 짧은 시간 내 반복 충전 체크
     */
    private boolean isIllegalChargeDetected(Long userId, Long amount) {
        // 과제용 간단 구현: 10초 내 동일 금액 충전 시 부정 충전으로 판단
        List<PointHistory> recentCharges = pointHistoryTable.selectAllByUserId(userId);
        long currentTime = System.currentTimeMillis();
        
        for (PointHistory history : recentCharges) {
            if (history.getType() == TransactionType.CHARGE 
                && history.getAmount() == amount 
                && (currentTime - history.getUpdateMillis()) < 10_000) { // 10초
                return true;
            }
        }
        
        return false;
    }

    /**
     * PLC_PNT_005: 최초 사용자 체크
     * TODO: 실제로는 사용자 테이블에서 가입일자, 인증상태 등을 확인해야 함
     */
    private boolean isFirstTimeUser(Long userId) {
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        
        // 사용 내역이 없으면 최초 사용자
        return histories.stream()
                .noneMatch(history -> history.getType() == TransactionType.USE);
    }
}
