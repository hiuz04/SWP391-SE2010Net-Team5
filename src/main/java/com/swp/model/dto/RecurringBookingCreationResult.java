package com.swp.model.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecurringBookingCreationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Long> bookingIds;
    private final List<SkippedBookingSlot> skippedSlots;
    private final int totalExpectedSlots;

    public RecurringBookingCreationResult(
            List<Long> bookingIds,
            List<SkippedBookingSlot> skippedSlots,
            int totalExpectedSlots
    ) {
        this.bookingIds = bookingIds == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(bookingIds));
        this.skippedSlots = skippedSlots == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(skippedSlots));
        this.totalExpectedSlots = totalExpectedSlots;
    }

    public List<Long> getBookingIds() {
        return bookingIds;
    }

    public List<SkippedBookingSlot> getSkippedSlots() {
        return skippedSlots;
    }

    public int getTotalExpectedSlots() {
        return totalExpectedSlots;
    }

    public int getCreatedCount() {
        return bookingIds.size();
    }

    public int getSkippedCount() {
        return skippedSlots.size();
    }

    public boolean hasCreatedBookings() {
        return !bookingIds.isEmpty();
    }
}
