/**
 * Purchases, card bill entries, installments, refunds, splits between members and the review queue.
 *
 * <p>Implements RN-01 to RN-14, RN-16 to RN-29, RF-26, RF-40 to RF-45, RF-74 to RF-79d (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.entry;
