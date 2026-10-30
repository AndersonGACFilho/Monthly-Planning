package com.planejamentomensal.summary.api;

import com.planejamentomensal.carryover.application.CarryOverView;
import java.math.BigDecimal;

/**
 * Amount a member carried from one period into the next (RF-28).
 *
 * @param memberId   member who owes
 * @param fromPeriod source period, {@code yyyy-MM}
 * @param toPeriod   period the amount is owed in, {@code yyyy-MM}
 * @param amount     positive amount
 * @since 1.0
 */
public record CarryOverModel(long memberId, String fromPeriod, String toPeriod, BigDecimal amount) {

    static CarryOverModel of(CarryOverView v) {
        return new CarryOverModel(v.memberId(), v.fromPeriod().toString(), v.toPeriod().toString(), v.amount().amount());
    }
}
