package com.solitairegame.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class GameMain extends JPanel {

    private JFrame parentFrame;
    private GameBoard gameBoard;

    private boolean isMainMenuVisible = false;
    private BufferedImage blurredBoardImage = null;

    public GameMain(JFrame parentFrame, GameBoard gameBoard) {
        this.parentFrame = parentFrame;
        this.gameBoard = gameBoard;

        setOpaque(false);
        // setBackground(new Color(0, 0, 0, 127)); // Artık paintComponent'te daha manuel kontrol edeceğiz

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("Solitaire");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, gbc);

        gbc.insets = new Insets(30, 0, 10, 0);
        add(Box.createRigidArea(new Dimension(0, 0)), gbc);

        gbc.insets = new Insets(10, 0, 10, 0);
        add(createMenuButton("Devam Et"), gbc);
        add(createMenuButton("Yeni Oyun"), gbc);
        add(createMenuButton("Ayarlar"), gbc);
        add(createMenuButton("Çıkış"), gbc);

        addListeners();
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(new Color(50, 150, 50));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(200, 50));
        button.setFocusPainted(false);
        return button;
    }

    private void addListeners() {
        // "Devam Et" butonu
        ((JButton) getComponent(2)).addActionListener(e -> {
            hideMainMenu();
        });

        // "Yeni Oyun" butonu
        ((JButton) getComponent(3)).addActionListener(e -> {
            if (gameBoard != null) {
                gameBoard.resetGame(); // resetGame() zaten zamanlayıcıyı yeniden başlatıyor
                hideMainMenu(); // Menüyü kapat ve oyunun başlamasına izin ver
            }
        });

        // "Ayarlar" butonu
        ((JButton) getComponent(4)).addActionListener(e -> {
            JOptionPane.showMessageDialog(parentFrame, "Ayarlar özelliği henüz eklenmedi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
        });

        // "Çıkış" butonu
        ((JButton) getComponent(5)).addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(
                parentFrame,
                "Oyundan çıkmak istediğinizden emin misiniz?",
                "Çıkış Onayı",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (response == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
    }

    public void showMainMenu() {
        isMainMenuVisible = true;
        setVisible(true);

        if (gameBoard != null && gameBoard.getMenuPanel() != null) {
            gameBoard.getMenuPanel().pauseTimer(); // Menü açılırken zamanlayıcıyı duraklat
        }
        if (gameBoard != null) {
             gameBoard.setEnabled(false);
             new Thread(() -> {
                 BufferedImage currentBoardImage = gameBoard.getBoardImage();
                 if (currentBoardImage != null) {
                     BufferedImage newBlurredImage = applyOptimizedBlur(currentBoardImage);
                     SwingUtilities.invokeLater(() -> {
                         if (isMainMenuVisible) {
                             blurredBoardImage = newBlurredImage;
                             repaint();
                         }
                     });
                 }
             }).start();
        }
        repaint();
    }

    public void hideMainMenu() {
        isMainMenuVisible = false;
        setVisible(false);

        if (gameBoard != null && gameBoard.getMenuPanel() != null) {
            gameBoard.getMenuPanel().resumeTimer(); // <-- Düzeltme: startTimer() yerine resumeTimer() olmalı!
        }
        if (gameBoard != null) {
            gameBoard.setEnabled(true);
            gameBoard.requestFocusInWindow();
        }
        blurredBoardImage = null;
        repaint();
    }

    public boolean isMainMenuVisible() {
        return isMainMenuVisible;
    }

    private BufferedImage applyOptimizedBlur(BufferedImage image) {
        if (image == null) {
            return null;
        }

        int blurRadius = 10;
        int iterations = 2;

        BufferedImage currentImage = image;
        
        for (int i = 0; i < iterations; i++) {
            float[] vertKernel = new float[blurRadius * 2 + 1];
            for (int j = 0; j < vertKernel.length; j++) {
                vertKernel[j] = 1.0f / vertKernel.length;
            }
            Kernel verticalKernel = new Kernel(1, vertKernel.length, vertKernel);
            ConvolveOp vertOp = new ConvolveOp(verticalKernel, ConvolveOp.EDGE_NO_OP, null);
            BufferedImage tempImage = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(), currentImage.getType());
            vertOp.filter(currentImage, tempImage);
            currentImage = tempImage;

            float[] horzKernel = new float[blurRadius * 2 + 1];
            for (int j = 0; j < horzKernel.length; j++) {
                horzKernel[j] = 1.0f / horzKernel.length;
            }
            Kernel horizontalKernel = new Kernel(horzKernel.length, 1, horzKernel);
            ConvolveOp horzOp = new ConvolveOp(horizontalKernel, ConvolveOp.EDGE_NO_OP, null);
            tempImage = new BufferedImage(currentImage.getWidth(), currentImage.getHeight(), currentImage.getType());
            horzOp.filter(currentImage, tempImage);
            currentImage = tempImage;
        }

        return currentImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        if (blurredBoardImage != null) {
            g2d.drawImage(blurredBoardImage, 0, 0, getWidth(), getHeight(), null);
        }

        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.dispose();

        super.paintComponent(g);
    }
}