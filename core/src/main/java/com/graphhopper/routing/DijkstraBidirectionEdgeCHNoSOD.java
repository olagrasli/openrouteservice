/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing;

import com.graphhopper.routing.ch.CHEntry;
import com.graphhopper.routing.ch.Path4CH;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GHUtility;

public class DijkstraBidirectionEdgeCHNoSOD extends GenericDijkstraBidirection<CHEntry> {
    private int from;
    private int to;

    public DijkstraBidirectionEdgeCHNoSOD(Graph graph, Weighting weighting, TraversalMode traversalMode) {
        super(graph, weighting, traversalMode);
        if (traversalMode != TraversalMode.EDGE_BASED_2DIR) {
            // todo: properly test/implement u-turn mode and allow it here
            throw new IllegalArgumentException(String.format("Traversal mode '%s' not supported by this algorithm, " +
                    "for node based traversal use DijkstraBidirectionCH instead", traversalMode));
        }
    }

    @Override
    public void initFrom(int from, double weight) {
        this.from = from;
        currFrom = createStartEntry(from, weight);
        pqOpenSetFrom.add(currFrom);
        if (currTo != null && currTo.adjNode == from) {
            // special case of identical start and end
            setFinished();
            return;
        }
        EdgeFilter filter = additionalEdgeFilter;
        setEdgeFilter(EdgeFilter.ALL_EDGES);
        fillEdgesFrom();
        setEdgeFilter(filter);

    }

    @Override
    public void initTo(int to, double weight) {
        this.to = to;
        currTo = createStartEntry(to, weight);
        pqOpenSetTo.add(currTo);
        if (currFrom != null && currFrom.adjNode == to) {
            // special case of identical start and end
            setFinished();
            return;
        }
        EdgeFilter filter = additionalEdgeFilter;
        setEdgeFilter(EdgeFilter.ALL_EDGES);
        fillEdgesTo();
        setEdgeFilter(filter);
    }

    private void setFinished() {
        bestPath.sptEntry = currFrom;
        bestPath.edgeTo = currTo;
        bestPath.setWeight(0);
        finishedFrom = true;
        finishedTo = true;
    }

    @Override
    public Path calcPath(int from, int to) {
        this.from = from;
        this.to = to;
        return super.calcPath(from, to);
    }

    @Override
    protected void initCollections(int size) {
        super.initCollections(Math.min(size, 2000));
    }

    @Override
    public boolean finished() {
        // we need to finish BOTH searches for CH!
        if (finishedFrom && finishedTo)
            return true;

        // changed also the final finish condition for CH
        return currFrom.weight >= bestPath.getWeight() && currTo.weight >= bestPath.getWeight();
    }

