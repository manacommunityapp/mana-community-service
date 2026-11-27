package com.manacommunity.api.dsa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IntervalMergingService {

    public List<int[]> findAvailableSlots(List<int[]> bookings, List<int[]> blackouts,
                                           int dayStartMinute, int dayEndMinute, int minDurationMinutes) {
        List<int[]> allBlocked = new ArrayList<>();
        allBlocked.addAll(bookings);
        allBlocked.addAll(blackouts);

        List<int[]> merged = mergeIntervals(allBlocked);

        List<int[]> available = new ArrayList<>();
        int cursor = dayStartMinute;

        for (int[] blocked : merged) {
            if (blocked[0] > cursor) {
                int gapDuration = blocked[0] - cursor;
                if (gapDuration >= minDurationMinutes) {
                    available.add(new int[]{cursor, blocked[0]});
                }
            }
            cursor = Math.max(cursor, blocked[1]);
        }

        if (dayEndMinute > cursor) {
            int gapDuration = dayEndMinute - cursor;
            if (gapDuration >= minDurationMinutes) {
                available.add(new int[]{cursor, dayEndMinute});
            }
        }

        return available;
    }

    public List<int[]> mergeIntervals(List<int[]> intervals) {
        if (intervals.isEmpty()) return List.of();

        List<int[]> sorted = intervals.stream()
                .sorted(Comparator.comparingInt(a -> a[0]))
                .collect(Collectors.toList());

        List<int[]> merged = new ArrayList<>();
        int[] current = sorted.get(0).clone();

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i)[0] <= current[1]) {
                current[1] = Math.max(current[1], sorted.get(i)[1]);
            } else {
                merged.add(current);
                current = sorted.get(i).clone();
            }
        }
        merged.add(current);
        return merged;
    }

    public List<int[]> findGuardShiftGaps(List<int[]> shifts, int dayStartMinute, int dayEndMinute) {
        List<int[]> merged = mergeIntervals(shifts);
        List<int[]> gaps = new ArrayList<>();
        int cursor = dayStartMinute;

        for (int[] shift : merged) {
            if (shift[0] > cursor) {
                gaps.add(new int[]{cursor, shift[0]});
            }
            cursor = Math.max(cursor, shift[1]);
        }
        if (dayEndMinute > cursor) {
            gaps.add(new int[]{cursor, dayEndMinute});
        }
        return gaps;
    }

    public record SlotResult(List<int[]> availableSlots, List<int[]> mergedBlocked, int totalAvailableMinutes) {}

    public SlotResult computeFullResult(List<int[]> bookings, List<int[]> blackouts,
                                          int dayStart, int dayEnd, int minDuration) {
        List<int[]> allBlocked = new ArrayList<>(bookings);
        allBlocked.addAll(blackouts);
        List<int[]> merged = mergeIntervals(allBlocked);
        List<int[]> available = findAvailableSlots(bookings, blackouts, dayStart, dayEnd, minDuration);
        int total = available.stream().mapToInt(s -> s[1] - s[0]).sum();
        return new SlotResult(available, merged, total);
    }
}
