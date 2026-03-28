package stima;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class test {
    static vertex max = new vertex(-999999, -999999, -999999);
    static vertex min = new vertex(999999, 999999, 999999);
    static Tree ocTree;
    static int verticesCount = 1;
    static int[] nodes = new int[0];
    static int[] skipNodes = new int[0];

    public static class vertex {
        public double x, y, z;

        public vertex(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    };

    public static class face {
        public vertex a, b, c;

        public face(vertex a, vertex b, vertex c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    };

    static ArrayList<vertex> vertices = new ArrayList<vertex>();
    static ArrayList<face> faces = new ArrayList<face>();

    private static void reset(int depth) {
        nodes = new int[depth + 1];
        skipNodes = new int[depth + 1];
        if (nodes.length > 0) {
            nodes[0] = 1;
        }
    }

    private static int countLeaf(Tree.Node node) {
        if (node == null) {
            return 0;
        }

        if (Tree.is_leaf(node)) {
            return 1;
        }

        int count = 0;
        for (int i = 0; i < 8; i++) {
            if (node.children[i] != null) {
                count += countLeaf(node.children[i]);
            }
        }
        return count;
    }

    public static vertex subtract(vertex a, vertex b) {
        return new vertex(a.x - b.x, a.y - b.y, a.z - b.z);
    };

    public static vertex cross(vertex a, vertex b) {
        return new vertex(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
    };

    public static double dot(vertex a, vertex b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    };

    // fancy Moller Trumbore
    // basically checks if a line cast from a vertex will past a specific face
    // origin + range*direction = v1 + u*edge1 + v*edge2
    // https://en.wikipedia.org/wiki/M%C3%B6ller%E2%80%93Trumbore_intersection_algorithm

    public static boolean raycasting(vertex origin, vertex direction, face face, vertex intersection) {
        double EPSILON = 0.0000001;

        vertex v1 = face.a;
        vertex v2 = face.b;
        vertex v3 = face.c;

        vertex edge1 = subtract(v2, v1);
        vertex edge2 = subtract(v3, v1);
        vertex h = cross(direction, edge2);
        double a = dot(edge1, h);

        if (a > -EPSILON && a < EPSILON)
            return false; // ray is parallel to triangle

        double f = 1.0 / a;
        vertex s = subtract(origin, v1);
        double u = f * dot(s, h);

        if (u < 0.0 || u > 1.0)
            return false;

        vertex q = cross(s, edge1);
        double v = f * dot(direction, q);

        if (v < 0.0 || u + v > 1.0)
            return false;

        double t = f * dot(edge2, q);
        if (t > EPSILON) { // ray intersection
            intersection.x = origin.x + direction.x * t;
            intersection.y = origin.y + direction.y * t;
            intersection.z = origin.z + direction.z * t;
            return true;
        } else
            return false;

    };

    public static class Tree {
        private Node root;

        public Tree(vertex max, vertex min, int max_depth) {
            root = new Node(starting_cube(max, min));
        }

        public class Node {
            private vertex[] surrounding = new vertex[8];
            private Node[] children;

            public Node(vertex[] surr) {
                this.surrounding = surr;
                this.children = new Node[8];
            }
        }

        /* build starting cube */
        public static vertex[] starting_cube(vertex max, vertex min) {
            double xlen = max.x - min.x, ylen = max.y - min.y, zlen = max.z - min.z, actualLen;
            if (xlen >= ylen) {
                if (xlen >= zlen) {
                    actualLen = xlen;
                } else {
                    actualLen = zlen;
                }
            } else {
                if (ylen >= zlen) {
                    actualLen = ylen;
                } else {
                    actualLen = zlen;
                }
            }

            vertex[] temp = new vertex[8];

            temp[0] = new vertex(min.x, min.y, min.z + actualLen);
            temp[1] = new vertex(min.x + actualLen, min.y, min.z + actualLen);
            temp[2] = new vertex(min.x + actualLen, min.y, min.z);
            temp[3] = new vertex(min.x, min.y, min.z);
            temp[4] = new vertex(min.x, min.y + actualLen, min.z + actualLen);
            temp[5] = new vertex(min.x + actualLen, min.y + actualLen, min.z + actualLen);
            temp[6] = new vertex(min.x + actualLen, min.y + actualLen, min.z);
            temp[7] = new vertex(min.x, min.y + actualLen, min.z);

            // System.out.println("actualLen: "+actualLen);
            // for(int i=0;i<8;i++){
            // System.out.println("num"+i+":\n"+"x: "+temp[i].x+"\ny: "+temp[i].y+"\nz:
            // "+temp[i].z+"\n");
            // }

            return temp;
        }

        /* split the nodes/cube into 8 equally sized cubes */
        // public void split_cube(Node root, int depth, int max_depth) {
        //     // System.out.println("splitting cube!");
        //     double currentLen = root.surrounding[5].x - root.surrounding[3].x;
        //     if (depth >= max_depth) {
        //         return;
        //     }

        //     if (depth + 1 < nodes.length) {
        //         nodes[depth + 1] += 8;
        //     }

        //     // 0
        //     vertex tempMax = new vertex(root.surrounding[0].x + currentLen / 2, root.surrounding[0].y + currentLen / 2,
        //             root.surrounding[0].z),
        //             tempMin = new vertex(root.surrounding[0].x, root.surrounding[0].y,
        //                     root.surrounding[0].z - currentLen / 2);
        //     Node temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[0] = temp;

        //     // 1
        //     tempMax = new vertex(root.surrounding[1].x, root.surrounding[1].y + currentLen / 2, root.surrounding[1].z);
        //     tempMin = new vertex(root.surrounding[1].x - currentLen / 2, root.surrounding[1].y,
        //             root.surrounding[1].z - currentLen / 2);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[1] = temp;

        //     // 2
        //     tempMax = new vertex(root.surrounding[2].x, root.surrounding[2].y + currentLen / 2,
        //             root.surrounding[2].z + currentLen / 2);
        //     tempMin = new vertex(root.surrounding[2].x - currentLen / 2, root.surrounding[2].y, root.surrounding[2].z);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[2] = temp;

        //     // 3
        //     tempMax = new vertex(root.surrounding[3].x + currentLen / 2, root.surrounding[3].y + currentLen / 2,
        //             root.surrounding[3].z + currentLen / 2);
        //     tempMin = new vertex(root.surrounding[3].x, root.surrounding[3].y, root.surrounding[3].z);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[3] = temp;

        //     // 4
        //     tempMax = new vertex(root.surrounding[4].x + currentLen / 2, root.surrounding[4].y, root.surrounding[4].z);
        //     tempMin = new vertex(root.surrounding[4].x, root.surrounding[4].y - currentLen / 2,
        //             root.surrounding[4].z - currentLen / 2);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[4] = temp;

        //     // 5
        //     tempMax = new vertex(root.surrounding[5].x, root.surrounding[5].y, root.surrounding[5].z);
        //     tempMin = new vertex(root.surrounding[5].x - currentLen / 2, root.surrounding[5].y - currentLen / 2,
        //             root.surrounding[5].z - currentLen / 2);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[5] = temp;

        //     // 6
        //     tempMax = new vertex(root.surrounding[6].x, root.surrounding[6].y, root.surrounding[6].z + currentLen / 2);
        //     tempMin = new vertex(root.surrounding[6].x - currentLen / 2, root.surrounding[6].y - currentLen / 2,
        //             root.surrounding[6].z);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[6] = temp;

        //     // 7
        //     tempMax = new vertex(root.surrounding[7].x + currentLen / 2, root.surrounding[7].y,
        //             root.surrounding[7].z + currentLen / 2);
        //     tempMin = new vertex(root.surrounding[7].x, root.surrounding[7].y - currentLen / 2, root.surrounding[7].z);
        //     temp = new Node(starting_cube(tempMax, tempMin));
        //     root.children[7] = temp;

        //     split_cube(root.children[0], depth + 1, max_depth);
        //     split_cube(root.children[1], depth + 1, max_depth);
        //     split_cube(root.children[2], depth + 1, max_depth);
        //     split_cube(root.children[3], depth + 1, max_depth);
        //     split_cube(root.children[4], depth + 1, max_depth);
        //     split_cube(root.children[5], depth + 1, max_depth);
        //     split_cube(root.children[6], depth + 1, max_depth);
        //     split_cube(root.children[7], depth + 1, max_depth);

        //     // for(int i=0;i<8;i++){
        //     // System.out.println("num_"+i+":\n"+"x:
        //     // "+root.children[0].surrounding[i].x+"\ny:
        //     // "+root.children[0].surrounding[i].y+"\nz:
        //     // "+root.children[0].surrounding[i].z+"\n");
        //     // }

        // }

        /* check if vertex is in a cube/node */
        public static boolean is_in_cube(Node node, vertex v) {
            return ((v.x >= node.surrounding[3].x && v.x <= node.surrounding[5].x) &&
                    (v.y >= node.surrounding[3].y && v.y <= node.surrounding[5].y) &&
                    (v.z >= node.surrounding[3].z && v.z <= node.surrounding[5].z));
        }

        /* check if node is leaf */
        public static boolean is_leaf(Node node) {
            for (int i = 0; i < 8; i++) {
                if (node.children[i] != null) {
                    return false;
                }
            }
            return true;
        }

        /* Build a branch from node down to max_depth for the given intersection point (lazy building) */
        public void build_branch(Node node, vertex intersect, int depth, int maxDepth) {
            if (depth >= maxDepth || !is_in_cube(node, intersect)) {
                return;
            }

            double len = node.surrounding[5].x - node.surrounding[3].x;

            for (int i=0;i<8;i++) {
                if (node.children[i] == null) {
                    node.children[i] = (new Node(octant_vertex(node, i, len)));
                    if (depth + 1 < nodes.length) {
                        nodes[depth + 1]++;
                    }
                }

                if (is_in_cube(node.children[i], intersect)) {
                    build_branch(node.children[i], intersect, depth + 1, maxDepth);
                    break;
                }
            }
        }

        /* Create a specific child octant */
        public Node octant_node(Node parent, int octant, double len) {
            vertex tempMax, tempMin;

            switch (octant) {
                case 0:
                    tempMax = new vertex(parent.surrounding[0].x + len / 2, parent.surrounding[0].y + len / 2,
                            parent.surrounding[0].z);
                    tempMin = new vertex(parent.surrounding[0].x, parent.surrounding[0].y,
                            parent.surrounding[0].z - len / 2);
                    break;
                case 1: 
                    tempMax = new vertex(parent.surrounding[1].x, parent.surrounding[1].y + len / 2, parent.surrounding[1].z);
                    tempMin = new vertex(parent.surrounding[1].x - len / 2, parent.surrounding[1].y,
                            parent.surrounding[1].z - len / 2);
                    break;
                case 2: 
                    tempMax = new vertex(parent.surrounding[2].x, parent.surrounding[2].y + len / 2,
                            parent.surrounding[2].z + len / 2);
                    tempMin = new vertex(parent.surrounding[2].x - len / 2, parent.surrounding[2].y, parent.surrounding[2].z);
                    break;
                case 3: 
                    tempMax = new vertex(parent.surrounding[3].x + len / 2, parent.surrounding[3].y + len / 2,
                            parent.surrounding[3].z + len / 2);
                    tempMin = new vertex(parent.surrounding[3].x, parent.surrounding[3].y, parent.surrounding[3].z);
                    break;
                case 4: 
                    tempMax = new vertex(parent.surrounding[4].x + len / 2, parent.surrounding[4].y, parent.surrounding[4].z);
                    tempMin = new vertex(parent.surrounding[4].x, parent.surrounding[4].y - len / 2,
                            parent.surrounding[4].z - len / 2);
                    break;
                case 5: 
                    tempMax = new vertex(parent.surrounding[5].x, parent.surrounding[5].y, parent.surrounding[5].z);
                    tempMin = new vertex(parent.surrounding[5].x - len / 2, parent.surrounding[5].y - len / 2,
                            parent.surrounding[5].z - len / 2);
                    break;
                case 6: 
                    tempMax = new vertex(parent.surrounding[6].x, parent.surrounding[6].y, parent.surrounding[6].z + len / 2);
                    tempMin = new vertex(parent.surrounding[6].x - len / 2, parent.surrounding[6].y - len / 2,
                            parent.surrounding[6].z);
                    break;
                case 7: 
                    tempMax = new vertex(parent.surrounding[7].x + len / 2, parent.surrounding[7].y,
                            parent.surrounding[7].z + len / 2);
                    tempMin = new vertex(parent.surrounding[7].x, parent.surrounding[7].y - len / 2, parent.surrounding[7].z);
                    break;
                default:
                    return null;
            }

            return new Node(starting_cube(tempMax, tempMin));
        }

        public vertex[] octant_vertex(Node parent, int octant, double len) {
            vertex tempMax, tempMin;

            switch (octant) {
                case 0:
                    tempMax = new vertex(parent.surrounding[0].x + len / 2, parent.surrounding[0].y + len / 2,
                            parent.surrounding[0].z);
                    tempMin = new vertex(parent.surrounding[0].x, parent.surrounding[0].y,
                            parent.surrounding[0].z - len / 2);
                    break;
                case 1: 
                    tempMax = new vertex(parent.surrounding[1].x, parent.surrounding[1].y + len / 2, parent.surrounding[1].z);
                    tempMin = new vertex(parent.surrounding[1].x - len / 2, parent.surrounding[1].y,
                            parent.surrounding[1].z - len / 2);
                    break;
                case 2: 
                    tempMax = new vertex(parent.surrounding[2].x, parent.surrounding[2].y + len / 2,
                            parent.surrounding[2].z + len / 2);
                    tempMin = new vertex(parent.surrounding[2].x - len / 2, parent.surrounding[2].y, parent.surrounding[2].z);
                    break;
                case 3: 
                    tempMax = new vertex(parent.surrounding[3].x + len / 2, parent.surrounding[3].y + len / 2,
                            parent.surrounding[3].z + len / 2);
                    tempMin = new vertex(parent.surrounding[3].x, parent.surrounding[3].y, parent.surrounding[3].z);
                    break;
                case 4: 
                    tempMax = new vertex(parent.surrounding[4].x + len / 2, parent.surrounding[4].y, parent.surrounding[4].z);
                    tempMin = new vertex(parent.surrounding[4].x, parent.surrounding[4].y - len / 2,
                            parent.surrounding[4].z - len / 2);
                    break;
                case 5: 
                    tempMax = new vertex(parent.surrounding[5].x, parent.surrounding[5].y, parent.surrounding[5].z);
                    tempMin = new vertex(parent.surrounding[5].x - len / 2, parent.surrounding[5].y - len / 2,
                            parent.surrounding[5].z - len / 2);
                    break;
                case 6: 
                    tempMax = new vertex(parent.surrounding[6].x, parent.surrounding[6].y, parent.surrounding[6].z + len / 2);
                    tempMin = new vertex(parent.surrounding[6].x - len / 2, parent.surrounding[6].y - len / 2,
                            parent.surrounding[6].z);
                    break;
                case 7: 
                    tempMax = new vertex(parent.surrounding[7].x + len / 2, parent.surrounding[7].y,
                            parent.surrounding[7].z + len / 2);
                    tempMin = new vertex(parent.surrounding[7].x, parent.surrounding[7].y - len / 2, parent.surrounding[7].z);
                    break;
                default:
                    return null;
            }

            return starting_cube(tempMax, tempMin);
        }

        /* Main function that writes into the destination file */
        public static void build_cubes(String fileDest, Node node, int depth, int max_depth) {
            if (depth == max_depth) {
                StringBuilder temp = new StringBuilder();
                for (int i = 0; i < 8; i++) {
                    temp.append("v " + node.surrounding[i].x + " " + node.surrounding[i].y + " "
                            + node.surrounding[i].z + "\n");
                }
                temp.append("f " + (verticesCount + 3) + " " + (verticesCount + 2) + " " + (verticesCount + 1) + " "
                        + verticesCount + "\n");
                temp.append("f " + verticesCount + " " + (verticesCount + 1) + " " + (verticesCount + 5) + " "
                        + (verticesCount + 4) + "\n");
                temp.append("f " + verticesCount + " " + (verticesCount + 4) + " " + (verticesCount + 7) + " "
                        + (verticesCount + 3) + "\n");
                temp.append("f " + (verticesCount + 1) + " " + (verticesCount + 2) + " " + (verticesCount + 6) + " "
                        + (verticesCount + 5) + "\n");
                temp.append("f " + (verticesCount + 2) + " " + (verticesCount + 3) + " " + (verticesCount + 7) + " "
                        + (verticesCount + 6) + "\n");
                temp.append("f " + (verticesCount + 4) + " " + (verticesCount + 5) + " " + (verticesCount + 6) + " "
                        + (verticesCount + 7) + "\n");

                try (FileWriter writer = new FileWriter(fileDest, true)) {
                    writer.write(temp.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                verticesCount += 8;
            } else if (depth < max_depth) {
                for (int i = 0; i < 8; i++) {
                    if (node.children[i] != null) {
                        build_cubes(fileDest, node.children[i], depth + 1, max_depth);
                    }
                }
            }
        }
    };

    

    /* Just to find max and min of every vertices */
    public static void find_extremes(vertex temp) {
        if (temp.x <= min.x) {
            min.x = temp.x;
        }
        if (temp.y <= min.y) {
            min.y = temp.y;
        }
        if (temp.z <= min.z) {
            min.z = temp.z;
        }
        if (temp.x >= max.x) {
            max.x = temp.x;
        }
        if (temp.y >= max.y) {
            max.y = temp.y;
        }
        if (temp.z >= max.z) {
            max.z = temp.z;
        }
    }

    public static void markActiveVoxel(Tree.Node node, ArrayList<face> candidateFaces, double epsilon,
            double rootMinX, double rootMinY, double rootMinZ, double step, int depth, int maxDepth) {
        if (candidateFaces.isEmpty()) {
            return;
        }

        if (depth >= maxDepth) {
            vertex center = new vertex(
                    (node.surrounding[3].x + node.surrounding[5].x) / 2,
                    (node.surrounding[3].y + node.surrounding[5].y) / 2,
                    (node.surrounding[3].z + node.surrounding[5].z) / 2);

            for (face f : candidateFaces) {
                vertex intersection = new vertex(0, 0, 0);

                vertex origin = new vertex(rootMinX - step, center.y, center.z);
                vertex dir = new vertex(1, 0, 0);
                if (raycasting(origin, dir, f, intersection)) {
                    intersection.x += epsilon;
                    if (Tree.is_in_cube(node, intersection)) {
                        ocTree.build_branch(ocTree.root, intersection, 0, maxDepth);
                        return;
                    }
                }

                origin = new vertex(center.x, rootMinY - step, center.z);
                dir = new vertex(0, 1, 0);
                if (raycasting(origin, dir, f, intersection)) {
                    intersection.y += epsilon;
                    if (Tree.is_in_cube(node, intersection)) {
                        ocTree.build_branch(ocTree.root, intersection, 0, maxDepth);
                        return;
                    }
                }

                origin = new vertex(center.x, center.y, rootMinZ - step);
                dir = new vertex(0, 0, 1);
                if (raycasting(origin, dir, f, intersection)) {
                    intersection.z += epsilon;
                    if (Tree.is_in_cube(node, intersection)) {
                        ocTree.build_branch(ocTree.root, intersection, 0, maxDepth);
                        return;
                    }
                }
            }
            return;
        }

        double currentLen = node.surrounding[5].x - node.surrounding[3].x;
        
        for (int i = 0; i < 8; i++) {
            vertex[] surroundTheChild = ocTree.octant_vertex(node, i, currentLen);
            ArrayList<face> childFaces = new ArrayList<face>();
            for (face f : candidateFaces) {
                double faceMinX = Math.min(f.a.x, Math.min(f.b.x, f.c.x));
                double faceMinY = Math.min(f.a.y, Math.min(f.b.y, f.c.y));
                double faceMinZ = Math.min(f.a.z, Math.min(f.b.z, f.c.z));
                double faceMaxX = Math.max(f.a.x, Math.max(f.b.x, f.c.x));
                double faceMaxY = Math.max(f.a.y, Math.max(f.b.y, f.c.y));
                double faceMaxZ = Math.max(f.a.z, Math.max(f.b.z, f.c.z));

                // cuz we're not creating the whole tree
                double nodeMinX = surroundTheChild[3].x;
                double nodeMinY = surroundTheChild[3].y;
                double nodeMinZ = surroundTheChild[3].z;
                double nodeMaxX = surroundTheChild[5].x;
                double nodeMaxY = surroundTheChild[5].y;
                double nodeMaxZ = surroundTheChild[5].z;

                if (!(faceMaxX < nodeMinX || faceMinX > nodeMaxX ||
                        faceMaxY < nodeMinY || faceMinY > nodeMaxY ||
                        faceMaxZ < nodeMinZ || faceMinZ > nodeMaxZ)) {
                    childFaces.add(f);
                }
            }

            if (!childFaces.isEmpty()) {
                if (node.children[i] == null) {
                    node.children[i] = ocTree.octant_node(node, i, currentLen);
                }
                markActiveVoxel(node.children[i], childFaces, epsilon, rootMinX, rootMinY, rootMinZ,
                        step, depth + 1, maxDepth);
            } else if (depth + 1 < skipNodes.length) {
                skipNodes[depth + 1]++;
            }
        }
    }

    public static String objToVoxeL(File file, int depth, String outputName) throws Exception {
        long startNs = System.nanoTime();

        max = new vertex(-999999, -999999, -999999);
        min = new vertex(999999, 999999, 999999);
        verticesCount = 1;
        vertices.clear();
        faces.clear();
        reset(depth);

        try (Scanner read = new Scanner(file)) {
            while (read.hasNextLine()) {
                String data = read.nextLine().trim();
                if (data.isEmpty() || data.startsWith("#")) {
                    continue;
                }

                String[] splitData = data.split("\\s+");
                if (splitData[0].equals("v")) {
                    vertex temp = new vertex(
                            Double.parseDouble(splitData[1]),
                            Double.parseDouble(splitData[2]),
                            Double.parseDouble(splitData[3]));
                    vertices.add(temp);
                    find_extremes(temp);
                }
            }
        }

        ocTree = new Tree(max, min, depth);

        try (Scanner read = new Scanner(file)) {
            while (read.hasNextLine()) {
                String data = read.nextLine().trim();
                if (data.isEmpty() || data.startsWith("#")) {
                    continue;
                }

                String[] splitData = data.split("\\s+");
                if (splitData[0].equals("f")) {
                    int a = Integer.parseInt(splitData[1].split("/")[0]);

                    // for quads
                    for (int i = 2; i < splitData.length - 1; i++) {
                        int b = Integer.parseInt(splitData[i].split("/")[0]);
                        int c = Integer.parseInt(splitData[i + 1].split("/")[0]);

                        face temp = new face(vertices.get(a - 1), vertices.get(b - 1),
                                vertices.get(c - 1));
                        faces.add(temp);
                    }
                }
            }
        }

        double rootlength = ocTree.root.surrounding[5].x -
                ocTree.root.surrounding[3].x;
        double steps = rootlength / Math.pow(2, depth);
        double epsilon = steps / 1000;

        double minx = ocTree.root.surrounding[3].x, miny =
                ocTree.root.surrounding[3].y, minz = ocTree.root.surrounding[3].z;

        markActiveVoxel(ocTree.root, faces, epsilon, minx, miny, minz, steps, 0, depth);

        String fileDest = outputName;
        String savedPath = "./test/" + fileDest;
        try {
            File newFile = new File(savedPath);
            File parent = newFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            newFile.createNewFile();
            new FileWriter(savedPath).close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        Tree.build_cubes(savedPath, ocTree.root, 0, depth);

        int voxelCount = countLeaf(ocTree.root);
        int generatedVertexCount = voxelCount * 8;
        int generatedFaceCount = voxelCount * 6;
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        StringBuilder output = new StringBuilder();
        output.append("Created Voxel: ").append(voxelCount).append("\n");
        output.append("Created Vertex: ").append(generatedVertexCount).append("\n");
        output.append("Created Faces: ").append(generatedFaceCount).append("\n");
        output.append("Node Stats:\n");
        for (int d = 1; d <= depth; d++) {
            output.append(d).append(" : ").append(nodes[d]).append("\n");
        }
        output.append("Skipped Nodes Stats:\n");
        for (int d = 1; d <= depth; d++) {
            output.append(d).append(" : ").append(skipNodes[d]).append("\n");
        }
        output.append("Depth: ").append(depth).append("\n");
        output.append("Time: ").append(elapsedMs).append(" ms\n");
        output.append(".obj Saved To: ").append(new File(savedPath).getAbsolutePath()).append("\n");

        String summaryText = output.toString();
        System.out.print(summaryText);
        return summaryText;
    }
}