    @Override
    protected void updateBestPath(EdgeIteratorState edgeState, SPTEntry entryCurrent, int traversalId) {
        boolean reverse = bestWeightMapFrom == bestWeightMapOther;
        // special case where the fwd/bwd search runs directly into the opposite node, for example if the highest level
        // node of the shortest path matches the source or target. in this case one of the searches does not contribute
        // anything to the shortest path.
        int oppositeNode = reverse ? from : to;
        if (edgeState.getAdjNode() == oppositeNode) {
            if (entryCurrent.weight < bestPath.getWeight()) {
                bestPath.setSwitchToFrom(reverse);
                bestPath.setSPTEntry(entryCurrent);
                bestPath.setWeight(entryCurrent.weight);
                bestPath.setSPTEntryTo(new SPTEntry(EdgeIterator.NO_EDGE, oppositeNode, 0));
                return;
            }
        }
        // we can not use in/outEdgeExplorer here because this method will be called within a loop using the 
        // in/outEdgeExplorer already
        EdgeExplorer edgeExplorer = graph.createEdgeExplorer(new DefaultEdgeFilter(flagEncoder, reverse, !reverse));
        EdgeIterator iter = edgeExplorer.setBaseNode(edgeState.getAdjNode());
        while (iter.next()) {
            final int edgeId = getOrigEdgeId(iter, !reverse);
            final int prevOrNextOrigEdgeId = getOrigEdgeId(edgeState, reverse);
            if (!traversalMode.hasUTurnSupport() && edgeId == prevOrNextOrigEdgeId) {
                continue;
            }
            // we need the traversal id of the incoming edge, this is different to what traversalMode.createTraversalId
            // would calculate. todo: can we get rid of 'knowing' how the traversal ids are calculated here ?
            EdgeIteratorState edgeIteratorState = graph.getEdgeIteratorState(edgeId, iter.getBaseNode());
            int key = GHUtility.createEdgeKey(edgeIteratorState.getBaseNode(), edgeIteratorState.getAdjNode(), edgeId, !reverse);
            SPTEntry entryOther = bestWeightMapOther.get(key);
            if (entryOther == null) {
                continue;
            }

            // passing NO_EDGE yields the pure edge cost that needs to be subtracted to obtain the pure turn costs
            double turnCostsAtBridgeNode = weighting.calcWeight(iter, reverse, prevOrNextOrigEdgeId)
                    - weighting.calcWeight(iter, reverse, EdgeIterator.NO_EDGE);
            double newWeight = entryCurrent.weight + entryOther.weight + turnCostsAtBridgeNode;
            if (newWeight < bestPath.getWeight()) {
                bestPath.setSwitchToFrom(reverse);
                bestPath.setSPTEntry(entryCurrent);
                bestPath.setWeight(newWeight);
                bestPath.setSPTEntryTo(entryOther);
            }
        }
    }

    @Override
    protected Path createAndInitPath() {
        bestPath = new Path4CH(graph, graph.getBaseGraph(), weighting);
        return bestPath;
    }

    @Override
    protected CHEntry createStartEntry(int node, double weight) {
        return new CHEntry(node, weight);
    }

    @Override
    protected CHEntry createEntry(EdgeIteratorState edge, int edgeId, double weight, CHEntry parent) {
        CHEntry entry = new CHEntry(edge.getEdge(), edgeId, edge.getAdjNode(), weight);
        entry.parent = parent;
        return entry;
    }

    @Override
    protected int getOrigEdgeId(EdgeIteratorState edge, boolean reverse) {
        return reverse ? edge.getFirstOrigEdge() : edge.getLastOrigEdge();
    }

    @Override
    protected int getTraversalId(EdgeIteratorState edge, int origEdgeId, boolean reverse) {
        EdgeIteratorState iterState = graph.getEdgeIteratorState(origEdgeId, edge.getAdjNode());
        return traversalMode.createTraversalId(iterState, reverse);
    }

    @Override
    protected void updateEntry(CHEntry entry, EdgeIteratorState edge, int edgeId, double weight, CHEntry parent) {
        entry.edge = edge.getEdge();
        entry.incEdge = edgeId;
        entry.weight = weight;
        entry.parent = parent;
    }

    @Override
    protected double calcWeight(EdgeIteratorState edge, CHEntry currEdge, boolean reverse) {
        return weighting.calcWeight(edge, reverse, currEdge.incEdge) + currEdge.weight;
    }

    @Override
    protected boolean accept(EdgeIteratorState edge, CHEntry currEdge, boolean reverse) {
        int edgeId = getOrigEdgeId(edge, !reverse);
        // todo: is it really enough to compare edge ids here ? what if there are two different edges between the 
        // same nodes ?
        if (!traversalMode.hasUTurnSupport() && edgeId == currEdge.incEdge)
            return false;

        return additionalEdgeFilter == null || additionalEdgeFilter.accept(edge);
    }


    @Override
    public String getName() {
        return "dijkstrabi|ch|edge_based|no_sod";
    }

    @Override
    public String toString() {
        return getName() + "|" + weighting;
    }

}
