import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskGraph implements Serializable {
    private Map<String, List<String>> graph = new HashMap<>();
    private List<List<String>> topologicalOrder;
    private String rootTarget;
    private static final ConcurrentHashMap<String, Set<String>> assocTargetAllDependencies = new ConcurrentHashMap<>();

    public TaskGraph(Map<String, List<String>> targets, String rootTarget, boolean localMod) {
        this.rootTarget = rootTarget;
        buildGraph(targets);
        if (!localMod && topologicalOrder != null) {
            populateAllDependencies(targets);
        }
    }

    public List<List<String>> getTopologicalOrder() {
        return topologicalOrder;
    }

    public Map<String, Set<String>> getDependenciesTree() {
        return new HashMap<>(assocTargetAllDependencies);
    }

    // Build the dependency graph, where dependencies point to dependents.
    private void buildGraph(Map<String, List<String>> targets) {
        Map<String, Integer> inDegree = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(rootTarget);

        while (!queue.isEmpty()) {
            String currentTarget = queue.poll();
            if (visited.contains(currentTarget)) continue;
            visited.add(currentTarget);

            graph.putIfAbsent(currentTarget, new ArrayList<>());
            inDegree.putIfAbsent(currentTarget, 0);

            List<String> dependencies = targets.getOrDefault(currentTarget, new ArrayList<>());
            for (String dependency : dependencies) {
                graph.putIfAbsent(dependency, new ArrayList<>());
                inDegree.putIfAbsent(dependency, 0);

                graph.get(dependency).add(currentTarget);
                inDegree.put(currentTarget, inDegree.get(currentTarget) + 1);

                queue.add(dependency);
            }
        }
        topologicalSort(inDegree);
    }

    // Topological sorting using Kahn's Algorithm
    public void topologicalSort(Map<String, Integer> inDegree) {
        Queue<String> queue = new LinkedList<>();
        List<List<String>> sortedOrder = new ArrayList<>();

        for (String target : inDegree.keySet()) {
            if (inDegree.get(target) == 0) {
                queue.add(target);
            }
        }

        int graphSize = graph.size();
        do {
            Queue<String> tempQueue = new LinkedList<>();
            List<String> tempSortedOrder = new ArrayList<>();
            graphSize -= queue.size();

            while (!queue.isEmpty()) {
                String current = queue.poll();
                tempSortedOrder.add(current);

                for (String neighbor : graph.get(current)) {
                    inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                    if (inDegree.get(neighbor) == 0) {
                        tempQueue.add(neighbor);
                    }
                }
            }
            queue = tempQueue;
            sortedOrder.add(tempSortedOrder);
        } while (graphSize > 0);

        if (graphSize != 0) {
            topologicalOrder = null; // Cycle detected
        } else {
            topologicalOrder = sortedOrder;
        }
    }

    // Method to populate all dependencies
    private void populateAllDependencies(Map<String, List<String>> targets) {
        for (String targetsWithNoDeps : topologicalOrder.get(0)) {
            assocTargetAllDependencies.put(targetsWithNoDeps, new HashSet<>());
        }
        for (int i = 1; i < topologicalOrder.size(); i++) {
            List<String> currentLevel = topologicalOrder.get(i);
            currentLevel.parallelStream().forEach(target -> {
                List<String> directDeps = targets.getOrDefault(target, new ArrayList<>());
                Set<String> allDependencies = new HashSet<>();
                for (String directDep : directDeps) {
                    allDependencies.add(directDep);
                    allDependencies.addAll(assocTargetAllDependencies.get(directDep));
                }
                assocTargetAllDependencies.put(target, allDependencies);
            });
        }
    }

    public void printGraph() {
        final String RESET = "\u001B[0m";
        final String BLUE = "\u001B[34m";
        final String GREEN = "\u001B[32m";
        final String YELLOW = "\u001B[33m";
        final String CYAN = "\u001B[36m";
        final String BOLD = "\u001B[1m";
    
        // Print the title with a separator
        System.out.println(BLUE + BOLD + "=== Task Graph (Dependencies) ===" + RESET);
        System.out.println();
    
        // Iterate through the graph
        for (String node : graph.keySet()) {
            // Start with the target node
            System.out.print(CYAN + "[Task] " + RESET + GREEN + node + RESET);
    
            // Print dependencies in a visually distinct way
            List<String> dependencies = graph.get(node);
            if (dependencies.isEmpty()) {
                System.out.println(" " + YELLOW + "-> No dependencies" + RESET);
            } else {
                // Add a clear separator for dependencies
                System.out.print(" " + YELLOW + "->" + RESET + " ");
                String depStr = String.join(", ", dependencies);
                System.out.println(CYAN + "[" + depStr + "]" + RESET);
            }
    
            // Add a separator between each task for better readability
            System.out.println("------------------------------------------------------");
        }
    
        // End with a footer
        System.out.println(BLUE + BOLD + "=== End of Task Graph ===" + RESET);
        System.out.println("");
    }

    public void printExecutionOrder() {
        final String RESET = "\u001B[0m";
        final String BLUE = "\u001B[34m";
        final String GREEN = "\u001B[32m";
        final String CYAN = "\u001B[36m";
        final String BOLD = "\u001B[1m";
    
        // Print the title with a separator
        System.out.println(BLUE + BOLD + "=== Execution Order ===" + RESET);
        System.out.println();
        // Iterate through each level of the execution order
        for (int i = 0; i < topologicalOrder.size(); i++) {
            // Print the level title
            System.out.println(GREEN + "Level " + (i + 1) + " ##############################" + RESET);
            
            // Iterate through each task in the current level
            for (String target : topologicalOrder.get(i)) {
                System.out.println("\t" + CYAN + "[Task] " + RESET + GREEN + target + RESET);
                System.out.println("\t------------------------------------------------------");
            }
        }    
        // End with a footer
        System.out.println(BLUE + BOLD + "=== End of Execution Order ===" + RESET);
        System.out.println("");
    }

    public void printTree() {
        final String RESET = "\u001B[0m";
        final String BLUE = "\u001B[34m";
        final String GREEN = "\u001B[32m";
        final String CYAN = "\u001B[36m";
        final String BOLD = "\u001B[1m";
    
        // Print the title with a separator
        System.out.println(BLUE + BOLD + "=== Dependency Tree ===" + RESET);
        System.out.println();
    
        // Iterate through each target and its dependencies
        for (Map.Entry<String, Set<String>> entry : assocTargetAllDependencies.entrySet()) {
            String target = entry.getKey();
            Set<String> dependencies = entry.getValue();
    
            // Print the target name
            System.out.println(GREEN + "[Target] " + RESET + BOLD + target + RESET);
            System.out.println(GREEN + "Dependencies: " + RESET);
    
            // Print each dependency for the current target
            for (String dependency : dependencies) {
                System.out.println("\t" + CYAN + "- " + dependency + RESET);
            }
    
            System.out.println("\t------------------------------------------------------");
        }
    
        // End with a footer
        System.out.println(BLUE + BOLD + "=== End of Dependency Tree ===" + RESET);
        System.out.println("");
    }
    
}
