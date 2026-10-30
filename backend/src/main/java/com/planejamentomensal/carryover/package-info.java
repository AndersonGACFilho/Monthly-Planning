/**
 * Amounts a third-party member still owes at the end of a billing period, carried into the next one without moving
 * any entry out of its own period.
 *
 * <p>Implements RF-28 (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.carryover;
