package com.solitairegame.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame; // JFrame'i kullanacağımız için import ettik
import javax.swing.JLabel;
import javax.swing.JOptionPane; // Çıkış onayı için
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.solitairegame.solitaireapp.SolitaireApp; // SolitaireApp'i import ediyoruz

public class SolitaireTitleBar extends JPanel { // Sınıf adını koruduk
	private Point initialClickForDrag;
	private SolitaireApp ownerFrame; // JFrame yerine SolitaireApp kullanıyoruz, çünkü SolitaireApp JFrame'den miras alıyor ve ek metotlara ihtiyacımız olabilir
	private Rectangle normalBounds;
	private boolean frameIsMaximized = false;
	private final int PREFERRED_HEIGHT = 30;
	private JLabel titleLabel;
	private boolean isCurrentlyDragging = false;

	// SolitaireApp'e özel minimum boyutları burada tanımlayalım
	private static final int SOLITAIRE_MIN_WIDTH = 800; // Örnek olarak solitaire için uygun bir minimum genişlik
	private static final int SOLITAIRE_MIN_HEIGHT = 600; // Örnek olarak solitaire için uygun bir minimum yükseklik


	public SolitaireTitleBar(SolitaireApp frame, String title) { // Constructor'ı SolitaireApp alacak şekilde güncelledik
		this.ownerFrame = frame;
		setLayout(new BorderLayout());
		setBackground(Color.decode("#051005")); // Önceki başlık çubuğundaki siyah arka plan
		setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));

		// Sol Taraf: Uygulama İkonu
		JPanel iconDisplayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, (PREFERRED_HEIGHT - 20) / 2)); // 5px sol boşluk, dikey ortalama
		iconDisplayPanel.setOpaque(false);

		URL appIconUrl = getClass().getResource("/images/AppLogo.jpg"); // Resim yolunu kontrol edin: AppLogo.png
		JLabel appIconLabel;
		if (appIconUrl != null) {
			Image iconImage = new ImageIcon(appIconUrl).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
			appIconLabel = new JLabel(new ImageIcon(iconImage));
		} else {
			appIconLabel = new JLabel();
			System.err.println("SolitaireTitleBar: AppLogo.jpg not found in /images/ path!");
		}
		appIconLabel.setPreferredSize(new Dimension(20, 20));
		iconDisplayPanel.add(appIconLabel);

		// Sol panelin sabit genişliği
		iconDisplayPanel.setPreferredSize(new Dimension(PREFERRED_HEIGHT + 10, PREFERRED_HEIGHT)); // İkon ve biraz boşluk için

		add(iconDisplayPanel, BorderLayout.WEST);

		// Sağ Kontrol Düğmeleri Paneli
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttonPanel.setOpaque(false);

		JButton hideButton = createTitleBarButton("_");
		hideButton.addActionListener(e -> ownerFrame.setState(Frame.ICONIFIED));
		buttonPanel.add(hideButton);

		JButton maximizeButton = createTitleBarButton("\u25A2"); // Kare ikonu
		maximizeButton.addActionListener(e -> toggleMaximize());
