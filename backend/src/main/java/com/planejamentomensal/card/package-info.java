/**
 * Credit cards, their finals, holders and closing/due cycles.
 *
 * <p>Implements RF-02, RF-03, RF-04, RF-05 (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.card;
