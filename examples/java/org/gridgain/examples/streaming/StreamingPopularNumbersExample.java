/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.streaming;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.product.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.streamer.index.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static org.gridgain.grid.product.GridProductEdition.*;

/**
 * Real time streaming popular numbers counter. This example receives a constant stream of
 * random numbers. The gaussian distribution is chosen to make sure that numbers closer
 * to 0 have higher probability. This example will find {@link #POPULAR_NUMBERS_CNT} number
 * of popular numbers over last N number of numbers, where N is specified as streamer
 * window size in {@code examples/config/example-streamer.xml} configuration file and
 * is set to {@code 10,000}.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ggstart.{sh|bat} examples/config/example-streaming.xml'}.
 * <p>
 * Alternatively you can run {@link StreamingNodeStartup} in another JVM which will start GridGain node
 * with {@code examples/config/example-streaming.xml} configuration.
 */
@GridOnlyAvailableIn(STREAMING)
public class StreamingPopularNumbersExample {
    /** Count of most popular numbers to retrieve from grid. */
    private static final int POPULAR_NUMBERS_CNT = 10;

    /** Random number generator. */
    private static final Random RAND = new Random();

    /** Count of total numbers to generate. */
    private static final int CNT = 10_000_000;

    /** Comparator sorting random number entries by number popularity. */
    private static final Comparator<GridStreamerIndexEntry<Integer, Integer, Long>> cmp =
        new Comparator<GridStreamerIndexEntry<Integer, Integer, Long>>() {
            @Override public int compare(GridStreamerIndexEntry<Integer, Integer, Long> e1,
                GridStreamerIndexEntry<Integer, Integer, Long> e2) {
                return e2.value().compareTo(e1.value());
            }
        };

    /** Reducer selecting first POPULAR_NUMBERS_CNT values. */
    private static class PupularNumbersReducer implements GridReducer<Collection<GridStreamerIndexEntry<Integer, Integer, Long>>,
        Collection<GridStreamerIndexEntry<Integer, Integer, Long>>> {
        /** */
        private List<GridStreamerIndexEntry<Integer, Integer, Long>> sorted = new ArrayList<>();

        /** {@inheritDoc} */
        @Override public boolean collect(@Nullable Collection<GridStreamerIndexEntry<Integer, Integer, Long>> col) {
            if (col != null && !col.isEmpty())
                // Add result from remote node to sorted set.
                sorted.addAll(col);

            return true;
        }

        /** {@inheritDoc} */
        @Override public Collection<GridStreamerIndexEntry<Integer, Integer, Long>> reduce() {
            Collections.sort(sorted, cmp);

            return sorted.subList(0, POPULAR_NUMBERS_CNT < sorted.size() ? POPULAR_NUMBERS_CNT : sorted.size());
        }
    }

    /**
     * Executes example.
     *
     * @param args Command line arguments, none required.
     * @throws GridException If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        Timer popularNumbersQryTimer = new Timer("numbers-query-worker");

        // Start grid.
        final Grid g = GridGain.start("examples/config/example-streamer.xml");

        System.out.println();
        System.out.println(">>> Streaming popular numbers example started.");

        try {
            // Schedule query to find most popular words to run every 3 seconds.
            TimerTask task = scheduleQuery(g, popularNumbersQryTimer);

            streamData(g);

            // Force one more run to get final counts.
            task.run();

            popularNumbersQryTimer.cancel();

            // Reset all streamers on all nodes to make sure that
            // consecutive executions start from scratch.
            g.compute().run(new Runnable() {
                @Override public void run() {
                    GridStreamer streamer = g.streamer("popular-numbers");

                    if (streamer == null)
                        System.err.println("Default streamer not found (is example-streamer.xml " +
                            "configuration used on all nodes?)");
                    else {
                        System.out.println("Clearing number counters from streamer.");

                        streamer.reset();
                    }
                }
            }).get();
        }
        finally {
            GridGain.stop(true);
        }
    }

    /**
     * Streams random numbers into the system.
     *
     * @param g Grid.
     * @throws GridException If failed.
     */
    private static void streamData(final Grid g) throws GridException {
        final GridStreamer streamer = g.streamer("popular-numbers");

        // Use gaussian distribution to ensure that
        // numbers closer to 0 have higher probability.
        for (int i = 0; i < CNT; i++)
            streamer.addEvent(((Double)(RAND.nextGaussian() * 10)).intValue());
    }

    /**
     * Schedules our popular numbers query to run every 3 seconds.
     *
     * @param g Grid.
     * @param timer Timer.
     * @return Scheduled task.
     */
    private static TimerTask scheduleQuery(final Grid g, Timer timer) {
        TimerTask task = new TimerTask() {
            @Override public void run() {
                final GridStreamer streamer = g.streamer("popular-numbers");

                try {
                    // Send reduce query to all 'popular-numbers' streamers
                    // running on local and remote noes.
                    Collection<GridStreamerIndexEntry<Integer, Integer, Long>> col = streamer.context().reduce(
                        // This closure will execute on remote nodes.
                        new GridClosure<GridStreamerContext,
                            Collection<GridStreamerIndexEntry<Integer, Integer, Long>>>() {
                            @Override public Collection<GridStreamerIndexEntry<Integer, Integer, Long>> apply(
                                GridStreamerContext ctx) {
                                GridStreamerIndex<Integer, Integer, Long> view = ctx.<Integer>window().index();

                                return view.entries(-1 * POPULAR_NUMBERS_CNT);
                            }
                        },
                        // The reducer will always execute locally, on the same node
                        // that submitted the query.
                        new PupularNumbersReducer());

                    for (GridStreamerIndexEntry<Integer, Integer, Long> cntr : col)
                        System.out.printf("%3d=%d\n", cntr.key(), cntr.value());

                    System.out.println("----------------");
                }
                catch (GridException e) {
                    e.printStackTrace();
                }
            }
        };

        timer.schedule(task, 3000, 3000);

        return task;
    }

    /**
     * Sample streamer stage to compute average.
     */
    @SuppressWarnings("PublicInnerClass")
    public static class StreamerStage implements GridStreamerStage<Integer> {
        /** {@inheritDoc} */
        @Override public String name() {
            return "exampleStage";
        }

        /** {@inheritDoc} */
        @Nullable @Override public Map<String, Collection<?>> run(GridStreamerContext ctx, Collection<Integer> nums)
            throws GridException {
            GridStreamerWindow<Integer> win = ctx.window();

            // Add numbers to window.
            win.enqueueAll(nums);

            // Clear evicted numbers.
            win.clearEvicted();

            // Null means that there are no more stages
            // and that stage pipeline is completed.
            return null;
        }
    }

    /**
     * This class will be set as part of window index configuration.
     */
    private static class IndexUpdater implements GridStreamerIndexUpdater<Integer, Integer, Long> {
        /** {@inheritDoc} */
        @Override public Integer indexKey(Integer evt) {
            // We use event as index key, so event and key are the same.
            return evt;
        }

        /** {@inheritDoc} */
        @Nullable @Override public Long onAdded(GridStreamerIndexEntry<Integer, Integer, Long> entry, Integer evt) {
            return entry.value() + 1;
        }

        /** {@inheritDoc} */
        @Nullable @Override public Long onRemoved(GridStreamerIndexEntry<Integer, Integer, Long> entry, Integer evt) {
            return entry.value() - 1 == 0 ? null : entry.value() - 1;
        }

        /** {@inheritDoc} */
        @Override public Long initialValue(Integer evt, Integer key) {
            return 1L;
        }
    }
}
