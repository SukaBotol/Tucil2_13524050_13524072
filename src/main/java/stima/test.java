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
            root = new Node(starting_cube(max, min), null);
            split_cube(root, 0, max_depth);
        }

        public class Node {
            private vertex[] surrounding = new vertex[8];
            private boolean isActive;
            private Node parent;
            private Node[] children;

            public Node(vertex[] surr, Node parent) {
                this.surrounding = surr;
                this.parent = parent;
                this.isActive = false;
                this.children = new Node[8];
            }
        }

        /* build starting cube */
        public vertex[] starting_cube(vertex max, vertex min) {
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
            vertex num0 = new vertex(min.x, min.y, min.z + actualLen);
            vertex num1 = new vertex(min.x + actualLen, min.y, min.z + actualLen);
            vertex num2 = new vertex(min.x + actualLen, min.y, min.z);
            vertex num3 = new vertex(min.x, min.y, min.z);
            vertex num4 = new vertex(min.x, min.y + actualLen, min.z + actualLen);
            vertex num5 = new vertex(min.x + actualLen, min.y + actualLen, min.z + actualLen);
            vertex num6 = new vertex(min.x + actualLen, min.y + actualLen, min.z);
            vertex num7 = new vertex(min.x, min.y + actualLen, min.z);

            temp[0] = num0;
            temp[1] = num1;
            temp[2] = num2;
            temp[3] = num3;
            temp[4] = num4;
            temp[5] = num5;
            temp[6] = num6;
            temp[7] = num7;

            // System.out.println("actualLen: "+actualLen);
            // for(int i=0;i<8;i++){
            // System.out.println("num"+i+":\n"+"x: "+temp[i].x+"\ny: "+temp[i].y+"\nz:
            // "+temp[i].z+"\n");
            // }

            return temp;
        }

        /* split the nodes/cube into 8 equally sized cubes */
        public void split_cube(Node root, int depth, int max_depth) {
            // System.out.println("splitting cube!");
            double currentLen = root.surrounding[5].x - root.surrounding[3].x;
            if (depth >= max_depth) {
                return;
            }
            // 0
            vertex tempMax = new vertex(root.surrounding[0].x + currentLen / 2, root.surrounding[0].y + currentLen / 2,
                    root.surrounding[0].z),
                    tempMin = new vertex(root.surrounding[0].x, root.surrounding[0].y,
                            root.surrounding[0].z - currentLen / 2);
            Node temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[0] = temp;

            // 1
            tempMax = new vertex(root.surrounding[1].x, root.surrounding[1].y + currentLen / 2, root.surrounding[1].z);
            tempMin = new vertex(root.surrounding[1].x - currentLen / 2, root.surrounding[1].y,
                    root.surrounding[1].z - currentLen / 2);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[1] = temp;

            // 2
            tempMax = new vertex(root.surrounding[2].x, root.surrounding[2].y + currentLen / 2,
                    root.surrounding[2].z + currentLen / 2);
            tempMin = new vertex(root.surrounding[2].x - currentLen / 2, root.surrounding[2].y, root.surrounding[2].z);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[2] = temp;

            // 3
            tempMax = new vertex(root.surrounding[3].x + currentLen / 2, root.surrounding[3].y + currentLen / 2,
                    root.surrounding[3].z + currentLen / 2);
            tempMin = new vertex(root.surrounding[3].x, root.surrounding[3].y, root.surrounding[3].z);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[3] = temp;

            // 4
            tempMax = new vertex(root.surrounding[4].x + currentLen / 2, root.surrounding[4].y, root.surrounding[4].z);
            tempMin = new vertex(root.surrounding[4].x, root.surrounding[4].y - currentLen / 2,
                    root.surrounding[4].z - currentLen / 2);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[4] = temp;

            // 5
            tempMax = new vertex(root.surrounding[5].x, root.surrounding[5].y, root.surrounding[5].z);
            tempMin = new vertex(root.surrounding[5].x - currentLen / 2, root.surrounding[5].y - currentLen / 2,
                    root.surrounding[5].z - currentLen / 2);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[5] = temp;

            // 6
            tempMax = new vertex(root.surrounding[6].x, root.surrounding[6].y, root.surrounding[6].z + currentLen / 2);
            tempMin = new vertex(root.surrounding[6].x - currentLen / 2, root.surrounding[6].y - currentLen / 2,
                    root.surrounding[6].z);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[6] = temp;

            // 7
            tempMax = new vertex(root.surrounding[7].x + currentLen / 2, root.surrounding[7].y,
                    root.surrounding[7].z + currentLen / 2);
            tempMin = new vertex(root.surrounding[7].x, root.surrounding[7].y - currentLen / 2, root.surrounding[7].z);
            temp = new Node(starting_cube(tempMax, tempMin), root);
            root.children[7] = temp;

            split_cube(root.children[0], depth + 1, max_depth);
            split_cube(root.children[1], depth + 1, max_depth);
            split_cube(root.children[2], depth + 1, max_depth);
            split_cube(root.children[3], depth + 1, max_depth);
            split_cube(root.children[4], depth + 1, max_depth);
            split_cube(root.children[5], depth + 1, max_depth);
            split_cube(root.children[6], depth + 1, max_depth);
            split_cube(root.children[7], depth + 1, max_depth);

            // for(int i=0;i<8;i++){
            // System.out.println("num_"+i+":\n"+"x:
            // "+root.children[0].surrounding[i].x+"\ny:
            // "+root.children[0].surrounding[i].y+"\nz:
            // "+root.children[0].surrounding[i].z+"\n");
            // }

        }

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

        /* Main function that writes into the destination file */
        public static void build_cubes(String fileDest, Node node) {
            if (is_leaf(node)) {
                if (node.isActive) {
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
                }
            } else {
                for (int i = 0; i < 8; i++) {
                    build_cubes(fileDest, node.children[i]);
                }
            }
        }
    };

    // Create a tree with starting vertices of max and min of x y and z

    // public static void main(String[] args) throws Exception {
    // Scanner scancli = new Scanner(System.in);
    // System.out.println("File dir: ");
    // String filedir = scancli.nextLine();
    // System.out.println("reading " + filedir);
    // File file = new File(filedir);

    // try (Scanner read = new Scanner(file)) {
    // while (read.hasNextLine()) {
    // String data = read.nextLine();
    // String[] splitData = data.trim().split("\\s+");
    // if (splitData[0].equals("v")) {
    // double x = Double.parseDouble(splitData[1]);
    // double y = Double.parseDouble(splitData[2]);
    // double z = Double.parseDouble(splitData[3]);
    // vertex temp = new vertex(x, y, z);

    // vertices.add(temp);
    // find_extremes(temp);
    // }
    // }
    // }
    // // System.out.println("Max tree depth: ");
    // // String maxD = scancli.nextLine();
    // // int D = Integer.parseInt(maxD);
    // int depth = 6;
    // ocTree = new Tree(max, min, depth);

    // try (Scanner read = new Scanner(file)) {
    // while (read.hasNextLine()) {
    // String data = read.nextLine();
    // String[] splitData = data.trim().split("\\s+");

    // // vertex alr stored
    // // if (splitData[0].equals("v")) {
    // // double x = Double.parseDouble(splitData[1]);
    // // double y = Double.parseDouble(splitData[2]);
    // // double z = Double.parseDouble(splitData[3]);
    // // vertex temp = new vertex(x, y, z);
    // // process_vertices(temp, ocTree.root);
    // // }

    // if (splitData[0].equals("f")) {
    // int a = Integer.parseInt(splitData[1].split("/")[0]);

    // // for quads
    // for (int i = 2; i < splitData.length-1; i++) {
    // int b = Integer.parseInt(splitData[i].split("/")[0]);
    // int c = Integer.parseInt(splitData[i+1].split("/")[0]);

    // face temp = new face(vertices.get(a-1), vertices.get(b-1),
    // vertices.get(c-1));
    // faces.add(temp);
    // }
    // }
    // }
    // }

    // double rootlength = ocTree.root.surrounding[5].x -
    // ocTree.root.surrounding[3].x;
    // double steps = rootlength / Math.pow(2, depth);
    // double epsilon = steps / 1000;

    // double minx = ocTree.root.surrounding[3].x, miny =
    // ocTree.root.surrounding[3].y, minz = ocTree.root.surrounding[3].z;
    // double maxx = ocTree.root.surrounding[5].x, maxy =
    // ocTree.root.surrounding[5].y, maxz = ocTree.root.surrounding[5].z;

    // for (double y = miny + steps / 2; y<maxy; y+= steps){
    // for (double z = minz + steps / 2; z<maxz; z+= steps) {
    // vertex origin = new vertex(minx-steps, y, z);
    // // x axis
    // vertex dir = new vertex(1,0,0);
    // for (face f: faces){
    // vertex intersection = new vertex(0,0,0);
    // if(raycasting(origin, dir, f, intersection)){
    // intersection.x += epsilon;
    // process_vertices(intersection, ocTree.root);
    // }
    // }
    // }
    // }

    // for (double x = minx + steps / 2; x<maxx; x+= steps){
    // for (double z = minz + steps / 2; z<maxz; z+= steps) {
    // vertex origin = new vertex(x, miny-steps, z);
    // // y axis
    // vertex dir = new vertex(0,1,0);
    // for (face f: faces){
    // vertex intersection = new vertex(0,0,0);
    // if(raycasting(origin, dir, f, intersection)){
    // intersection.y += epsilon;
    // process_vertices(intersection, ocTree.root);
    // }
    // }
    // }
    // }

    // for (double x = minx + steps / 2; x<maxx; x+= steps){
    // for (double y = miny + steps / 2; y<maxy; y+= steps) {
    // vertex origin = new vertex(x, y, minz-steps);
    // // z axis
    // vertex dir = new vertex(0,0,1);
    // for (face f: faces){
    // vertex intersection = new vertex(0,0,0);
    // if(raycasting(origin, dir, f, intersection)){
    // intersection.z += epsilon;
    // process_vertices(intersection, ocTree.root);
    // }
    // }
    // }
    // }

    // System.out.println("save as: ");
    // String fileDest = scancli.nextLine();
    // // copied this from W3schools lmao
    // try {
    // File newFile = new File("./test/" + fileDest); // Create File object
    // newFile.createNewFile();
    // new FileWriter("./test/" + fileDest).close();
    // } catch (IOException e) {
    // System.out.println("An error occurred.");
    // e.printStackTrace(); // Print error details
    // }

    // Tree.build_cubes("./test/" + fileDest, ocTree.root);

    // // System.out.println("starting cube: "+ocTree.root.isActive);
    // // System.out.println("starting cube 1.0:
    // "+ocTree.root.children[0].isActive);
    // // System.out.println("starting cube 1.1:
    // "+ocTree.root.children[1].isActive);
    // // System.out.println("starting cube 1.2:
    // "+ocTree.root.children[2].isActive);
    // // System.out.println("starting cube 1.3:
    // "+ocTree.root.children[3].isActive);
    // // System.out.println("starting cube 1.4:
    // "+ocTree.root.children[4].isActive);
    // // System.out.println("starting cube 1.5:
    // "+ocTree.root.children[5].isActive);
    // // System.out.println("starting cube 1.6:
    // "+ocTree.root.children[6].isActive);
    // // System.out.println("starting cube 1.7:
    // "+ocTree.root.children[7].isActive);

    // // System.out.println("Cube?\n"+"x: "+(max.x-min.x)+"\ny:
    // "+(max.y-min.y)+"\nz:
    // // "+(max.z-min.z)+"\n");
    // // System.out.println("MIN:\n"+"x: "+min.x+"\ny: "+min.y+"\nz: "+min.z+"\n");
    // }

    /* Populating the isTrue boolean on corresponding nodes */
    public static void process_vertices(vertex v, Tree.Node node) {
        if (Tree.is_in_cube(node, v)) {
            node.isActive = true;
            for (int i = 0; i < 8; i++) {
                if (node.children[i] != null) {
                    process_vertices(v, node.children[i]);
                }
            }
        }
        // System.out.println("x: "+v.x+"\ny: "+v.y+"\nz: "+v.z+"\n");
    }

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

    public static String objToVoxeL(File file, int depth, String outputName) throws Exception {
        max = new vertex(-999999, -999999, -999999);
        min = new vertex(999999, 999999, 999999);
        verticesCount = 1;
        vertices.clear();
        faces.clear();

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
        double maxx = ocTree.root.surrounding[5].x, maxy =
                ocTree.root.surrounding[5].y, maxz = ocTree.root.surrounding[5].z;

        for (double y = miny + steps / 2; y < maxy; y += steps) {
            for (double z = minz + steps / 2; z < maxz; z += steps) {
                vertex origin = new vertex(minx - steps, y, z);
                // x axis
                vertex dir = new vertex(1, 0, 0);
                for (face f : faces) {
                    vertex intersection = new vertex(0, 0, 0);
                    if (raycasting(origin, dir, f, intersection)) {
                        intersection.x += epsilon;
                        process_vertices(intersection, ocTree.root);
                    }
                }
            }
        }

        for (double x = minx + steps / 2; x < maxx; x += steps) {
            for (double z = minz + steps / 2; z < maxz; z += steps) {
                vertex origin = new vertex(x, miny - steps, z);
                // y axis
                vertex dir = new vertex(0, 1, 0);
                for (face f : faces) {
                    vertex intersection = new vertex(0, 0, 0);
                    if (raycasting(origin, dir, f, intersection)) {
                        intersection.y += epsilon;
                        process_vertices(intersection, ocTree.root);
                    }
                }
            }
        }

        for (double x = minx + steps / 2; x < maxx; x += steps) {
            for (double y = miny + steps / 2; y < maxy; y += steps) {
                vertex origin = new vertex(x, y, minz - steps);
                // z axis
                vertex dir = new vertex(0, 0, 1);
                for (face f : faces) {
                    vertex intersection = new vertex(0, 0, 0);
                    if (raycasting(origin, dir, f, intersection)) {
                        intersection.z += epsilon;
                        process_vertices(intersection, ocTree.root);
                    }
                }
            }
        }

        String fileDest = outputName;
        try {
            File newFile = new File("./test/" + fileDest); 
            File parent = newFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            newFile.createNewFile();
            new FileWriter("./test/" + fileDest).close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        Tree.build_cubes("./test/" + fileDest, ocTree.root);
        return "Saved " + fileDest;
    }
}
