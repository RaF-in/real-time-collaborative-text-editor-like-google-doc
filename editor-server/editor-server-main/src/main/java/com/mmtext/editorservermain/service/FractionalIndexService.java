package com.mmtext.editorservermain.service;
import org.springframework.stereotype.Service;

@Service
public class FractionalIndexService {

    private static final char MIN_CHAR = 'a';
    private static final char MAX_CHAR = 'z';
    private static final int BASE = MAX_CHAR - MIN_CHAR + 1;
    private static final String START_INDEX = "a";
    private static final String END_INDEX = "z";

    /**
     * Generate an index between two existing indices (i1 < i2).
     * This is a correct implementation of fractional indexing used in real CRDTs.
     */
    public String generateIndexBetween(String indexBefore, String indexAfter) {
        // Normalize boundaries
        String i1 = (indexBefore == null || indexBefore.isEmpty()) ? START_INDEX : indexBefore;
        String i2 = (indexAfter == null || indexAfter.isEmpty()) ? END_INDEX : indexAfter;
        return generate(i1, i2, "");
    }

    /**
     * Recursive fractional index generator.
     *
     * prefix = the already-confirmed common prefix
     * a = remaining part of i1
     * b = remaining part of i2
     */
    private String generate(String a, String b, String prefix) {
        char c1 = a.isEmpty() ? MIN_CHAR : a.charAt(0);
        char c2 = b.isEmpty() ? MAX_CHAR : b.charAt(0);

        int v1 = c1 - MIN_CHAR;
        int v2 = c2 - MIN_CHAR;

        // Case 1: Equal characters → continue deeper
        if (v1 == v2) {
            return generate(
                    a.isEmpty() ? "" : a.substring(1),
                    b.isEmpty() ? "" : b.substring(1),
                    prefix + c1
            );
        }

        // Case 2: There is space between them
        if (v1 + 1 < v2) {
            int mid = (v1 + v2) / 2;  // midpoint
            return prefix + (char) (MIN_CHAR + mid);
        }

        // Case 3: NO SPACE → must descend deeper
        // Example: c1='b'(1), c2='c'(2) → no midpoint.
        // We lock c1 into prefix and recurse.
        return generate(
                a.length() <= 1 ? "" : a.substring(1), // drop first char of a
                "",                                    // b = infinite upper bound
                prefix + c1
        );
    }

    /**
     * Compare two fractional indices
     * @return negative if pos1 < pos2, 0 if equal, positive if pos1 > pos2
     */
    public int compare(String pos1, String pos2) {
        if (pos1 == null || pos2 == null) {
            throw new IllegalArgumentException("Positions cannot be null");
        }
        return pos1.compareTo(pos2);
    }
}
