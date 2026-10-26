/**
 * Shared kernel: money, billing period, audit, portable paths and storage settings.
 *
 * <p>Implements RNF-01, RNF-05, POR-03, POR-04, POR-10, POR-11 (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.shared;
