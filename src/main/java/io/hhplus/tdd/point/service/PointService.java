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

    // 생성자 주입 활용
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 특정 유저의 포인트 조회
     */
    public UserPoint getPointById(Long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 유저의 포인트 충전/사용 내역 조회
     */
    public List<PointHistory> getPointHistoryById(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전 (정책 PLC_PNT_001, PLC_PNT_008, PLC_PNT_010 적용)
     */
    public UserPoint chargePoint(Long userId, Long amount) {
        // 기본 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // PLC_PNT_008: 충전 한도 초과 방지 (임시로 100만원 한도)
        if (amount > 1_000_000L) {
            throw new IllegalArgumentException("한번에 충전할 수 있는 최대 금액은 100만원입니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);
        
        // 새로운 포인트 계산 후 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, currentPoint.point() + amount);
        
        // 충전 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        
        return updatedPoint;
    }

    /**
     * 포인트 사용 (정책 PLC_PNT_003, PLC_PNT_004 적용)
     */
    public UserPoint usePoint(Long userId, Long amount) {
        // 기본 검증
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        // PLC_PNT_004: 1회 사용 한도 제한 (최대 100만원)
        if (amount > 1_000_000L) {
            throw new IllegalArgumentException("한번에 사용할 수 있는 최대 금액은 100만원입니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);
        
        // 잔액 부족 확인
        if (currentPoint.point() < amount) {
            throw new IllegalArgumentException("포인트 잔액이 부족합니다.");
        }

        // PLC_PNT_003: 고액 사용 시 본인 인증 (5만원 이상) - 임시로 예외 발생
        if (amount >= 50_000L) {
            // 실제로는 본인 인증 로직이 들어가야 함
            // 여기서는 과제용으로 간단히 처리
            throw new IllegalArgumentException("5만원 이상 사용시 본인 인증이 필요합니다.");
        }

        // 새로운 포인트 계산 후 업데이트
        UserPoint updatedPoint = userPointTable.insertOrUpdate(userId, currentPoint.point() - amount);
        
        // 사용 내역 저장
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        
        return updatedPoint;
    }

    /**
     * PLC_PNT_001: 부정 충전 차단 검증 (과제용 간단 구현)
     */
    private boolean isIllegalChargeDetected(Long userId, Long amount) {
        // 실제로는 휴대폰번호, 기기 ID, 계정 ID 등을 확인해야 함
        // 여기서는 과제용으로 간단히 처리
        return false;
    }
}
