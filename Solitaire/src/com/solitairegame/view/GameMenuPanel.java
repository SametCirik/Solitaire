package com.solitairegame.view;

import javax.swing.*;

import com.solitairegame.solitaireapp.SolitaireApp;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GameMenuPanel extends JPanel {

    public static final int PREFERRED_WIDTH = 250; 
    public static final int PREFERRED_HEIGHT = 700; 
    
	private JLabel timerLabel;
    private JButton mainMenuButton;
    private JButton settingsButton;
    private JButton aboutButton;

    private Timer gameTimer;
    private long startTime;
    private boolean timerRunning = false;
    private long pausedTime = 0; // Duraklatıldığında geçen süreyi tutar

    private GameBoard gameBoard;
    
    private JFrame parentFrame; 

    public GameMenuPanel(GameBoard gameBoard, JFrame parentFrame) {
        System.out.println("GameMenuPanel: Constructor çağrıldı.");
        this.gameBoard = gameBoard;
        this.parentFrame = parentFrame;
        
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setBackground(new Color(0, 50, 0));

        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(0, 41, 0))); 

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(Box.createRigidArea(new Dimension(0, 200)));

        timerLabel = new JLabel("00:00");
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 36));
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(timerLabel);
        add(Box.createRigidArea(new Dimension(0, 20)));

        mainMenuButton = createMenuButton("Ana Menü");
        settingsButton = createMenuButton("Ayarlar");
        aboutButton = createMenuButton("Hakkında");

        add(mainMenuButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(settingsButton);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(aboutButton);
        add(Box.createRigidArea(new Dimension(0, 10)));

        add(Box.createVerticalGlue());

        addListeners();
        // BURADA STARTTIMER KESİNLİKLE YOK! Constructor'dan kaldırıldı.
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(new Color(50, 150, 50));
        button.setForeground(Color.WHITE);
        button.setMaximumSize(new Dimension(150, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        return button;
    }

    private void addListeners() {
    	mainMenuButton.addActionListener(new ActionListener() {
    	    @Override
    	    public void actionPerformed(ActionEvent e) {
    	        System.out.println("GameMenuPanel: Ana Menü Butonuna basıldı."); 
    	        if (parentFrame instanceof SolitaireApp) {
    	            SolitaireApp solitaireApp = (SolitaireApp) parentFrame;
    	            // Ana menüye dönerken zamanlayıcıyı durdurma sorumluluğu SolitaireApp'e verildi
    	            solitaireApp.toggleMainMenu(); 
    	        } else {
    	            JOptionPane.showMessageDialog(parentFrame, "Ana Menü özelliği çağrılamadı: SolitaireApp bulunamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
    	        }
    	    }
    	});
    	
    	settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("GameMenuPanel: Ayarlar Butonuna basıldı."); 
                if (parentFrame instanceof SolitaireApp) { 
                	JOptionPane.showMessageDialog(parentFrame, "Ayarlar özelliği henüz eklenmedi.", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(parentFrame, "Ayarlar özelliği çağrılamadı: SolitaireApp bulunamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("GameMenuPanel: Hakkında Butonuna basıldı."); 
                JOptionPane.showMessageDialog(parentFrame,
                                              "Java Solitaire Oyunu\nVersiyon: 1.0\nGeliştirici: Samet Cırık",
                                              "Hakkında",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public void pauseTimer() {
        System.out.println("GameMenuPanel: pauseTimer() çağrıldı. Mevcut durum (timerRunning): " + timerRunning); 
        if (timerRunning) {
            timerRunning = false;
            if (gameTimer != null && gameTimer.isRunning()) {
                gameTimer.stop();
                System.out.println("GameMenuPanel: Timer durduruldu (pause)."); 
            }
            pausedTime = System.currentTimeMillis() - startTime; 
            System.out.println("GameMenuPanel: pausedTime kaydedildi: " + pausedTime + " ms."); 
        } else {
            System.out.println("GameMenuPanel: pauseTimer() çağrıldı ama timer zaten durmuş/null durumda."); 
        }
    }

    public void resumeTimer() {
        System.out.println("GameMenuPanel: resumeTimer() çağrıldı. Mevcut durum (timerRunning): " + timerRunning + ", (pausedTime): " + pausedTime + " ms."); 
        if (!timerRunning) { 
            if (gameTimer != null) { // gameTimer'ın initialize edilmiş olması beklenir
                startTime = System.currentTimeMillis() - pausedTime; 
                timerRunning = true;
                gameTimer.start();
                System.out.println("GameMenuPanel: Timer devam ettirildi."); 
            } else {
                // Bu durum sadece startTimer() hiç çağrılmadıysa oluşur.
                // Normal akışta GameBoard animasyon bitince startTimer() çağırır.
                System.out.println("GameMenuPanel: HATA: resumeTimer() çağrıldı ama gameTimer null. Bu bir mantık hatası olabilir."); 
            }
        } else {
            System.out.println("GameMenuPanel: resumeTimer() çağrıldı ama timer zaten çalışıyor."); 
        }
    }


    public void startTimer() {
        System.out.println("GameMenuPanel: startTimer() çağrıldı (YENİ BAŞLANGIÇ)."); 
        startTime = System.currentTimeMillis();
        pausedTime = 0; // Her yeni başlangıçta duraklatılan süreyi sıfırla
        timerRunning = true;

        if (gameTimer != null) { // Mevcut bir timer varsa durdur (isRunning kontrolüne gerek yok, sadece null olmasın yeter)
            gameTimer.stop(); 
            System.out.println("GameMenuPanel: Mevcut timer durduruldu (startTimer içinde)."); 
        }

        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (timerRunning) { // Sadece çalışıyorsa güncelle
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    long elapsedSeconds = elapsedMillis / 1000;
                    long minutes = elapsedSeconds / 60;
                    long seconds = elapsedSeconds % 60;
                    timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
                }
            }
        });
        gameTimer.start();
        System.out.println("GameMenuPanel: Yeni timer başlatıldı (startTimer sonrası)."); 
    }

    public void stopTimer() {
        System.out.println("GameMenuPanel: stopTimer() çağrıldı."); 
        timerRunning = false;
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
            System.out.println("GameMenuPanel: Timer durduruldu (stop)."); 
        } else {
             System.out.println("GameMenuPanel: stopTimer() çağrıldı ama timer zaten durmuş/null durumda."); 
        }
    }

    public void resetTimer() {
    	timerRunning = false;
        System.out.println("GameMenuPanel: resetTimer() çağrıldı."); 
        stopTimer(); // Zamanlayıcıyı durdur
        pausedTime = 0; // Geçen süreyi sıfırla
        timerLabel.setText("00:00"); // Görüntüyü sıfırla
        // KESİNLİKLE BURADA startTimer() VEYA resumeTimer() ÇAĞRILMAYACAK!
        // BAŞLATMA İŞİ SADECE GameBoard'daki animasyon bitiminde yapılacak.
        if (timerRunning) { // Sadece çalışıyorsa güncelle
            long elapsedMillis = System.currentTimeMillis() - startTime;
            long elapsedSeconds = elapsedMillis / 1000;
            long minutes = elapsedSeconds / 60;
            long seconds = elapsedSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    public void onGameWon() {
        System.out.println("GameMenuPanel: onGameWon() çağrıldı."); 
        stopTimer(); 
        JOptionPane.showMessageDialog(parentFrame, "Tebrikler! Oyunu " + timerLabel.getText() + " sürede bitirdiniz.", "Oyun Bitti!", JOptionPane.INFORMATION_MESSAGE);
    }
}