package net.superkat.wavify.wave;

import java.util.Collection;
import java.util.PriorityQueue;
import java.util.Random;
import net.superkat.wavify.scan.ShoreScanner;

/** Reserve complete surf sets, so river spawns cannot consume the following crests' slots. */
public final class ShoreWaveSchedule {
    public static final class Crest implements Comparable<Crest> {
        public final ShoreScanner.Front front;
        public final long birthTick;
        private final long order;

        private Crest(ShoreScanner.Front front, long birthTick, long order) {
            this.front = front;
            this.birthTick = birthTick;
            this.order = order;
        }

        @Override public int compareTo(Crest other) {
            int time = Long.compare(birthTick, other.birthTick);
            return time == 0 ? Long.compare(order, other.order) : time;
        }
    }

    private final PriorityQueue<Crest> crests = new PriorityQueue<Crest>();
    private long order;

    /** One coherent choice for the coastline: usually one crest, occasionally a small set. */
    public static int chooseCount(Random random, int maximum) {
        if (maximum <= 1) return 1;
        int roll = random.nextInt(100);
        if (roll < 70) return 1;
        if (roll < 95 || maximum == 2) return 2;
        return 3 + random.nextInt(maximum - 2);
    }

    public void addSets(Collection<ShoreScanner.Front> fronts, long birthTick, int count, int gap, int availableSlots) {
        if (count < 1 || gap < 1) return;
        int frontBudget = Math.max(0, availableSlots - crests.size()) / count;
        for (ShoreScanner.Front front : fronts) {
            if (frontBudget-- <= 0) break;
            for (int i = 0; i < count; i++) crests.add(new Crest(front, birthTick + (long)i * gap, order++));
        }
    }

    public Crest poll(long tick) {
        return crests.isEmpty() || crests.peek().birthTick > tick ? null : crests.remove();
    }

    public int size() { return crests.size(); }
    public void clear() { crests.clear(); order = 0; }
}
