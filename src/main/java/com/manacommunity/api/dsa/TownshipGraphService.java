package com.manacommunity.api.dsa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class TownshipGraphService {

    public static class Edge {
        public String to;
        public int weight;
        public boolean heavyVehicleAllowed;

        public Edge(String to, int weight, boolean heavyVehicleAllowed) {
            this.to = to;
            this.weight = weight;
            this.heavyVehicleAllowed = heavyVehicleAllowed;
        }
    }

    public PathResult findShortestPath(Map<String, List<Edge>> graph, String source, String destination,
                                         boolean isHeavyVehicle) {
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<String[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> Integer.parseInt(a[1])));

        for (String node : graph.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }
        dist.put(source, 0);
        pq.offer(new String[]{source, "0"});

        while (!pq.isEmpty()) {
            String[] curr = pq.poll();
            String u = curr[0];
            int d = Integer.parseInt(curr[1]);

            if (d > dist.getOrDefault(u, Integer.MAX_VALUE)) continue;
            if (u.equals(destination)) break;

            List<Edge> edges = graph.getOrDefault(u, List.of());
            for (Edge edge : edges) {
                if (isHeavyVehicle && !edge.heavyVehicleAllowed) continue;

                int newDist = d + edge.weight;
                if (newDist < dist.getOrDefault(edge.to, Integer.MAX_VALUE)) {
                    dist.put(edge.to, newDist);
                    prev.put(edge.to, u);
                    pq.offer(new String[]{edge.to, String.valueOf(newDist)});
                }
            }
        }

        if (!dist.containsKey(destination) || dist.get(destination) == Integer.MAX_VALUE) {
            return new PathResult(List.of(), -1, false, "No valid route found");
        }

        List<String> path = new ArrayList<>();
        String node = destination;
        while (node != null) {
            path.add(0, node);
            node = prev.get(node);
        }

        return new PathResult(path, dist.get(destination), true,
                "Route found: " + dist.get(destination) + " min via " + path.size() + " nodes");
    }

    public Map<String, List<Edge>> buildDemoGraph() {
        Map<String, List<Edge>> graph = new HashMap<>();

        addEdge(graph, "Gate-1", "Tower-A", 3, true);
        addEdge(graph, "Gate-1", "Tower-B", 5, true);
        addEdge(graph, "Gate-1", "Park", 4, false);
        addEdge(graph, "Gate-2", "Tower-C", 2, true);
        addEdge(graph, "Gate-2", "Park", 6, true);
        addEdge(graph, "Gate-2", "Tower-D", 7, true);
        addEdge(graph, "Tower-A", "Tower-B", 2, true);
        addEdge(graph, "Tower-A", "Club", 4, false);
        addEdge(graph, "Tower-B", "Tower-C", 3, true);
        addEdge(graph, "Tower-B", "Park", 2, false);
        addEdge(graph, "Tower-C", "Tower-D", 2, true);
        addEdge(graph, "Tower-C", "Club", 5, true);
        addEdge(graph, "Tower-D", "Tower-H", 3, true);
        addEdge(graph, "Park", "Club", 3, false);
        addEdge(graph, "Park", "Tower-D", 4, true);
        addEdge(graph, "Club", "Tower-H", 6, true);
        addEdge(graph, "Tower-D", "Gate-3", 2, true);
        addEdge(graph, "Tower-H", "Gate-3", 4, true);

        return graph;
    }

    private void addEdge(Map<String, List<Edge>> graph, String from, String to, int weight, boolean heavyAllowed) {
        graph.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge(to, weight, heavyAllowed));
        graph.computeIfAbsent(to, k -> new ArrayList<>()).add(new Edge(from, weight, heavyAllowed));
    }

    public record PathResult(List<String> path, int totalWeight, boolean found, String message) {}
}
