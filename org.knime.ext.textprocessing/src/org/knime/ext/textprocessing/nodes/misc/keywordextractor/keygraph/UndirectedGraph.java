/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 22, 2008 (Pierre-Francois Laquerre): created
 */
package org.knime.ext.textprocessing.nodes.misc.keywordextractor.keygraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import org.knime.ext.textprocessing.util.UnorderedPair;

/**
 * Simple undirected graph class for Keygraph.
 *
 * @author Pierre-Francois Laquerre, University of Konstanz
 * @param <NodeType> the type of objects used for nodes
 * @param <WeightType> the type of objects used for the edge weights
 */
public class UndirectedGraph<NodeType, WeightType> {

    private Set<Edge> m_edges;

    // Which edges are incident to which node?
    private Map<NodeType, Set<Edge>> m_incident;

    private Set<NodeType> m_nodes;

    /**
     * Default constructor.
     */
    public UndirectedGraph() {
        m_edges = new LinkedHashSet<Edge>();
        m_incident = new LinkedHashMap<NodeType, Set<Edge>>();
        m_nodes = new LinkedHashSet<NodeType>();
    }

    /**
     * Adds a a node to the graph.
     *
     * @param n the node to add
     */
    public void addNode(final NodeType n) {
        m_nodes.add(n);
        m_incident.put(n, new LinkedHashSet<Edge>());
    }

    /**
     * Adds an edge to the graph. Both nodes must already be in the graph.
     *
     * @param e the edge to add
     */
    private void addEdge(final Edge e) {
        NodeType first = e.getFirst();
        NodeType second = e.getSecond();

        if (!(m_nodes.contains(first) && m_nodes.contains(second))) {
            throw new IllegalArgumentException("Both nodes must be present in"
                    + " the graph before connecting them");
        }

        m_edges.add(e);
        m_incident.get(first).add(e);
        m_incident.get(second).add(e);
    }

    /**
     * Removes an edge from the graph.
     *
     * @param e the edge to remove
     */
    private void removeEdge(final Edge e) {
        m_edges.remove(e);
        m_incident.get(e.getFirst()).remove(e);
        m_incident.get(e.getSecond()).remove(e);
    }

    /**
     * Prunes all edges e from n1 to n2 if no path connects n1 and n2 in G - e.
     *
     * @return the number of pruned edges
     */
    public int pruneWeakEdges() {
        int oldSize = m_edges.size();

        // Must copy the set as we will modify the set of edges in the loop
        for (Edge e : new HashSet<Edge>(m_edges)) {
            NodeType n1 = e.getFirst();
            NodeType n2 = e.getSecond();

            // Temporarily prune e
            removeEdge(e);

            // Only put it back if a path from n1 to n2 exists in G - e
            if (pathExists(n1, n2)) {
                addEdge(e);
            }
        }

        return oldSize - m_edges.size();
    }

    /**
     * @param n1 the first node
     * @param n2 the second node
     * @return true if a path exists between the two nodes
     */
    private boolean pathExists(final NodeType n1, final NodeType n2) {
        Stack<NodeType> unvisited = new Stack<NodeType>();
        Set<NodeType> visited = new HashSet<NodeType>();

        unvisited.addAll(getNeighbours(n1));
        visited.add(n1);

        while (!unvisited.isEmpty()) {
            NodeType n = unvisited.pop();

            if (!visited.contains(n)) {
                if (n.equals(n2)) {
                    return true;
                }
                visited.add(n);
                Set<NodeType> neighbours = getNeighbours(n);
                neighbours.removeAll(visited);
                unvisited.addAll(neighbours);
            }
        }

        return false;
    }

    private class Edge extends UnorderedPair<NodeType> {

        /**
         * @param first the first node
         * @param second the second node
         * @param weight the weight of the edge
         */
        public Edge(final NodeType first, final NodeType second,
                final WeightType weight) {
            super(first, second);
        }

    }

    /**
     * @param node the node whose neighbours we want
     * @return all neighbouring nodes of the node
     */
    public Set<NodeType> getNeighbours(final NodeType node) {
        if (!m_nodes.contains(node)) {
            throw new IllegalArgumentException("The node must be present in "
                    + "the graph");
        }

        Set<NodeType> neighbours = new HashSet<NodeType>();

        for (Edge e : m_incident.get(node)) {
            neighbours.add(e.getOther(node));
        }

        return neighbours;
    }

    /**
     * @return the graph's nodes
     */
    public Set<NodeType> getNodes() {
        return Collections.unmodifiableSet(m_nodes);
    }

    /**
     * Adds an edge from n1 to n2 to the graph.
     *
     * @param n1 the first node
     * @param n2 the second node
     * @param weight the weight of the edge
     */
    public void addEdge(final NodeType n1, final NodeType n2,
            final WeightType weight) {
        if (!(m_nodes.contains(n1) && m_nodes.contains(n2))) {
            throw new IllegalArgumentException("Both nodes must be in the graph");
        }

        addEdge(new Edge(n1, n2, weight));
    }

    /**
     * @return the set of connected subgraphs in this graph
     */
    public Set<Set<NodeType>> getConnectedSubgraphs() {
        Set<Set<NodeType>> subgraphs = new HashSet<Set<NodeType>>();

        Set<NodeType> unclassified = new HashSet<NodeType>(m_nodes);
        while (!unclassified.isEmpty()) {
            NodeType root = pop(unclassified);
            Set<NodeType> subgraph = getConnectedSubgraph(root);
            unclassified.removeAll(subgraph);
            subgraphs.add(subgraph);
        }

        return subgraphs;
    }

    /**
     * @param root the starting node
     * @return the set of nodes in the connected subgraph that contains 'root'
     */
    private Set<NodeType> getConnectedSubgraph(final NodeType root) {
        if (!m_nodes.contains(root)) {
            throw new IllegalArgumentException("The node must be present in "
                    + "the graph");
        }

        Set<NodeType> subgraph = new HashSet<NodeType>();
        subgraph.add(root);

        extendSubgraph(root, subgraph);

        return subgraph;
    }

    /**
     * Extends a subgraph with the neighbours of a given root node and recurses.
     *
     * @param root the root whose neighbours will be added
     * @param subgraph the subgraph to extend
     */
    private void extendSubgraph(final NodeType root,
            final Set<NodeType> subgraph) {
        if (!m_nodes.contains(root)) {
            throw new IllegalArgumentException("The node must be present in "
                    + "the graph");
        }

        for (NodeType neighbour : getNeighbours(root)) {
            if (!subgraph.contains(neighbour)) {
                subgraph.add(neighbour);
                extendSubgraph(neighbour, subgraph);
            }
        }
    }

    /**
     * @param set the set from which to pop an element
     * @return the element that was removed from the set
     */
    private static<T> T pop(final Set<T> set) throws NoSuchElementException {
        Iterator<T> it = set.iterator();
        T e = it.next();
        it.remove();
        return e;
    }
}
