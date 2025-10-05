package com.solitairegame.solitaireapp;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JLayeredPane;
import com.solitairegame.view.GameBoard;
import com.solitairegame.view.GameMenuPanel;
import com.solitairegame.view.GameMain;
import com.solitairegame.view.SolitaireTitleBar;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.Color;

public class SolitaireApp extends JFrame {

    private GameBoard gameBoard;
    private GameMain gameMain;
    private GameMenuPanel menuPanel;
    private SolitaireTitleBar titleBar;

    public SolitaireApp() {
        System.out.println("SolitaireApp: Başlatılıyor.");
        setTitle("Java Solitaire");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        setUndecorated(true);

        try {
            URL iconURL = getClass().getResource("/images/AppLogo.jpg");
            if (iconURL != null) {
                Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
                setIconImage(icon);
            } else {
                System.err.println("SolitaireApp: AppLogo.jpg bulunamadı. Lütfen 'src/main/resources/images/AppLogo.jpg' yolunu kontrol edin.");
            }
        } catch (Exception e) {
            System.err.println("SolitaireApp: Uygulama ikonu yüklenirken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(
            GameBoard.PREFERRED_WIDTH + GameMenuPanel.PREFERRED_WIDTH,
            GameBoard.PREFERRED_HEIGHT
        ));

        gameBoard = new GameBoard();
        menuPanel = new GameMenuPanel(gameBoard, this);
        gameMain = new GameMain(this, gameBoard);
        gameMain.setVisible(false);

        gameBoard.setMenuPanel(menuPanel);

        titleBar = new SolitaireTitleBar(this, "Java Solitaire");

        layeredPane.add(gameBoard, JLayeredPane.DEFAULT_LAYER);
        gameBoard.setBounds(0, 0, GameBoard.PREFERRED_WIDTH, GameBoard.PREFERRED_HEIGHT);

        layeredPane.add(menuPanel, JLayeredPane.DEFAULT_LAYER);
        menuPanel.setBounds(GameBoard.PREFERRED_WIDTH, 0, GameMenuPanel.PREFERRED_WIDTH, GameBoard.PREFERRED_HEIGHT);

        layeredPane.add(gameMain, JLayeredPane.PALETTE_LAYER);
        gameMain.setBounds(0, 0, GameBoard.PREFERRED_WIDTH, GameBoard.PREFERRED_HEIGHT);

        setLayout(new BorderLayout());
        add(titleBar, BorderLayout.NORTH);
        add(layeredPane, BorderLayout.CENTER);

        pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = getSize();
        int x = (screenSize.width - windowSize.width) / 2;
        int y = (screenSize.height - windowSize.height) / 2;
        setLocation(x, y);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.out.println("SolitaireApp: ESC tuşuna basıldı.");
                    toggleMainMenu();
                }
            }
        });
        setFocusable(true);
        requestFocusInWindow();

        setVisible(true);
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public GameMain getGameMain() {
        return gameMain;
    }

    public void toggleMainMenu() {
        System.out.println("SolitaireApp: toggleMainMenu() çağrıldı. isMainMenuVisible: " + gameMain.isMainMenuVisible()); // DEBUG
        if (gameMain.isMainMenuVisible()) {
            gameMain.hideMainMenu();
            System.out.println("SolitaireApp: Ana menü gizlendi. Zamanlayıcıya devam ediliyor..."); // DEBUG
            if (menuPanel != null) {
                menuPanel.resumeTimer(); // BURADA resumeTimer() OLMALI!
            }
        } else {
            System.out.println("SolitaireApp: Ana menü gösteriliyor. Zamanlayıcı duraklatılıyor..."); // DEBUG
            if (menuPanel != null) {
                menuPanel.pauseTimer();
            }
            gameMain.showMainMenu();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SolitaireApp();
        });
    }
}