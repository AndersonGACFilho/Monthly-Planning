package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.DeliveryChannelType;

/**
 * Contract of a way to deliver a statement (SOL-03). Implementations never throw for a delivery problem: they
 * return {@link DeliveryResult#failed(String)} so a batch goes on (RF-91).
 *
 * @since 1.0
 */
public interface DeliveryChannel {

    /**
     * Returns the channel this implementation serves.
     *
     * @return channel type
     */
    DeliveryChannelType type();

    /**
     * Delivers one statement.
     *
     * @param delivery statement, rendered texts and files
     * @return result
     */
    DeliveryResult deliver(DeliveryPackage delivery);
}
