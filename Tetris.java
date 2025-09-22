import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class Tetris extends JPanel implements ActionListener {
    // Board size
    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 20;
    private final int CELL_SIZE = 30;

    // Timer speed (ms per drop)
    private javax.swing.Timer gametimer;
    private int delay = 500;

    // Board: 0 = empty, >0 = color id
    private int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];

    // Current piece
    private Tetromino current;
    private int curRow, curCol; // top-left reference
    private Random rand = new Random();

    // Colors for pieces (index 1..7)
    private static final Color[] COLORS = {
            Color.BLACK,
            Color.CYAN, Color.BLUE, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.MAGENTA, Color.RED
    };

    public Tetris() {
        setPreferredSize(new Dimension(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        initGame();
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> movePiece(-1, 0);
                    case KeyEvent.VK_RIGHT -> movePiece(1, 0);
                    case KeyEvent.VK_DOWN -> softDrop();
                    case KeyEvent.VK_UP -> rotate();
                    case KeyEvent.VK_SPACE -> hardDrop();
                    case KeyEvent.VK_P -> togglePause();
                }
            }
        });
    }

    private void initGame() {
        clearBoard();
        spawnPiece();
        gametimer = new javax.swing.Timer(delay, this);
        gametimer.start();
    }

    private void clearBoard() {
        for (int r = 0; r < BOARD_HEIGHT; r++) Arrays.fill(board[r], 0);
    }

    private void spawnPiece() {
        current = Tetromino.random(rand);
        curRow = 0;
        curCol = BOARD_WIDTH / 2 - 2; // center-ish
        if (!isValidPosition(current.shape, curRow, curCol)) {
            // Game over: stop timer
            gametimer.stop();
            JOptionPane.showMessageDialog(this, "Game Over", "Tetris", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override public void actionPerformed(ActionEvent e) {
        tick();
    }

    private void tick() {
        if (!movePiece(0, 1)) {
            // lock piece
            lockPiece();
            clearLines();
            spawnPiece();
        }
        repaint();
    }

    private boolean movePiece(int dx, int dy) {
        if (isValidPosition(current.shape, curRow + dy, curCol + dx)) {
            curRow += dy;
            curCol += dx;
            return true;
        }
        return false;
    }

    private void softDrop() {
        if (!movePiece(0, 1)) {
            tick(); // if can't move down, lock immediately
        } else {
            repaint();
        }
    }

    private void hardDrop() {
        while (movePiece(0, 1)) {}
        tick(); // lock after hard drop
    }

    private void rotate() {
        int[][] rotated = current.rotatedShape();
        if (isValidPosition(rotated, curRow, curCol)) {
            current.shape = rotated;
        } else {
            // simple wall kick attempts (left/right)
            if (isValidPosition(rotated, curRow, curCol - 1)) curCol--;
            else if (isValidPosition(rotated, curRow, curCol + 1)) curCol++;
        }
        repaint();
    }

    private boolean isValidPosition(int[][] shape, int row, int col) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c] != 0) {
                    int br = row + r;
                    int bc = col + c;
                    if (bc < 0 || bc >= BOARD_WIDTH || br >= BOARD_HEIGHT) return false;
                    if (br >= 0 && board[br][bc] != 0) return false;
                }
            }
        }
        return true;
    }

    private void lockPiece() {
        int colorId = current.colorId;
        for (int r = 0; r < current.shape.length; r++) {
            for (int c = 0; c < current.shape[r].length; c++) {
                if (current.shape[r][c] != 0) {
                    int br = curRow + r;
                    int bc = curCol + c;
                    if (br >= 0 && br < BOARD_HEIGHT && bc >= 0 && bc < BOARD_WIDTH) {
                        board[br][bc] = colorId;
                    }
                }
            }
        }
    }

    private void clearLines() {
        int lines = 0;
        for (int r = BOARD_HEIGHT - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < BOARD_WIDTH; c++) {
                if (board[r][c] == 0) { full = false; break; }
            }
            if (full) {
                lines++;
                // move everything above down
                for (int rr = r; rr > 0; rr--) {
                    board[rr] = Arrays.copyOf(board[rr-1], BOARD_WIDTH);
                }
                board[0] = new int[BOARD_WIDTH];
                r++; // recheck same row index as rows fell
            }
        }
        // optional: speed up after clearing lines or update score
        if (lines > 0) {
            int newDelay = Math.max(50, delay - lines * 10);
            if (newDelay != delay) {
                delay = newDelay;
                gametimer.setDelay(delay);
            }
        }
    }

    private boolean paused = false;
    private void togglePause() {
        paused = !paused;
        if (paused) gametimer.stop(); else gametimer.start();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // draw board cells
        for (int r = 0; r < BOARD_HEIGHT; r++) {
            for (int c = 0; c < BOARD_WIDTH; c++) {
                drawCell(g, c * CELL_SIZE, r * CELL_SIZE, board[r][c]);
            }
        }
        // draw current piece
        for (int r = 0; r < current.shape.length; r++) {
            for (int c = 0; c < current.shape[r].length; c++) {
                if (current.shape[r][c] != 0) {
                    int x = (curCol + c) * CELL_SIZE;
                    int y = (curRow + r) * CELL_SIZE;
                    drawCell(g, x, y, current.colorId);
                }
            }
        }
        // grid lines
        g.setColor(Color.GRAY);
        for (int x = 0; x <= BOARD_WIDTH * CELL_SIZE; x += CELL_SIZE) g.drawLine(x, 0, x, BOARD_HEIGHT * CELL_SIZE);
        for (int y = 0; y <= BOARD_HEIGHT * CELL_SIZE; y += CELL_SIZE) g.drawLine(0, y, BOARD_WIDTH * CELL_SIZE, y);
    }

    private void drawCell(Graphics g, int x, int y, int colorId) {
        if (colorId == 0) {
            g.setColor(Color.BLACK);
            g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            return;
        }
        g.setColor(COLORS[colorId]);
        g.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        g.setColor(COLORS[colorId].brighter());
        g.drawRect(x + 1, y + 1, CELL_SIZE - 3, CELL_SIZE - 3);
    }

    // --- Tetromino definitions ---
    static class Tetromino {
        int[][] shape;
        int colorId;

        Tetromino(int[][] s, int colorId) { this.shape = s; this.colorId = colorId; }

        static Tetromino random(Random r) {
            int i = r.nextInt(7);
            return switch (i) {
                case 0 -> new Tetromino(new int[][]{ {1,1,1,1} }, 1); // I
                case 1 -> new Tetromino(new int[][]{ {2,0,0},{2,2,2} }, 2); // J
                case 2 -> new Tetromino(new int[][]{ {0,0,3},{3,3,3} }, 3); // L
                case 3 -> new Tetromino(new int[][]{ {4,4},{4,4} }, 4); // O
                case 4 -> new Tetromino(new int[][]{ {0,5,5},{5,5,0} }, 5); // S
                case 5 -> new Tetromino(new int[][]{ {6,6,6},{0,6,0} }, 6); // T
                default -> new Tetromino(new int[][]{ {7,7,0},{0,7,7} }, 7); // Z
            };
        }

        // Return a rotated copy (90 degrees clockwise)
        int[][] rotatedShape() {
            int h = shape.length;
            int w = shape[0].length;
            int[][] res = new int[w][h];
            for (int r = 0; r < h; r++) for (int c = 0; c < w; c++) res[c][h - 1 - r] = shape[r][c];
            return res;
        }
    }

    // --- Main launcher ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris - Simple Java Version");
            Tetris game = new Tetris();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
