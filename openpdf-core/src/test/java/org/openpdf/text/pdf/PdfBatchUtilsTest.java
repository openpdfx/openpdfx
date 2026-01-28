package org.openpdf.text.pdf;

import org.junit.jupiter.api.Test;
import org.openpdf.text.Document;
import org.openpdf.text.Paragraph;
import org.openpdf.text.utils.PdfBatch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PdfBatchUtils to ensure it runs batch jobs on virtual threads.
 */
class PdfBatchUtilsTest {

    private static Path tinyPdf(String prefix) throws Exception {
        Path p = Files.createTempFile(prefix, ".pdf");
        var doc = new Document();
        try (var out = new FileOutputStream(p.toFile())) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Hello OpenPDF"));
            doc.close();
        }
        return p;
    }

    private static Path multiPagePdf(String prefix, int pageCount) throws Exception {
        Path p = Files.createTempFile(prefix, ".pdf");
        var doc = new Document();
        try (var out = new FileOutputStream(p.toFile())) {
            PdfWriter.getInstance(doc, out);
            doc.open();
            for (int i = 1; i <= pageCount; i++) {
                if (i > 1) {
                    doc.newPage();
                }
                doc.add(new Paragraph("Page " + i));
            }
            doc.close();
        }
        return p;
    }


    @Test
    void runBatch_usesVirtualThreads() {
        // We don't create any executors here; runBatch does that internally.
        var tasks = List.of(
                (Callable<Integer>) () -> {
                    assertTrue(Thread.currentThread().isVirtual(), "Task should run on a virtual thread");
                    return 1;
                },
                (Callable<Integer>) () -> {
                    assertTrue(Thread.currentThread().isVirtual(), "Task should run on a virtual thread");
                    return 2;
                },
                (Callable<Integer>) () -> {
                    assertTrue(Thread.currentThread().isVirtual(), "Task should run on a virtual thread");
                    return 3;
                }
        );

        var result = PdfBatch.run(tasks, v -> {}, t -> fail(t));
        assertTrue(result.isAllSuccessful(), "All tasks should succeed");
        assertEquals(3, result.successes.size());
        assertEquals(0, result.failures.size());
    }

    @Test
    void batchMerge_createsOutput_and_runsOnVirtualThreads() throws Exception {
        // Prepare inputs
        Path a = tinyPdf("a-");
        Path b = tinyPdf("b-");
        Path merged = Files.createTempFile("merged-", ".pdf");

        // Use the batch API which internally uses virtual threads.
        var jobs = List.of(new PdfBatchUtils.MergeJob(List.of(a, b), merged));

        var result = PdfBatchUtils.batchMerge(jobs,
                // onSuccess: simple sanity checks per job
                out -> {
                    assertTrue(Files.exists(out), "Merged PDF should exist");
                    try {
                        assertTrue(Files.size(out) > 0, "Merged PDF should be non-empty");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                t -> fail(t)
        );

        assertTrue(result.isAllSuccessful(), "Merge job should succeed");

        // Cleanup
        Files.deleteIfExists(a);
        Files.deleteIfExists(b);
        Files.deleteIfExists(merged);
    }

    @Test
    void rotatePages_rotateTo90Degrees() throws Exception {
        Path input = multiPagePdf("rotate-input-", 3);
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate all pages 90 degrees
        Path result = PdfBatchUtils.rotatePages(input, output, 90);

        assertNotNull(result);
        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);

        // Verify rotation was applied
        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(3, reader.getNumberOfPages());
            for (int i = 1; i <= 3; i++) {
                assertEquals(90, reader.getPageRotation(i), 
                    "Page " + i + " should be rotated 90 degrees");
            }
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePages_rotateTo180Degrees() throws Exception {
        Path input = multiPagePdf("rotate-input-", 2);
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate all pages 180 degrees
        PdfBatchUtils.rotatePages(input, output, 180);

        // Verify rotation was applied
        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(2, reader.getNumberOfPages());
            assertEquals(180, reader.getPageRotation(1));
            assertEquals(180, reader.getPageRotation(2));
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePages_rotateTo270Degrees() throws Exception {
        Path input = tinyPdf("rotate-input-");
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate all pages 270 degrees
        PdfBatchUtils.rotatePages(input, output, 270);

        // Verify rotation was applied
        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(270, reader.getPageRotation(1));
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePages_withNegativeRotation() throws Exception {
        Path input = tinyPdf("rotate-input-");
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate with negative angle (should normalize to 270)
        PdfBatchUtils.rotatePages(input, output, -90);

        // Verify rotation was applied and normalized
        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(270, reader.getPageRotation(1));
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePages_cumulativeRotation() throws Exception {
        Path input = tinyPdf("rotate-input-");
        Path temp = Files.createTempFile("rotate-temp-", ".pdf");
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // First rotation: 90 degrees
        PdfBatchUtils.rotatePages(input, temp, 90);
        
        // Second rotation: another 90 degrees (total should be 180)
        PdfBatchUtils.rotatePages(temp, output, 90);

        // Verify cumulative rotation
        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(180, reader.getPageRotation(1));
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(temp);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePages_invalidRotation_throwsException() throws Exception {
        Path input = tinyPdf("rotate-input-");
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Invalid rotation angle
        assertThrows(IllegalArgumentException.class, 
            () -> PdfBatchUtils.rotatePages(input, output, 45));

        assertThrows(IllegalArgumentException.class, 
            () -> PdfBatchUtils.rotatePages(input, output, 100));

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePage_singlePage() throws Exception {
        Path input = multiPagePdf("rotate-input-", 3);
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate only page 2 by 90 degrees
        PdfBatchUtils.rotatePage(input, output, 2, 90);

        // Verify only page 2 was rotated
        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(3, reader.getNumberOfPages());
            assertEquals(0, reader.getPageRotation(1), "Page 1 should not be rotated");
            assertEquals(90, reader.getPageRotation(2), "Page 2 should be rotated 90 degrees");
            assertEquals(0, reader.getPageRotation(3), "Page 3 should not be rotated");
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePage_firstPage() throws Exception {
        Path input = multiPagePdf("rotate-input-", 2);
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate first page
        PdfBatchUtils.rotatePage(input, output, 1, 180);

        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(180, reader.getPageRotation(1));
            assertEquals(0, reader.getPageRotation(2));
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePage_lastPage() throws Exception {
        Path input = multiPagePdf("rotate-input-", 3);
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Rotate last page
        PdfBatchUtils.rotatePage(input, output, 3, 270);

        try (PdfReader reader = new PdfReader(Files.readAllBytes(output))) {
            assertEquals(0, reader.getPageRotation(1));
            assertEquals(0, reader.getPageRotation(2));
            assertEquals(270, reader.getPageRotation(3));
        }

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void rotatePage_invalidPageNumber_throwsException() throws Exception {
        Path input = multiPagePdf("rotate-input-", 2);
        Path output = Files.createTempFile("rotate-output-", ".pdf");

        // Page 0 (invalid)
        assertThrows(IllegalArgumentException.class, 
            () -> PdfBatchUtils.rotatePage(input, output, 0, 90));

        // Page beyond total pages
        assertThrows(IllegalArgumentException.class, 
            () -> PdfBatchUtils.rotatePage(input, output, 10, 90));

        // Cleanup
        Files.deleteIfExists(input);
        Files.deleteIfExists(output);
    }

    @Test
    void batchRotatePages_multipleFiles() throws Exception {
        Path input1 = tinyPdf("rotate1-");
        Path input2 = tinyPdf("rotate2-");
        Path output1 = Files.createTempFile("rotated1-", ".pdf");
        Path output2 = Files.createTempFile("rotated2-", ".pdf");

        var jobs = List.of(
            new PdfBatchUtils.RotateJob(input1, output1, 90),
            new PdfBatchUtils.RotateJob(input2, output2, 180)
        );

        var result = PdfBatchUtils.batchRotatePages(jobs,
            out -> assertTrue(Files.exists(out)),
            t -> fail(t)
        );

        assertTrue(result.isAllSuccessful());
        assertEquals(2, result.successes.size());

        // Verify rotations
        try (PdfReader reader1 = new PdfReader(Files.readAllBytes(output1))) {
            assertEquals(90, reader1.getPageRotation(1));
        }
        try (PdfReader reader2 = new PdfReader(Files.readAllBytes(output2))) {
            assertEquals(180, reader2.getPageRotation(1));
        }

        // Cleanup
        Files.deleteIfExists(input1);
        Files.deleteIfExists(input2);
        Files.deleteIfExists(output1);
        Files.deleteIfExists(output2);
    }
}
