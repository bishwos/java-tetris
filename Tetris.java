import org.jline.terminal.*;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class TestImmediateRead {
    static final String BLOCK = "[:]";
    static int BLOCK_SIZE = 3;
    static int COLUMNS;
    static int BOTTOM;
    static int CENTER;
    static int[][] points;
    static Random rand = new Random(33);
    static int blockNumber = 0;
    static int xi = 0;

    private static void clear() {
        System.out.print("\033[H\033[2J"); // Moves cursor to home and clears screen
        System.out.flush();
    }

    public static void render() {
        clear();
        for (int y = 0; y < BOTTOM; y++) {
            for (int x = 0; x < COLUMNS; x++) {
                if (points[y][x] == -1) {
                    System.out.print("===");
                } else if (points[y][x] > 0) {
//                    System.out.print("["+points[y][x]+"]");
                    System.out.print("[==]");
                } else {
                    System.out.print("....");
                }
            }
            System.out.print("\r\n");
        }
        System.out.println("xi: " + xi + ", BLOCK: " + blockNumber + ", COLUMNS: " + COLUMNS + ", BOTTOM: " + BOTTOM + ", CENTER: " + CENTER);
    }

    // Method to rotate a block 90 degrees clockwise
    private static int[][][] rotateBlock(int[][][] block) {
        int[][][] rotated = new int[block.length][][];
        
        // Find the center of the block for rotation
        int centerX = 0, centerY = 0;
        int totalPoints = 0;
        
        for (int[][] column : block) {
            for (int[] point : column) {
                centerX += point[0];
                centerY += point[1];
                totalPoints++;
            }
        }
        
        if (totalPoints > 0) {
            centerX /= totalPoints;
            centerY /= totalPoints;
        }
        
        for (int i = 0; i < block.length; i++) {
            rotated[i] = new int[block[i].length][];
            for (int j = 0; j < block[i].length; j++) {
                int[] point = block[i][j];
                // Rotate 90 degrees clockwise around the block's center
                int dx = point[0] - centerX;
                int dy = point[1] - centerY;
                // Apply rotation: (dx, dy) -> (dy, -dx)
                rotated[i][j] = new int[]{centerX + dy, centerY - dx};
            }
        }
        
        return rotated;
    }

    // Method to check if a rotated block can be placed
    private static boolean canPlaceBlock(int[][][] block) {
        for (int[][] column : block) {
            for (int[] point : column) {
                int x = point[0];
                int y = point[1];

                // Check bounds
                if (x < 0 || x >= COLUMNS || y < 0 || y >= BOTTOM) {
                    return false;
                }

                // Check collision with existing blocks
                if (points[y][x] > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty raw </dev/tty"}).waitFor();
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Attributes attributes = terminal.getAttributes();
            Attributes raw = new Attributes(attributes);
            raw.setLocalFlag(Attributes.LocalFlag.ECHO, false);
            NonBlockingReader reader = terminal.reader();
            terminal.flush();

            Size size = terminal.getSize();
            COLUMNS = Math.max(Math.floorDiv(size.getColumns() - 10 - 100, BLOCK_SIZE), 9);
            BOTTOM = Math.max(size.getRows() - 1 - 30, 9);
            CENTER = COLUMNS / 2;


            points = new int[BOTTOM][];
            for (int y = 0; y < BOTTOM; y++) {
                points[y] = new int[COLUMNS];
            }

            boolean newBlock = true;
            int blockCount = 1;
            int[][][] block = new int[][][]{{{CENTER, 0}}};
            ;
            while (true) {
                // Check if any lines are full and fill them with -1 if so.
                for (int y = BOTTOM - 1; y > 0; y--) {
                    boolean lineFull = true;
                    for (int x = 0; x < COLUMNS; x++) {
                        if (points[y][x] <= 0) {
                            lineFull = false;
                            break;
                        }
                    }
                    if (lineFull) {
                        Arrays.fill(points[y], -1);
                    }
                }

                // Shift lines when empty
                int shift = 0;
                for (int y = BOTTOM - 1; y > 0; y--) {
                    if (points[y][0] == -1) {
                        shift++;
                    }
                    if (y - shift >= 0) {
                        points[y] = Arrays.copyOf(points[y - shift], COLUMNS);
                    } else {
                        Arrays.fill(points[y], 0);
                    }
                }

                if (newBlock) {
                    int blockType = rand.nextInt(4, 5);
                    blockNumber = (blockCount * 10) + blockType;
                    blockCount++;
                    block = switch (blockType) {
                        case 2 -> new int[][][]{{{CENTER - 1, 0}}, {{CENTER, 0}}};
                        case 3 -> new int[][][]{{{CENTER - 1, 0}}, {{CENTER, 0}}, {{CENTER + 1, 0}}};
                        case 4 -> new int[][][]{{{CENTER - 1, 0}}, {{CENTER, 0}, {CENTER, 1}}, {{CENTER + 1, 0}}};
                        default -> new int[][][]{{{CENTER, 0}}};
                    };
                }

                int input = reader.read(1000L);
                xi = 0;
                if (input == 'x') {
                    break;
                }
                boolean mirrorX = false;
                switch (input) {
                    case 'a':
                        xi = -1;
                        break;
                    case 'd':
                        xi = 1;
                        break;
                    case 'w':
                        mirrorX = true;
                        break;
                    case 's':
                        break;
                    default:
                }

                // Handle block rotation when 'w' is pressed
                if (mirrorX) {
                    int[][][] rotatedBlock = rotateBlock(block);
                    if (canPlaceBlock(rotatedBlock)) {
                        block = rotatedBlock;
                    }
                }

                // Check if can move in y-axis down
                int yi = 1;
                for (int[][] value : block) {
                    int[] currentBlock = value[value.length - 1];
                    int x = currentBlock[0];
                    int y = currentBlock[1];

                    if (y + yi >= points.length || points[y + yi][x] > 0) {
                        yi = 0;
                        break;
                    }
                }

                // Check if can move in x-axis down
                int[][] edge = xi > 0 ? block[block.length - 1] : block[0];
                if (xi != 0) {
                    for (int[] ints : edge) {

                        int x = ints[0];
                        int y = ints[1];

                        if (x + xi < 0 || x + xi > points[0].length - 1 || points[y][x + xi] > 0) {
                            xi = 0;
                            break;
                        }
                    }
                }

                // Copy block into points
                for (int[][] column : block) {
                    for (int[] cur : column) {
                        cur[0] = cur[0] + xi;
                        if (cur[0] == -1) {
                            continue;
                        }
                        cur[1] = cur[1] + yi;
                        points[cur[1]][cur[0]] = blockNumber;
                    }
                }

                render();

                newBlock = yi == 0;
                if (!newBlock) {
                    for (int[][] column : block) {
                        for (int[] cur : column) {
                            points[cur[1]][cur[0]] = 0;
                        }
                    }
                }
            }
        }
    }
}
