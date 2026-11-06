// Developer: V.Viswa Vamsi
package nightfall;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import nightfall.ui.Theme;

/**
 * Top-level window host. Owns the JFrame, wires the GamePanel, translates the
 * F11 fullscreen request and routes the menu Quit action back to a normal
 * window-close so shutdown stays in one place.
 */
public class GameWindow {

    private final JFrame frame;
    private final GamePanel panel;

    private Rectangle windowedBounds;
    private boolean fullscreen;

    public GameWindow(GameEngine engine) {
        panel = new GamePanel(engine);

        frame = new JFrame("PROJECT NIGHTFALL");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setResizable(true);
        frame.pack();
        frame.setLocationRelativeTo(null);

        panel.setFullscreenAction(this::toggleFullscreen);
        panel.getInput().setQuitAction(this::requestQuit);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                panel.shutdown();
            }
        });
    }

    /** Shows the window on the Swing event thread. */
    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public JFrame frame() {
        return frame;
    }

    // ======================================================================
    // Fullscreen
    // ======================================================================

    private void toggleFullscreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        if (device == null || !device.isFullScreenSupported()) {
            return;
        }

        if (!fullscreen) {
            // Remember the decorated window bounds so we can restore them.
            windowedBounds = frame.getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            device.setFullScreenWindow(frame);
            fullscreen = true;
        } else {
            device.setFullScreenWindow(null);
            frame.setUndecorated(false);
            if (windowedBounds != null) {
                frame.setBounds(windowedBounds);
            }
            frame.setVisible(true);
            fullscreen = false;
        }
        panel.requestFocusInWindow();
    }

    // ======================================================================
    // Quit
    // ======================================================================

    private void requestQuit() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
