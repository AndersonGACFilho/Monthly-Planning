/**
 * Members (accounts / cost shares): people or cost centres that own incomes, expenses and card entries.
 *
 * <p>Implements RF-01, RF-20, RF-80, XLS-04 (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.member;
