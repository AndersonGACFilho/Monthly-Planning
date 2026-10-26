/**
 * Monthly overview: totals by category, member and card, committed installments.
 *
 * <p>Implements RF-10 to RF-17, RF-25, RF-27, RF-36 to RF-38 (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.summary;
