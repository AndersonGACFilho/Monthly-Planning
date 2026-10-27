package com.planejamentomensal.statement.application.workbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;

/**
 * Removes from a model file what the app rebuilds or never exports, before POI opens it (XLS-12): pivot tables
 * and their caches (rebuilt per formula mode), the calculation chain, and document-library metadata.
 *
 * @since 1.0
 */
final class ModelPackageCleaner {

    private static final String PIVOT_TABLE = "/relationships/pivotTable";
    private static final String PIVOT_CACHE = "/relationships/pivotCacheDefinition";
    private static final String CALC_CHAIN = "/relationships/calcChain";
    private static final String CUSTOM_XML = "/relationships/customXml";
    private static final String CUSTOM_PROPERTIES = "/relationships/custom-properties";

    private ModelPackageCleaner() {
    }

    /**
     * Cleans a model.
     *
     * @param model XLSX bytes
     * @return cleaned XLSX bytes
     */
    static byte[] clean(byte[] model) {
        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(model)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<PackagePart> doomed = new ArrayList<>();
            for (PackagePart part : pkg.getParts()) {
                if (part.isRelationshipPart()) {
                    continue;
                }
                for (PackageRelationship rel : new ArrayList<>(collect(part))) {
                    String type = rel.getRelationshipType();
                    if (type.endsWith(PIVOT_TABLE) || type.endsWith(PIVOT_CACHE) || type.endsWith(CALC_CHAIN)
                            || type.endsWith(CUSTOM_XML)) {
                        PackagePart target = pkg.getPart(PackagingURIHelper.createPartName(
                                PackagingURIHelper.resolvePartUri(part.getPartName().getURI(), rel.getTargetURI())));
                        if (target != null) {
                            doomed.add(target);
                        }
                        part.removeRelationship(rel.getId());
                    }
                }
            }
            for (PackageRelationship rel : new ArrayList<>(collectPackage(pkg))) {
                if (rel.getRelationshipType().endsWith(CUSTOM_PROPERTIES)) {
                    PackagePart target = pkg.getPart(PackagingURIHelper.createPartName(rel.getTargetURI()));
                    if (target != null) {
                        doomed.add(target);
                    }
                    pkg.removeRelationship(rel.getId());
                }
            }
            for (PackagePart part : doomed) {
                removeWithChildren(pkg, part);
            }
            PackagePart workbook = pkg.getPart(PackagingURIHelper.createPartName("/xl/workbook.xml"));
            if (workbook != null) {
                String xml;
                try (InputStream in = workbook.getInputStream()) {
                    xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                String cleaned = xml.replaceAll("<pivotCaches>.*?</pivotCaches>", "")
                        .replaceAll("<definedName [^>]*>[^<]*#REF![^<]*</definedName>", "");
                if (!cleaned.equals(xml)) {
                    try (OutputStream os = workbook.getOutputStream()) {
                        os.write(cleaned.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            pkg.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidFormatException e) {
            throw new IllegalArgumentException("Not a spreadsheet model", e);
        }
    }

    private static List<PackageRelationship> collect(PackagePart part) throws InvalidFormatException {
        List<PackageRelationship> list = new ArrayList<>();
        part.getRelationships().forEach(list::add);
        return list;
    }

    private static List<PackageRelationship> collectPackage(OPCPackage pkg) {
        List<PackageRelationship> list = new ArrayList<>();
        pkg.getRelationships().forEach(list::add);
        return list;
    }

    private static void removeWithChildren(OPCPackage pkg, PackagePart part) throws InvalidFormatException {
        if (!pkg.containPart(part.getPartName())) {
            return;
        }
        for (PackageRelationship rel : collect(part)) {
            if (rel.getTargetMode() != null && rel.getTargetMode().name().equals("EXTERNAL")) {
                continue;
            }
            PackagePart child = pkg.getPart(PackagingURIHelper.createPartName(
                    PackagingURIHelper.resolvePartUri(part.getPartName().getURI(), rel.getTargetURI())));
            if (child != null) {
                removeWithChildren(pkg, child);
            }
        }
        pkg.removePart(part);
    }
}