//		buttonPanel.add(maximizeButton);

		JButton closeButton = createTitleBarButton("X");
		closeButton.setBackground(Color.decode("#C94C4C")); // Kırmızımsı kapatma butonu
		closeButton.addActionListener(e -> showExitConfirmation()); // Çıkış onayı metodunu çağırıyoruz
		buttonPanel.add(closeButton);
		add(buttonPanel, BorderLayout.EAST);

		// Merkezdeki Başlık Etiketi
		titleLabel = new JLabel(title, SwingConstants.CENTER);
		titleLabel.setForeground(Color.decode("#B0B0B0"));
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		add(titleLabel, BorderLayout.CENTER);

		setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));

		// Başlık çubuğunun sürüklenme işlevselliği ve çift tıklama ile maksimize etme
		MouseAdapter dragListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && ownerFrame.getCursor().getType() == Cursor.DEFAULT_CURSOR) {
					initialClickForDrag = e.getPoint();
					isCurrentlyDragging = true;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					isCurrentlyDragging = false;
					initialClickForDrag = null;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (isCurrentlyDragging && initialClickForDrag != null && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
					if (frameIsMaximized) return;

					// Pencerenin yeni konumunu hesapla
					int xMoved = e.getXOnScreen() - (SolitaireTitleBar.this.getLocationOnScreen().x + initialClickForDrag.x);
					int yMoved = e.getYOnScreen() - (SolitaireTitleBar.this.getLocationOnScreen().y + initialClickForDrag.y);
					ownerFrame.setLocation(ownerFrame.getX() + xMoved, ownerFrame.getY() + yMoved);
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// Çift tıklama ile maksimize/restore etme
				if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
					toggleMaximize();
				}
			}
		};
		addMouseListener(dragListener);
		addMouseMotionListener(dragListener);
	}

	public boolean isDragging() {
		return isCurrentlyDragging;
	}

	public void setTitle(String newTitle) {
		if (titleLabel != null) {
			titleLabel.setText(newTitle);
		}
	}

	private void configureTitleBarButton(JButton button) {
		button.setFocusable(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		// Butonların arka planını başlık çubuğunun arka planıyla aynı yapalım: #000000
		button.setBackground(Color.decode("#051005"));
		button.setForeground(Color.WHITE);
		// Mouse hover ve pressed efektleri (önceki koddan alındı)
		button.addMouseListener(new MouseAdapter() {
			private Color originalBg = button.getBackground(); // Orijinal rengi sakla
			@Override
			public void mouseEntered(MouseEvent e) {
				if (button.getText().equals("X")) { // Kapatma butonu için farklı renk
					button.setBackground(Color.decode("#D32F2F")); // Koyu kırmızı
				} else {
					button.setBackground(Color.decode("#1E2A1E")); // Daha açık gri
				}
			}
			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(originalBg); // Orijinal rengine dön
			}
			@Override
			public void mousePressed(MouseEvent e) {
				if (button.getText().equals("X")) {
					button.setBackground(Color.decode("#A00000")); // Daha koyu kırmızı
				} else {
					button.setBackground(Color.decode("#5E5E5E")); // Daha koyu gri
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				button.setBackground(originalBg); // Orijinal rengine dön
			}
		});
	}

	private JButton createTitleBarButton(String text) {
		JButton button = new JButton(text);
		configureTitleBarButton(button);
		button.setPreferredSize(new Dimension(45, PREFERRED_HEIGHT));
		return button;
	}

	// Çıkış onay diyaloğunu gösteren metod (önceki SolitaireTitleBar'dan alındı)
	private void showExitConfirmation() {
		int response = JOptionPane.showConfirmDialog(
			ownerFrame,
			"Oyundan çıkmak istediğinizden emin misiniz?",
			"Çıkış Onayı",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE
		);
		if (response == JOptionPane.YES_OPTION) {
			System.exit(0);
		}
	}

	private void toggleMaximize() {
		if (frameIsMaximized) {
			if(normalBounds != null) {
			    ownerFrame.setBounds(normalBounds);
			} else {
			    // Eğer normalBounds null ise, ekranın ortasına varsayılan bir boyut ver
			    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			    // Solitaire'a özel minimum boyutları kullan
			    int w = Math.max(SOLITAIRE_MIN_WIDTH, screenSize.width / 2);
			    int h = Math.max(SOLITAIRE_MIN_HEIGHT, screenSize.height / 2);
			    ownerFrame.setSize(w,h);
			    ownerFrame.setLocationRelativeTo(null);
			}
		} else {
			if ((ownerFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) { // Sadece normal durumdaysa mevcut sınırları kaydet
				normalBounds = ownerFrame.getBounds();
			}
			Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			ownerFrame.setBounds(screenBounds);
		}
		frameIsMaximized = !frameIsMaximized;
	}

	public boolean isFrameMaximized() {
	    return frameIsMaximized;
	}
}