// Developer: V.Viswa Vamsi
package nightfall;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import nightfall.data.AudioManager;
import nightfall.data.SettingsManager;
import nightfall.ui.HUDRenderer;
import nightfall.ui.MenuRenderer;

/**
 * Translates raw mouse and keyboard input into logical 1600x900 coordinates
 * and dispatches it against the current GameState (menu buttons, firing,
 * pause/restart shortcuts).
 */
public class InputController implements MouseListener, MouseMotionListener {

    private final GameEngine engine;

    private double scale = 1.0;
    private double offX;
    private double offY;
    private int mouseX;
    private int mouseY;
    private Runnable quitAction;

    public InputController(GameEngine engine) {
        this.engine = engine;
    }

    /** Called by GamePanel each frame with the current letterbox transform. */
    public void setViewport(double scale, double offX, double offY) {
        this.scale = scale;
        this.offX = offX;
        this.offY = offY;
    }

    /** Last known cursor position in logical space. */
    public int getMouseX() {
        return mouseX;
    }

    /** Last known cursor position in logical space. */
    public int getMouseY() {
        return mouseY;
    }

    /** Action invoked when the player chooses Quit from a menu. */
    public void setQuitAction(Runnable quitAction) {
        this.quitAction = quitAction;
    }

    private void updateMouse(int windowX, int windowY) {
        mouseX = (int) Math.round((windowX - offX) / scale);
        mouseY = (int) Math.round((windowY - offY) / scale);
    }

    // ======================================================================
    // Dispatch
    // ======================================================================

    /** Routes a click given in logical coordinates to the active screen. */
    public void handleClick(int x, int y) {
        GameState state = engine.getState();
        switch (state) {
            case MENU -> {
                if (MenuRenderer.MENU_PLAY.contains(x, y)) {
                    click();
                    engine.startNewGame();
                } else if (MenuRenderer.MENU_QUIT.contains(x, y)) {
                    click();
                    if (quitAction != null) {
                        quitAction.run();
                    }
                } else if (MenuRenderer.DIFF_NOOB.contains(x, y)) {
                    click();
                    engine.getSettings().difficulty = SettingsManager.Difficulty.NOOB;
                } else if (MenuRenderer.DIFF_PRO.contains(x, y)) {
                    click();
                    engine.getSettings().difficulty = SettingsManager.Difficulty.PRO;
                } else if (MenuRenderer.DIFF_GOD.contains(x, y)) {
                    click();
                    engine.getSettings().difficulty = SettingsManager.Difficulty.GOD;
                }
            }
            case PLAYING -> {
                if (HUDRenderer.PAUSE_BUTTON.contains(x, y)) {
                    click();
                    engine.togglePause();
                } else {
                    engine.fireAt(x, y);
                }
            }
            case PAUSED -> {
                if (MenuRenderer.PAUSE_RESUME.contains(x, y)) {
                    click();
                    engine.togglePause();
                } else if (MenuRenderer.PAUSE_RESTART.contains(x, y)) {
                    click();
                    engine.startNewGame();
                } else if (MenuRenderer.PAUSE_MENU.contains(x, y)) {
                    click();
                    engine.toMenu();
                }
            }
            case GAME_OVER -> {
                if (MenuRenderer.OVER_RESTART.contains(x, y)) {
                    click();
                    engine.startNewGame();
                } else if (MenuRenderer.OVER_MENU.contains(x, y)) {
                    click();
                    engine.toMenu();
                }
            }
            case VICTORY -> {
                if (MenuRenderer.VICTORY_PLAY_AGAIN.contains(x, y)) {
                    click();
                    engine.startNewGame();
                } else if (MenuRenderer.VICTORY_MENU.contains(x, y)) {
                    click();
                    engine.toMenu();
                }
            }
            default -> {
                // HOW_TO_PLAY / SETTINGS / HIGH_SCORES screens arrive in a later phase.
            }
        }
    }

    /** Routes a key press (KeyEvent virtual key codes). */
    public void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_P, KeyEvent.VK_ESCAPE -> {
                GameState state = engine.getState();
                if (state == GameState.PLAYING || state == GameState.PAUSED) {
                    click();
                    engine.togglePause();
                }
            }
            case KeyEvent.VK_R -> {
                GameState state = engine.getState();
                if (state == GameState.GAME_OVER || state == GameState.PAUSED) {
                    click();
                    engine.startNewGame();
                }
            }
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                GameState state = engine.getState();
                if (state == GameState.MENU) {
                    click();
                    engine.startNewGame();
                } else if (state == GameState.GAME_OVER) {
                    click();
                    engine.startNewGame();
                } else if (state == GameState.VICTORY) {
                    click();
                    engine.startNewGame();
                }
            }
            default -> {
            }
        }
    }

    private void click() {
        AudioManager audio = engine.getAudio();
        if (audio != null) {
            audio.play(AudioManager.SFX_CLICK);
        }
    }

    // ======================================================================
    // MouseListener / MouseMotionListener (window -> logical space)
    // ======================================================================

    @Override
    public void mousePressed(MouseEvent e) {
        updateMouse(e.getX(), e.getY());
        if (e.getButton() == MouseEvent.BUTTON1) {
            handleClick(mouseX, mouseY);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMouse(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateMouse(e.getX(), e.getY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
