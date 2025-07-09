package io.hhplus.tdd.point;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PointHistory {
    private final long id;
    private final long userId;
    private final long amount;
    private final TransactionType type;
    private final long updateMillis;
}
