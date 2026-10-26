/**
 * Import of Itau bills (PDF and XLSX): parsing, preview, confirmation and undo.
 *
 * <p>Implements RF-60 to RF-73, RNF-03, RNF-04, RNF-08 (see {@code docs/SPEC.md}).</p>
 *
 * <p>Layers: {@code api} (controllers, DTOs, assemblers), {@code application} (one use case per class
 * and its ports), {@code domain} (entities, value objects, policies) and {@code infra} (JPA repositories,
 * file adapters).</p>
 */
package com.planejamentomensal.imports;
