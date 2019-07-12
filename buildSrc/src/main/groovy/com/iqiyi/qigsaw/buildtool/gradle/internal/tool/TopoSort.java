package com.iqiyi.qigsaw.buildtool.gradle.internal.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Source code from @see <a href="http://www.voidcn.com/article/p-vzhfqloa-bbb.html"/>
 */
public class TopoSort {

    public static class Node {

        public Object val;

        public int pathIn = 0;

        public Node(Object val) {
            this.val = val;
        }
    }

    public static class Graph {

        public Set<Node> vertexSet = new HashSet<>();

        public Map<Node, Set<Node>> adjaNode = new HashMap<>();

        public boolean addNode(Node start, Node end) {
            if (!vertexSet.contains(start)) {
                vertexSet.add(start);
            }
            if (!vertexSet.contains(end)) {
                vertexSet.add(end);
            }
            if (adjaNode.containsKey(start)
                    && adjaNode.get(start).contains(end)) {
                return false;
            }
            if (adjaNode.containsKey(start)) {
                adjaNode.get(start).add(end);
            } else {
                Set<Node> temp = new HashSet<>();
                temp.add(end);
                adjaNode.put(start, temp);
            }
            end.pathIn++;
            return true;
        }
    }

    public static class KahnTopo {

        private List<Node> result;

        private Queue<Node> setOfZeroIndegree;

        private Graph graph;

        public KahnTopo(Graph di) {
            this.graph = di;
            this.result = new ArrayList<>();
            this.setOfZeroIndegree = new LinkedList<>();
            for (Node iterator : this.graph.vertexSet) {
                if (iterator.pathIn == 0) {
                    this.setOfZeroIndegree.add(iterator);
                }
            }
        }

        public void process() {
            while (!setOfZeroIndegree.isEmpty()) {
                Node v = setOfZeroIndegree.poll();
                result.add(v);
                if (this.graph.adjaNode.keySet().isEmpty()) {
                    return;
                }
                for (Node w : this.graph.adjaNode.get(v)) {
                    w.pathIn--;
                    if (0 == w.pathIn) {
                        setOfZeroIndegree.add(w);
                    }
                }
                this.graph.vertexSet.remove(v);
                this.graph.adjaNode.remove(v);
            }

            if (!this.graph.vertexSet.isEmpty()) {
                throw new IllegalArgumentException("Has Cycle!");
            }
        }

        public List<Node> getResult() {
            return result;
        }
    }
}
