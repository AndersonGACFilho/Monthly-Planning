package com.planejamentomensal.summary.api;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.summary.application.CarryOverUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carries what third parties still owe at the end of a period into the next one, and undoes it (RF-28).
 *
 * @since 1.0
 */
@RestController
public class CarryOverController {

    private final CarryOverUseCase useCase;

    /**
     * Creates the controller.
     *
     * @param useCase carry-over use case
     */
    public CarryOverController(CarryOverUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Carries the unpaid amount of each member into the next period, replacing what was carried before.
     *
     * @param billingPeriod source period, {@code yyyy-MM}
     * @param request       members
     * @return the carry-overs
     */
    @PostMapping("/api/summaries/{billingPeriod}/carry-overs")
    public List<CarryOverModel> carryOver(@PathVariable String billingPeriod, @Valid @RequestBody CarryOverRequest request) {
        return useCase.carry(BillingPeriod.parse(billingPeriod), request.memberIds()).stream().map(CarryOverModel::of)
                .toList();
    }

    /**
     * Removes the carry-over of a member out of a period.
     *
     * @param billingPeriod source period, {@code yyyy-MM}
     * @param memberId      member
     * @return 204
     */
    @DeleteMapping("/api/summaries/{billingPeriod}/carry-overs/{memberId}")
    public ResponseEntity<Void> undoCarryOver(@PathVariable String billingPeriod, @PathVariable long memberId) {
        useCase.undo(BillingPeriod.parse(billingPeriod), memberId);
        return ResponseEntity.noContent().build();
    }
}
