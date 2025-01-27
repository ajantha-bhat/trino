/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.execution.scheduler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.trino.metadata.Split;
import io.trino.sql.planner.plan.PlanNodeId;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;

class TestingTaskSourceCallback
        implements EventDrivenTaskSource.Callback
{
    private final Map<Integer, NodeRequirements> nodeRequirements = new HashMap<>();
    private final Map<Integer, ListMultimap<PlanNodeId, Split>> splits = new HashMap<>();
    private final SetMultimap<Integer, PlanNodeId> noMoreSplits = HashMultimap.create();
    private final Set<Integer> sealedPartitions = new HashSet<>();
    private boolean noMorePartitions;
    private final SettableFuture<List<TaskDescriptor>> taskDescriptors = SettableFuture.create();

    public ListenableFuture<List<TaskDescriptor>> getTaskDescriptorsFuture()
    {
        return taskDescriptors;
    }

    public List<TaskDescriptor> getTaskDescriptors()
    {
        try {
            return Futures.getDone(taskDescriptors);
        }
        catch (ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    public synchronized int getPartitionCount()
    {
        return nodeRequirements.size();
    }

    public synchronized NodeRequirements getNodeRequirements(int partition)
    {
        NodeRequirements result = nodeRequirements.get(partition);
        checkArgument(result != null, "partition not found: %s", partition);
        return result;
    }

    public synchronized Set<Integer> getSplitIds(int partition, PlanNodeId planNodeId)
    {
        ListMultimap<PlanNodeId, Split> partitionSplits = splits.getOrDefault(partition, ImmutableListMultimap.of());
        return partitionSplits.get(planNodeId).stream()
                .map(split -> (TestingConnectorSplit) split.getConnectorSplit())
                .map(TestingConnectorSplit::getId)
                .collect(toImmutableSet());
    }

    public synchronized boolean isNoMoreSplits(int partition, PlanNodeId planNodeId)
    {
        return noMoreSplits.get(partition).contains(planNodeId);
    }

    public synchronized boolean isSealed(int partition)
    {
        return sealedPartitions.contains(partition);
    }

    public synchronized boolean isNoMorePartitions()
    {
        return noMorePartitions;
    }

    public void checkContainsSplits(PlanNodeId planNodeId, Collection<Split> splits, boolean replicated)
    {
        Set<Integer> expectedSplitIds = splits.stream()
                .map(TestingConnectorSplit::getSplitId)
                .collect(Collectors.toSet());
        for (int partitionId = 0; partitionId < getPartitionCount(); partitionId++) {
            Set<Integer> partitionSplitIds = getSplitIds(partitionId, planNodeId);
            if (replicated) {
                assertThat(partitionSplitIds).containsAll(expectedSplitIds);
            }
            else {
                expectedSplitIds.removeAll(partitionSplitIds);
            }
        }
        if (!replicated) {
            assertThat(expectedSplitIds).isEmpty();
        }
    }

    @Override
    public synchronized void partitionsAdded(List<EventDrivenTaskSource.Partition> partitions)
    {
        verify(!noMorePartitions, "noMorePartitions is set");
        for (EventDrivenTaskSource.Partition partition : partitions) {
            verify(nodeRequirements.put(partition.partitionId(), partition.nodeRequirements()) == null, "partition already exist: %s", partition.partitionId());
        }
    }

    @Override
    public synchronized void noMorePartitions()
    {
        noMorePartitions = true;
        checkFinished();
    }

    @Override
    public synchronized void partitionsUpdated(List<EventDrivenTaskSource.PartitionUpdate> partitionUpdates)
    {
        for (EventDrivenTaskSource.PartitionUpdate partitionUpdate : partitionUpdates) {
            int partitionId = partitionUpdate.partitionId();
            verify(nodeRequirements.get(partitionId) != null, "partition does not exist: %s", partitionId);
            verify(!sealedPartitions.contains(partitionId), "partition is sealed: %s", partitionId);
            PlanNodeId planNodeId = partitionUpdate.planNodeId();
            if (!partitionUpdate.splits().isEmpty()) {
                verify(!noMoreSplits.get(partitionId).contains(planNodeId), "noMoreSplits is set for partition %s and plan node %s", partitionId, planNodeId);
                splits.computeIfAbsent(partitionId, (key) -> ArrayListMultimap.create()).putAll(planNodeId, partitionUpdate.splits());
            }
            if (partitionUpdate.noMoreSplits()) {
                noMoreSplits.put(partitionId, planNodeId);
            }
        }
    }

    @Override
    public synchronized void partitionsSealed(ImmutableIntArray partitionIds)
    {
        partitionIds.forEach(sealedPartitions::add);
        checkFinished();
    }

    private synchronized void checkFinished()
    {
        if (noMorePartitions && sealedPartitions.containsAll(nodeRequirements.keySet())) {
            verify(sealedPartitions.equals(nodeRequirements.keySet()), "unknown sealed partitions: %s", Sets.difference(sealedPartitions, nodeRequirements.keySet()));
            ImmutableList.Builder<TaskDescriptor> result = ImmutableList.builder();
            for (Integer partitionId : sealedPartitions) {
                ListMultimap<PlanNodeId, Split> taskSplits = splits.getOrDefault(partitionId, ImmutableListMultimap.of());
                verify(
                        noMoreSplits.get(partitionId).containsAll(taskSplits.keySet()),
                        "no more split is missing for partition %s: %s",
                        partitionId,
                        Sets.difference(taskSplits.keySet(), noMoreSplits.get(partitionId)));
                result.add(new TaskDescriptor(
                        partitionId,
                        taskSplits,
                        nodeRequirements.get(partitionId)));
            }
            taskDescriptors.set(result.build());
        }
    }

    @Override
    public synchronized void failed(Throwable t)
    {
        taskDescriptors.setException(t);
    }
}
