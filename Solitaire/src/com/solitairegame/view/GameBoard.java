package com.solitairegame.view;

import com.solitairegame.model.Card;
import com.solitairegame.model.Deck;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class GameBoard extends JPanel {

    public static final int PREFERRED_WIDTH = 750;
    public static final int PREFERRED_HEIGHT = 600;
    
    private final int CARD_WIDTH = 73;
    private final int CARD_HEIGHT = 98;
    private final int CARD_OVERLAP_Y = 20;
    private final int CARD_HORIZONTAL_SPACING = 20;

    private final int TABLEAU_START_X = 50;
    private final int TABLEAU_START_Y = 150;

    private final int FOUNDATION_START_X = TABLEAU_START_X;
    private final int FOUNDATION_START_Y = 30;

    private final int STOCK_PILE_X = TABLEAU_START_X + (6 * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
    private final int STOCK_PILE_Y = FOUNDATION_START_Y;
    private final int WASTE_PILE_X = STOCK_PILE_X - (CARD_WIDTH + CARD_HORIZONTAL_SPACING);
    private final int WASTE_PILE_Y = FOUNDATION_START_Y;

    private Deck deck;
    private List<List<Card>> tableauPiles;
    private List<Stack<Card>> foundationPiles;
    private Stack<Card> stockPile;
    private Stack<Card> wastePile;

    private boolean dealingAnimationActive = false;
    private Timer animationTimer;
    private Card animatingCard;
    private Point animationStartPoint;
    private Point animationEndPoint;
    private long animationStartTime;
    private final long ANIMATION_DURATION = 150; // Kart başına animasyon süresi (ms)

    private int currentTableauPileIndex = 0;
    private int currentCardInTableauPile = 0;

    private Card draggedCard = null;
    private List<Card> draggedCardPile = null;
    private int dragOffsetX, dragOffsetY;
    private Point initialCardPosition; 
    private Object originalSourcePile = null;
    private int originalSourcePileIndex = -1;

    private boolean gameStarted = false; // Oyunun başlayıp başlamadığını kontrol eder (animasyon sonrası)
    private boolean gameWon = false; // Oyunun kazanılıp kazanılmadığını tutar

    private Image backImage;
    
    private GameMenuPanel menuPanel; // GameMenuPanel referansı

    public GameBoard() {
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setBackground(new Color(0, 100, 0));

        loadCardBackImage();

        deck = new Deck();
        deck.shuffle();

        initializePilesForAnimation();
        addMouseListener(new SolitaireMouseListener());
        addMouseMotionListener(new SolitaireMouseMotionListener());

        startDealingAnimation(); // Oyun açılışında dağıtım animasyonunu başlat
    }
    
    public void setMenuPanel(GameMenuPanel menuPanel) {
        this.menuPanel = menuPanel;
    }
    
    public GameMenuPanel getMenuPanel() {
        return this.menuPanel;
    }

    public void stopTimer() {
        if (menuPanel != null) {
            menuPanel.stopTimer();
        }
    }

    public void startTimer() { 
        if (menuPanel != null) {
            menuPanel.startTimer();
        }
    }

    /**
     * Kartın arka yüzü resmini Toolkit kullanarak yükler.
     */
    private void loadCardBackImage() {
        try {
            URL imageUrl = getClass().getResource("/images/Back.png");
            if (imageUrl != null) {
                Image originalBackImage = Toolkit.getDefaultToolkit().getImage(imageUrl);
                backImage = originalBackImage;
            } else {
                System.err.println("images/Back.png bulunamadı. Dosya yolunu kontrol edin.");
                backImage = null;
            }
        } catch (Exception e) {
            System.err.println("Kart arka yüzü resmi yüklenirken hata oluştu: " + e.getMessage());
            e.printStackTrace();
            backImage = null;
        }
    }


    /**
     * Tüm desteleri animasyona uygun şekilde başlatır.
     * Tüm kartlar başlangıçta stockPile'da olacak.
     */
    private void initializePilesForAnimation() {
        tableauPiles = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            tableauPiles.add(new ArrayList<>()); // Başlangıçta boş desteler
        }

        foundationPiles = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            foundationPiles.add(new Stack<>());
        }

        stockPile = new Stack<>();
        wastePile = new Stack<>();

        // 52 kartın tamamını stockPile'a ekle (kapalı olarak)
        // Kartlar dağıtılırken pop edildiği için desteyi ters çeviriyoruz
        List<Card> tempDeckList = new ArrayList<>();
        while (deck.size() > 0) {
            tempDeckList.add(deck.dealCard());
        }
        Collections.reverse(tempDeckList); // Kartlar pop edildiğinde doğru sırada çıkması için

        for(Card card : tempDeckList) {
            card.setFaceUp(false); // Başlangıçta tüm kartlar kapalı
            stockPile.push(card);
        }
        gameWon = false; // Yeni oyun başladığında kazanma durumunu sıfırla

        // Animasyonla ilgili sayaçları sıfırla
        currentTableauPileIndex = 0;
        currentCardInTableauPile = 0;

        // Sürükleme değişkenlerini de sıfırlamak faydalı olacaktır.
        draggedCard = null;
        draggedCardPile = null;
        originalSourcePile = null;
        originalSourcePileIndex = -1;
    }

    /**
     * Tableau destelerine kart dağıtma animasyonunu başlatır.
     * Kartları stockPile'dan alıp ilgili tableau destelerine hareket ettirir.
     */
    private void startDealingAnimation() {
        dealingAnimationActive = true;
        animationStartTime = System.currentTimeMillis(); // Animasyon başlangıç zamanı

        // Yeni oyun/dağıtım animasyonu başladığında menü panelindeki zamanlayıcıyı sıfırla.
        // Bu noktada başlatma yok, sadece sıfırlama ve durdurma.
        if (menuPanel != null) {
            menuPanel.resetTimer(); 
        }

        prepareNextCardForAnimation(); // İlk kartı animasyona hazırla

        animationTimer = new Timer(10, e -> { // Daha akıcı animasyon için daha kısa gecikme
            if (!dealingAnimationActive) { // Animasyon bittiyse durdur
                ((Timer) e.getSource()).stop();
                return;
            }

            long elapsed = System.currentTimeMillis() - animationStartTime;
            double progress = (double) elapsed / ANIMATION_DURATION;

            if (progress >= 1.0) { // Animasyon tamamlandı
                progress = 1.0; // %100'e sabitle
                if (animatingCard != null) {
                    // Animasyon tamamlandığında kartın yüzünü çevir (en üstteki kartlar için)
                    if (currentCardInTableauPile == currentTableauPileIndex) { // Dağıtılan destenin en üstündeki kart
                        animatingCard.setFaceUp(true);
                    }
                    tableauPiles.get(currentTableauPileIndex).add(animatingCard);
                    animatingCard = null; // Animasyon bitti, kart artık destede
                }

                currentCardInTableauPile++; // Destenin bir sonraki kartına geç

                // Tüm tableau destelerine kartlar dağıtıldı mı kontrol et
                boolean allCardsDealt = true;
                for (int i = 0; i < 7; i++) {
                    // Her destede kendi indeksi kadar kart olmalı (+1, çünkü indeks 0'dan başlıyor)
                    // Örneğin 0. destede 1 kart, 1. destede 2 kart vb.
                    if (tableauPiles.get(i).size() < (i + 1)) {
                        allCardsDealt = false;
                        break;
                    }
                }

                if (!allCardsDealt) { // Hala kart dağıtılacak tableau destesi var
                    // Mevcut destedeki tüm kartlar dağıtıldıysa bir sonraki desteye geç
                    if (currentTableauPileIndex < 7 && currentCardInTableauPile > currentTableauPileIndex) {
                        currentTableauPileIndex++; // Bir sonraki tableau destesine geç
                        currentCardInTableauPile = 0; // Kart sayacını sıfırla
                    }
                    if (currentTableauPileIndex < 7) { // Bir sonraki tableau destesi için kart varsa
                         prepareNextCardForAnimation();
                    } else { // Tüm tableau destelerine dağıtım tamamlandı
                        dealingAnimationActive = false;
                        gameStarted = true; // Oyun artık manuel etkileşime hazır
                        ((Timer) e.getSource()).stop(); // Animasyon zamanlayıcısını durdur
                        // Animasyon bitti, zamanlayıcıyı BAŞLAT
                        if (menuPanel != null) {
                            menuPanel.startTimer(); 
                        }
                    }
                } else { // Tüm kartlar dağıtıldı (animasyon bitti)
                    dealingAnimationActive = false;
                    gameStarted = true; // Oyun artık manuel etkileşime hazır
                    ((Timer) e.getSource()).stop(); // Animasyon zamanlayıcısını durdur
                    // Animasyon bitti, zamanlayıcıyı BAŞLAT
                    if (menuPanel != null) {
                        menuPanel.startTimer();
                    }
                }
            }
            repaint(); // Her kareyi yeniden çiz
        });
        animationTimer.start();
    }

    private void prepareNextCardForAnimation() {
        // Eğer stok destesi boşsa veya tüm tableau desteleri dolduysa animasyonu durdur
        if (stockPile.isEmpty()) {
            dealingAnimationActive = false;
            gameStarted = true;
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            // Tüm kartlar dağıtıldıysa (veya stok boşsa), zamanlayıcıyı başlat.
            // Bu kontrol animasyon döngüsünün dışından da gelebilir, emin olmak için bırakıyoruz.
            if (menuPanel != null) {
                menuPanel.startTimer();
            }
            return;
        }

        // Sıradaki tableau destesine uygun kart sayısı kontrolü
        if (currentTableauPileIndex < 7 && tableauPiles.get(currentTableauPileIndex).size() < (currentTableauPileIndex + 1)) {
            animatingCard = stockPile.pop(); // Stok destesinden kartı al

            animationStartPoint = new Point(STOCK_PILE_X, STOCK_PILE_Y); // Başlangıç: Stock Pile
            animationEndPoint = new Point(
                TABLEAU_START_X + (currentTableauPileIndex * (CARD_WIDTH + CARD_HORIZONTAL_SPACING)),
                TABLEAU_START_Y + (currentCardInTableauPile * CARD_OVERLAP_Y)
            ); // Bitiş: İlgili tableau destesinin konumu

            animationStartTime = System.currentTimeMillis(); // Yeni animasyonun başlangıç zamanı
        } else if (currentTableauPileIndex < 7) {
            // Mevcut desteye dağıtılacak tüm kartlar dağıtıldı, bir sonraki desteye geç
            currentTableauPileIndex++;
            currentCardInTableauPile = 0; // Kart sayacını sıfırla
            prepareNextCardForAnimation(); // Tekrar çağırarak yeni desteye kart hazırlamasını sağla
        } else {
            // Tüm tableau desteleri doldu, ancak hala stokta kart kalmışsa (olmamalı, ama güvenlik)
            dealingAnimationActive = false;
            gameStarted = true;
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            if (menuPanel != null) {
                menuPanel.startTimer(); // Tüm dağıtım bittiğinde başlat
            }
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Hedef destelerini çiz (Foundation Piles)
        for (int i = 0; i < 4; i++) {
            int currentX = FOUNDATION_START_X + (i * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
            int currentY = FOUNDATION_START_Y;
            if (foundationPiles.get(i).isEmpty()) {
                drawEmptyCardSlot(g2d, currentX, currentY);
                // Burada "A" yerine simgeleri çizdiriyoruz
                g2d.setFont(new Font("Arial", Font.BOLD, 24)); // Semboller için daha büyük font
                FontMetrics fm = g2d.getFontMetrics();
                String text = "";
                // Foundation destelerinin sırasına göre uygun sembolü belirle
                // Genellikle kupa, karo, sinek, maça sırası kullanılır
                switch (i) {
                    case 0: text = "♥"; g2d.setColor(Color.RED); break; // Kupa
                    case 1: text = "♦"; g2d.setColor(Color.RED); break; // Karo
                    case 2: text = "♣"; g2d.setColor(Color.BLACK); break; // Sinek
                    case 3: text = "♠"; g2d.setColor(Color.BLACK); break; // Maça
                }
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text, currentX + (CARD_WIDTH - textWidth) / 2, currentY + (CARD_HEIGHT - textHeight) / 2 + fm.getAscent());
                g2d.setFont(new Font("Arial", Font.BOLD, 16)); // Eski fonta geri dön
            } else {
                drawCard(g2d, foundationPiles.get(i).peek(), currentX, currentY);
            }
        }

        // Çekme Destesini (Stock Pile) çiz
        if (stockPile.isEmpty() && !dealingAnimationActive) {
            drawEmptyCardSlot(g2d, STOCK_PILE_X, STOCK_PILE_Y);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "BOŞ";
            int textWidth = fm.stringWidth(text);
            g2d.drawString(text, STOCK_PILE_X + (CARD_WIDTH - textWidth) / 2, STOCK_PILE_Y + CARD_HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
        } else if (!stockPile.isEmpty() && !dealingAnimationActive) {
            // Animasyon yoksa ve stok destesi doluysa üstteki kartı kapalı çiz
            drawCard(g2d, stockPile.peek(), STOCK_PILE_X, STOCK_PILE_Y);
        } else if (dealingAnimationActive && animatingCard == null && !stockPile.isEmpty()) {
             // Animasyon aktif ama henüz yeni kart alınmadıysa veya önceki kart bittiyse stok destesini göster
             drawCard(g2d, stockPile.peek(), STOCK_PILE_X, STOCK_PILE_Y);
        } else if (dealingAnimationActive && stockPile.isEmpty() && animatingCard != null) {
             // Son kart hareket ediyorsa ve stok boşaldıysa, boş slotu göster
             drawEmptyCardSlot(g2d, STOCK_PILE_X, STOCK_PILE_Y);
        }


        // Atık Destesini (Waste Pile) çiz - Sadece en üstteki kartı çiziyoruz.
        if (wastePile.isEmpty()) {
            drawEmptyCardSlot(g2d, WASTE_PILE_X, WASTE_PILE_Y);
        } else {
            drawCard(g2d, wastePile.peek(), WASTE_PILE_X, WASTE_PILE_Y);
        }


        // Oyun alanı destelerini çiz (Tableau Piles)
        for (int i = 0; i < tableauPiles.size(); i++) {
            List<Card> pile = tableauPiles.get(i);
            int currentX = TABLEAU_START_X + (i * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
            int currentY = TABLEAU_START_Y;

            if (pile.isEmpty()) {
                drawEmptyCardSlot(g2d, currentX, currentY);
            } else {
                for (Card card : pile) {
                    // Sürüklenen veya animasyonlu kartı orijinal yerinde çizme
                    if (card != animatingCard && (draggedCardPile == null || !draggedCardPile.contains(card)) && card != draggedCard) {
                        drawCard(g2d, card, currentX, currentY);
                    }
                    currentY += CARD_OVERLAP_Y;
                }
            }
        }

        // Animasyonlu kartı çiz (eğer varsa)
        if (dealingAnimationActive && animatingCard != null) {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            double progress = (double) elapsed / ANIMATION_DURATION;
            if (progress > 1.0) progress = 1.0;

            int currentAnimX = (int) (animationStartPoint.x + (animationEndPoint.x - animationStartPoint.x) * progress);
            int currentAnimY = (int) (animationStartPoint.y + (animationEndPoint.y - animationStartPoint.y) * progress);
            drawCard(g2d, animatingCard, currentAnimX, currentAnimY);
        }


        // Sürüklenen kartı/kart yığınını en üstte çiz
        if (draggedCard != null || draggedCardPile != null) {
            // Fare pozisyonunu almak için
            Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mouseLoc, this);

            int currentDragX = mouseLoc.x - dragOffsetX;
            int currentDragY = mouseLoc.y - dragOffsetY;

            if (draggedCardPile != null) { // Yığın sürükleniyorsa
                for (int i = 0; i < draggedCardPile.size(); i++) {
                    Card cardInPile = draggedCardPile.get(i);
                    drawCard(g2d, cardInPile,
                             currentDragX,
                             currentDragY + (i * CARD_OVERLAP_Y));
                }
            } else if (draggedCard != null) { // Tek kart sürükleniyorsa
                drawCard(g2d, draggedCard,
                         currentDragX,
                         currentDragY);
            }
        }
    }

    /**
     * Tek bir kartı çizmek için yardımcı metod.
     * Kartın açık veya kapalı olmasına göre farklı çizim yapar.
     */
    private void drawCard(Graphics2D g2d, Card card, int x, int y) {
        if (card.isFaceUp()) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT);

            if (card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }

            String rankChar;
            switch (card.getRank()) {
                case ACE:   rankChar = "A"; break;
                case TWO:   rankChar = "2"; break;
                case THREE: rankChar = "3"; break;
                case FOUR:  rankChar = "4"; break;
                case FIVE:  rankChar = "5"; break;
                case SIX:   rankChar = "6"; break;
                case SEVEN: rankChar = "7"; break;
                case EIGHT: rankChar = "8"; break;
                case NINE:  rankChar = "9"; break;
                case TEN:   rankChar = "10"; break;
                case JACK:  rankChar = "J"; break;
                case QUEEN: rankChar = "Q"; break;
                case KING:  rankChar = "K"; break;
                default:    rankChar = "";
            }

            String suitChar;
            switch (card.getSuit()) {
                case CLUBS:    suitChar = "♣"; break;
                case DIAMONDS: suitChar = "♦"; break;
                case HEARTS:   suitChar = "♥"; break;
                case SPADES:   suitChar = "♠"; break;
                default:       suitChar = "";
            }

            // --- KARTIN KÖŞESİNDEKİ KÜÇÜK METİN (ORTALANMAMIŞ) ---
            g2d.setFont(new Font("Arial", Font.BOLD, 14)); // Köşe için daha küçük font
            FontMetrics fmCorner = g2d.getFontMetrics();
            String cornerText = rankChar + suitChar;
            int cornerTextX = x + 5; // Sol üst köşeden biraz içeride
            int cornerTextY = y + fmCorner.getAscent() + 5; // Sol üst köşeden biraz aşağıda
            g2d.drawString(cornerText, cornerTextX, cornerTextY);


            // --- KARTIN ORTASINDAKİ BÜYÜK METİN (ORTALANMIŞ) ---
            String mainCardText = rankChar; // Sadece rütbe ortada
            String mainSuitText = suitChar; // Sadece tür ortada

            g2d.setFont(new Font("Arial", Font.BOLD, 36)); // Merkezdeki metin için daha büyük font
            FontMetrics fmCenter = g2d.getFontMetrics();

            // Rütbeyi ortala
            int rankTextWidth = fmCenter.stringWidth(mainCardText);
            int rankTextX = x + (CARD_WIDTH - rankTextWidth) / 2;
            // Metin yüksekliğini dikkate alarak dikeyde ortalama ve hafif yukarı taşıma
            int rankTextY = y + (CARD_HEIGHT / 2) - (fmCenter.getHeight() / 2) + fmCenter.getAscent() - 10;

            g2d.drawString(mainCardText, rankTextX, rankTextY);

            // Türü (Suit) ortala, rütbenin biraz altında
            int suitTextWidth = fmCenter.stringWidth(mainSuitText);
            int suitTextX = x + (CARD_WIDTH - suitTextWidth) / 2;
            // Metin yüksekliğini dikkate alarak dikeyde ortalama ve rütbenin altında konumlandırma
            int suitTextY = y + (CARD_HEIGHT / 2) + (fmCenter.getHeight() / 2) + fmCenter.getAscent() - 10;

            g2d.drawString(mainSuitText, suitTextX, suitTextY);


        } else {
            // Kart kapalı olduğunda Back.png resmini çiz
            if (backImage != null) {
                g2d.drawImage(backImage, x, y, CARD_WIDTH, CARD_HEIGHT, this);
            } else {
                // Resim yüklenemezse veya bulunamazsa, eski varsayılan mavi arka planı çizmeye devam et
                g2d.setColor(new Color(0, 0, 150));
                g2d.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT);

                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Serif", Font.PLAIN, 12));
                String backText = "Solitaire";
                FontMetrics fmBack = g2d.getFontMetrics();
                int backTextWidth = fmBack.stringWidth(backText);
                g2d.drawString(backText, x + (CARD_WIDTH - backTextWidth) / 2, y + CARD_HEIGHT / 2);
            }
        }
    }

    /**
     * Boş kart yuvasını çizmek için yardımcı metod.
     */
    private void drawEmptyCardSlot(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(0, 50, 0)); // Daha koyu yeşil boşluk
        g2d.fillRect(x, y, CARD_WIDTH, CARD_HEIGHT);
        g2d.setColor(new Color(50, 150, 50)); // Açık yeşil kenarlık
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT);
        g2d.setStroke(new BasicStroke(1));
    }


    /**
     * Oyunun kazanılıp kazanılmadığını kontrol eder.
     * Tüm foundation desteleri dolduğunda oyun kazanılır.
     * @return Oyun kazanıldıysa true, aksi takdirde false döner.
     */
    private boolean checkWinCondition() {
        // Her bir Foundation destesini kontrol et
        for (Stack<Card> foundationPile : foundationPiles) {
            // Bir Foundation destesinde 13 kart (As'tan Papaz'a) olmalı
            if (foundationPile.size() != 13) {
                return false; // Herhangi bir deste eksikse kazanılmamıştır
            }
        }
        return true; // Tüm foundation desteleri dolu ve doğru ise oyun kazanılmıştır
    }

    /**
     * Oyun kazanıldığında çağrılır. Oyuncuya bilgi verir ve oyunu bitirir/yeniden başlatma seçeneği sunar.
     */
    private void handleWin() {
        gameWon = true; // Oyunun kazanıldığını işaretle
        gameStarted = false; // Oyunu durdur

        // Animasyon timer'ını durdur (eğer açıksa)
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // Oyun kazanıldığında menü panelindeki zamanlayıcıyı durdur
        if (menuPanel != null) { 
            menuPanel.onGameWon(); 
        } 

        // Basit bir kazandınız mesajı göster
        int response = JOptionPane.showConfirmDialog(
            this,
            "Tebrikler! Oyunu Kazandınız!\nYeni bir oyun başlatmak ister misiniz?",
            "Oyun Bitti",
            JOptionPane.YES_NO_OPTION
        );

        if (response == JOptionPane.YES_OPTION) { // Düzeltme: JOptionPane.YES_OPTION olacak
            resetGame(); // Oyunu yeniden başlat
        } else {
            System.exit(0); // Uygulamayı kapat
        }
    }

    /**
     * Oyunu başlangıç durumuna sıfırlar.
     */
    public void resetGame() {
        // Önceki animasyon zamanlayıcısını durdur
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        dealingAnimationActive = false; // Animasyon bayrağını sıfırla
        gameStarted = false; // Oyun başlangıç durumuna dönmeli

        // Yeni desteyi oluştur ve karıştır
        deck = new Deck();
        deck.shuffle();

        // Tüm desteleri yeniden başlat ve animasyon sayaçlarını sıfırla
        initializePilesForAnimation();

        // Animasyonu tekrar başlat. Zamanlayıcı, animasyon bitiminde başlayacak.
        startDealingAnimation();

        repaint(); // Tahtayı yeniden çiz
        System.out.println("DEBUG: Oyun sıfırlama tamamlandı. Kartlar dağıtılıyor.");
    }


    // --- Fare Olay Dinleyicileri ---

    private class SolitaireMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            // Animasyon devam ediyorsa veya oyun kazanılmışsa fare etkileşimini engelle
            if (dealingAnimationActive || gameWon) return;

            // Önceki sürükleme durumunu sıfırla
            draggedCard = null;
            draggedCardPile = null;
            originalSourcePile = null;
            originalSourcePileIndex = -1;

            // Stock Pile'a tıklandı mı? (Manual kart çekme)
            if (e.getX() >= STOCK_PILE_X && e.getX() <= STOCK_PILE_X + CARD_WIDTH &&
                e.getY() >= STOCK_PILE_Y && e.getY() <= STOCK_PILE_Y + CARD_HEIGHT) {
                if (!stockPile.isEmpty()) {
                    Card dealtCard = stockPile.pop();
                    dealtCard.setFaceUp(true); // Kartı aç
                    wastePile.push(dealtCard); // Atık destesine koy
                } else if (!wastePile.isEmpty()) {
                    // Stock Pile boşsa ve Waste Pile doluysa, Waste Pile'ı tekrar Stock Pile'a aktar
                    // Kartları ters çevir ve kapalı yap
                    while (!wastePile.isEmpty()) {
                        Card card = wastePile.pop();
                        card.setFaceUp(false);
                        stockPile.push(card);
                    }
                }
                repaint(); // Değişikliği yansıt
                return; // Başka bir işlem yapma
            }

            // Waste Pile'dan kart sürükleme (sadece en üstteki kart)
            if (!wastePile.isEmpty()) {
                Card topWasteCard = wastePile.peek(); 
                int wasteCardX = WASTE_PILE_X;
                int wasteCardY = WASTE_PILE_Y;

                if (e.getX() >= wasteCardX && e.getX() <= wasteCardX + CARD_WIDTH &&
                    e.getY() >= wasteCardY && e.getY() <= wasteCardY + CARD_HEIGHT) {

                    draggedCard = wastePile.pop(); // Kartı wastePile'dan kaldır
                    originalSourcePile = wastePile; // Orijinal desteyi kaydet

                    initialCardPosition = new Point(wasteCardX, wasteCardY); 
                    dragOffsetX = e.getX() - wasteCardX;
                    dragOffsetY = e.getY() - wasteCardY;
                    repaint();
                    return;
                }
            }

            // Tableau Piles'tan kart sürükleme
            for (int i = 0; i < tableauPiles.size(); i++) {
                List<Card> pile = tableauPiles.get(i);
                int currentX = TABLEAU_START_X + (i * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
                int currentY = TABLEAU_START_Y;

                if (!pile.isEmpty()) {
                    // Her bir kartın tıklanıp tıklanmadığını kontrol et
                    for (int j = 0; j < pile.size(); j++) {
                        Card card = pile.get(j);
                        int cardAreaY = TABLEAU_START_Y + (j * CARD_OVERLAP_Y);
                        int cardAreaHeight = (j == pile.size() - 1) ? CARD_HEIGHT : CARD_OVERLAP_Y; 

                        if (e.getX() >= currentX && e.getX() <= currentX + CARD_WIDTH &&
                            e.getY() >= cardAreaY && e.getY() <= cardAreaY + cardAreaHeight) {

                            // Sadece açık kartlar veya açık kartın altındaki kartlar sürüklenmeli
                            if (card.isFaceUp()) {
                                // Sürüklenen karttan başlayarak o destedeki tüm açık kartları al
                                List<Card> tempDraggedPile = new ArrayList<>(pile.subList(j, pile.size()));

                                if (tempDraggedPile.size() == 1) { 
                                    draggedCard = tempDraggedPile.get(0);
                                    draggedCardPile = null; 
                                } else { 
                                    draggedCardPile = tempDraggedPile;
                                    draggedCard = null; 
                                }

                                originalSourcePile = pile; // Orijinal desteyi kaydet (referans)
                                originalSourcePileIndex = i; // Hangi tableau destesi olduğunu kaydet

                                // Sürüklenen kartları desteden geçici olarak kaldır
                                // Sondan başlayarak silmek ConcurrentModificationException'ı engeller
                                for(int k = pile.size() - 1; k >= j; k--) {
                                    pile.remove(k);
                                }

                                initialCardPosition = new Point(currentX, cardAreaY); 
                                dragOffsetX = e.getX() - currentX;
                                dragOffsetY = e.getY() - cardAreaY;

                                repaint(); 
                                return;
                            }
                        }
                    }
                }
            }

            // Foundation Piles'tan kart sürükleme (sadece en üstteki kart)
            for (int i = 0; i < 4; i++) {
                Stack<Card> pile = foundationPiles.get(i);
                int currentX = FOUNDATION_START_X + (i * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
                int currentY = FOUNDATION_START_Y;

                if (!pile.isEmpty()) {
                    Card topCard = pile.peek(); 
                    if (e.getX() >= currentX && e.getX() <= currentX + CARD_WIDTH &&
                        e.getY() >= currentY && e.getY() <= currentY + CARD_HEIGHT) {

                        draggedCard = pile.pop(); // Kartı foundationPile'dan kaldır
                        originalSourcePile = pile; // Orijinal desteyi kaydet

                        initialCardPosition = new Point(currentX, currentY); 
                        dragOffsetX = e.getX() - currentX;
                        dragOffsetY = e.getY() - currentY;
                        repaint();
                        return;
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Oyun kazanıldıysa veya sürüklenen bir kart yoksa işlem yapma
            if (gameWon || (draggedCard == null && draggedCardPile == null)) {
                return;
            }

            boolean placed = false;
            Card cardToPlace = (draggedCardPile != null) ? draggedCardPile.get(0) : draggedCard; 

            // Kartı bir Foundation destesine bırakmayı dene (öncelik verilebilir)
            for (int i = 0; i < 4; i++) {
                Stack<Card> targetFoundation = foundationPiles.get(i);
                int targetX = FOUNDATION_START_X + (i * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
                int targetY = FOUNDATION_START_Y;

                if (e.getX() >= targetX && e.getX() <= targetX + CARD_WIDTH &&
                    e.getY() >= targetY && e.getY() <= targetY + CARD_HEIGHT) {
                    System.out.println("DEBUG: Hedef Foundation (" + i + ") üzerine bırakıldı.");
                    // Sadece tek kart sürükleniyorsa ve foundation'a uygunsa bırak
                    if (draggedCardPile == null) { 
                        System.out.println("DEBUG: Tek kart sürükleniyor.");
                        if (targetFoundation.isEmpty()) {
                            System.out.println("DEBUG: Hedef Foundation boş.");
                            // Boşsa, sadece As bırakılabilir
                            if (cardToPlace.getRank() == Card.Rank.ACE) {
                                targetFoundation.push(cardToPlace);
                                placed = true;
                                System.out.println("DEBUG: As başarıyla Foundation'a bırakıldı.");
                                // KAZANMA KONTROLÜ BURADA
                                if (checkWinCondition()) {
                                    handleWin();
                                }
                                break; 
                            } else {
                                System.out.println("DEBUG: Boş Foundation'a sadece As bırakılabilir. Bırakılan kart: " + cardToPlace.getRank() + " " + cardToPlace.getSuit());
                            }
                        } else {
                            System.out.println("DEBUG: Hedef Foundation boş değil.");
                            Card topFoundationCard = targetFoundation.peek();
                            System.out.println("DEBUG: Foundation'daki en üst kart: " + topFoundationCard.getRank() + " " + topFoundationCard.getSuit() + " (Ordinal: " + topFoundationCard.getRank().ordinal() + ")");
                            System.out.println("DEBUG: Bırakılmak istenen kart: " + cardToPlace.getRank() + " " + cardToPlace.getSuit() + " (Ordinal: " + cardToPlace.getRank().ordinal() + ")");

                            // Aynı tür ve bir üst değer olmalı
                            boolean sameSuit = cardToPlace.getSuit() == topFoundationCard.getSuit();
                            // Bırakılacak kartın rütbesi, hedef kartın rütbesinden bir fazla olmalı
                            boolean correctRank = (cardToPlace.getRank().ordinal()) == (topFoundationCard.getRank().ordinal() + 1);

                            System.out.println("DEBUG: sameSuit: " + sameSuit + ", correctRank: " + correctRank);

                            if (sameSuit && correctRank) {
                                targetFoundation.push(cardToPlace);
                                placed = true;
                                System.out.println("DEBUG: Kart başarıyla Foundation'a bırakıldı.");
                                // KAZANMA KONTROLÜ BURADA
                                if (checkWinCondition()) {
                                    handleWin();
                                }
                                break; 
                            } else {
                                System.out.println("DEBUG: Kart Foundation kurallarına uymuyor. sameSuit: " + sameSuit + ", correctRank: " + correctRank);
                            }
                        }
                    } else {
                        System.out.println("DEBUG: Çoklu kart sürükleniyor, Foundation'a bırakılamaz.");
                    }
                }
            }

            // Kartı bir Tableau destesine bırakmayı dene
            if (!placed) {
                for (int i = 0; i < tableauPiles.size(); i++) {
                    List<Card> targetPile = tableauPiles.get(i);
                    int targetX = TABLEAU_START_X + (i * (CARD_WIDTH + CARD_HORIZONTAL_SPACING));
                    int targetY = TABLEAU_START_Y;

                    // Eğer deste boşsa, hedef Y sadece başlangıç Y'si
                    // Eğer doluysa, hedef Y son kartın altı + boşluk
                    if (!targetPile.isEmpty()) {
                        targetY = TABLEAU_START_Y + (targetPile.size() * CARD_OVERLAP_Y);
                    }

                    // Fare imlecinin kartın bırakıldığı yerdeki konumuna göre kontrol
                    int mouseX = e.getX();
                    int mouseY = e.getY();

                    // Kartın bırakıldığı alan, hedef destenin üzerine denk geliyor mu?
                    // Hedef alanı biraz genişletiyoruz ki kartı bırakmak kolay olsun
                    if (mouseX >= targetX && mouseX <= targetX + CARD_WIDTH &&
                        mouseY >= targetY && mouseY <= targetY + CARD_HEIGHT + CARD_OVERLAP_Y) {

                        // Solitaire kuralı: Kırmızı üstüne siyah, büyük üstüne küçük (K, Q, J, 10...)
                        // Hedef deste boşsa sadece Papaz (King) bırakılabilir.
                        if (targetPile.isEmpty()) {
                            if (cardToPlace.getRank() == Card.Rank.KING) {
                                if (draggedCardPile != null) { // Bir yığın sürükleniyorsa
                                    targetPile.addAll(draggedCardPile);
                                } else { // Tek kart sürükleniyorsa
                                    targetPile.add(draggedCard);
                                }
                                placed = true;
                                break;
                            }
                        } else {
                            Card topTargetCard = targetPile.get(targetPile.size() - 1);
                            // Renkler farklı ve değerler artan sırada olmalı (target'ın bir altı olmalı)
                            boolean sameColor = (cardToPlace.getSuit() == Card.Suit.HEARTS || cardToPlace.getSuit() == Card.Suit.DIAMONDS) ==
                                                (topTargetCard.getSuit() == Card.Suit.HEARTS || topTargetCard.getSuit() == Card.Suit.DIAMONDS);
                            // Bırakılacak kartın rütbesi, hedef kartın rütbesinden bir küçük olmalı
                            boolean correctRank = (cardToPlace.getRank().ordinal() + 1) == topTargetCard.getRank().ordinal();

                            if (!sameColor && correctRank) {
                                if (draggedCardPile != null) {
                                    targetPile.addAll(draggedCardPile);
                                } else {
                                    targetPile.add(draggedCard);
                                }
                                placed = true;
                                break;
                            }
                        }
                    }
                }
            }


            // Eğer kart hiçbir yere bırakılamadıysa, başlangıç konumuna geri koy
            if (!placed) {
                if (originalSourcePile != null) {
                    if (originalSourcePile instanceof Stack) { // Waste veya Foundation'dan geliyorsa
                        if (draggedCard != null) {
                            ((Stack<Card>) originalSourcePile).push(draggedCard);
                        }
                    } else if (originalSourcePile instanceof List) { // Tableau'dan geliyorsa
                        // Tableau destesine geri eklemeden önce, kartların orijinal sıralamasını koru
                        if (draggedCardPile != null && originalSourcePileIndex != -1) {
                            // Orijinal desteye geri ekle
                            List<Card> sourceTableau = tableauPiles.get(originalSourcePileIndex);
                            sourceTableau.addAll(draggedCardPile);
                        } else if (draggedCard != null && originalSourcePileIndex != -1) { // Tek karttableau'dan geliyorsa
                            List<Card> sourceTableau = tableauPiles.get(originalSourcePileIndex);
                            sourceTableau.add(draggedCard);
                        }
                    }
                }
            } else {
                // Başarılı bir şekilde bırakıldıysa, tableau destesinde altındaki kartı açma kontrolü yap
                // Sadece tableau'dan kart alınmışsa ve geride bir kart kalmışsa.
                if (originalSourcePile instanceof List && originalSourcePileIndex != -1) {
                    List<Card> sourceTableau = tableauPiles.get(originalSourcePileIndex);
                    if (!sourceTableau.isEmpty()) {
                        Card topCardInSource = sourceTableau.get(sourceTableau.size() - 1);
                        if (!topCardInSource.isFaceUp()) {
                            topCardInSource.setFaceUp(true);
                        }
                    }
                }
            }

            // Sürükleme değişkenlerini sıfırla
            draggedCard = null;
            draggedCardPile = null;
            originalSourcePile = null;
            originalSourcePileIndex = -1;
            
            // Oyun kazanılmadıysa yeniden çiz
            if (!gameWon) {
                repaint();
            }
        }
    }

    private class SolitaireMouseMotionListener extends MouseMotionAdapter {
        @Override
        public void mouseDragged(MouseEvent e) {
            // Oyun kazanıldıysa kart sürüklemeyi engelle
            if (gameWon) return;

            if (draggedCard != null || draggedCardPile != null) {
                repaint(); // Kartın yeni konumunu çizmek için sürekli yeniden çiz
            }
        }
    }
    
    public BufferedImage getBoardImage() {
        // GameBoard'un mevcut boyutlarında bir BufferedImage oluştur
        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        // GameBoard'un kendisini bu BufferedImage'a çiz
        // Bu, GameBoard üzerindeki tüm kartları ve arka planı içerir
        this.paint(g2d); 
        
        g2d.dispose();
        return image;
    }
}