package com.dongjian.erp.manufacturingerpsystem.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DocSourceTraceCodec {

    private static final String PREFIX = "[#SRC=";
    private static final String ITEMS_SEGMENT = ";ITEMS=";

    private DocSourceTraceCodec() {
    }

    public static TraceMetadata parse(String rawRemark) {
        if (rawRemark == null || rawRemark.isBlank() || !rawRemark.startsWith(PREFIX)) {
            return new TraceMetadata(null, null, Collections.emptyList(), rawRemark);
        }

        int endIndex = rawRemark.indexOf(']');
        if (endIndex <= PREFIX.length()) {
            return new TraceMetadata(null, null, Collections.emptyList(), rawRemark);
        }

        String header = rawRemark.substring(PREFIX.length(), endIndex);
        String userRemark = rawRemark.substring(endIndex + 1);
        if (userRemark.startsWith(" ")) {
            userRemark = userRemark.substring(1);
        }

        int itemsSegmentIndex = header.indexOf(ITEMS_SEGMENT);
        if (itemsSegmentIndex < 0) {
            return new TraceMetadata(null, null, Collections.emptyList(), rawRemark);
        }

        String sourcePart = header.substring(0, itemsSegmentIndex);
        String itemsPart = header.substring(itemsSegmentIndex + ITEMS_SEGMENT.length());
        int separatorIndex = sourcePart.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex >= sourcePart.length() - 1) {
            return new TraceMetadata(null, null, Collections.emptyList(), rawRemark);
        }

        try {
            String sourceDocType = sourcePart.substring(0, separatorIndex);
            Long sourceDocId = Long.parseLong(sourcePart.substring(separatorIndex + 1));
            List<Long> sourceItemIds = new ArrayList<>();
            if (!itemsPart.isBlank()) {
                for (String item : itemsPart.split(",")) {
                    if (!item.isBlank()) {
                        sourceItemIds.add(Long.parseLong(item.trim()));
                    }
                }
            }
            return new TraceMetadata(sourceDocType, sourceDocId, sourceItemIds, userRemark);
        } catch (NumberFormatException ex) {
            return new TraceMetadata(null, null, Collections.emptyList(), rawRemark);
        }
    }

    public static String encode(String sourceDocType, Long sourceDocId, List<Long> sourceItemIds, String userRemark) {
        String normalizedRemark = userRemark == null ? "" : userRemark.trim();
        if (sourceDocType == null || sourceDocType.isBlank() || sourceDocId == null) {
            return normalizedRemark;
        }

        String header = PREFIX + sourceDocType.trim() + ":" + sourceDocId + ITEMS_SEGMENT
                + sourceItemIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("")
                + "]";
        return normalizedRemark.isBlank() ? header : header + " " + normalizedRemark;
    }

    public static final class TraceMetadata {
        private final String sourceDocType;
        private final Long sourceDocId;
        private final List<Long> sourceItemIds;
        private final String userRemark;

        public TraceMetadata(String sourceDocType, Long sourceDocId, List<Long> sourceItemIds, String userRemark) {
            this.sourceDocType = sourceDocType;
            this.sourceDocId = sourceDocId;
            this.sourceItemIds = sourceItemIds == null ? Collections.emptyList() : List.copyOf(sourceItemIds);
            this.userRemark = userRemark;
        }

        public String getSourceDocType() {
            return sourceDocType;
        }

        public Long getSourceDocId() {
            return sourceDocId;
        }

        public List<Long> getSourceItemIds() {
            return sourceItemIds;
        }

        public String getUserRemark() {
            return userRemark;
        }

        public boolean hasSource() {
            return sourceDocType != null && !sourceDocType.isBlank() && sourceDocId != null;
        }
    }
}
