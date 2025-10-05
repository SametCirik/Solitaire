package com.solitairegame.model;

//com.solitairegame.model/Deck.java

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
 private List<Card> cards;

 public Deck() {
     cards = new ArrayList<>();
     // 52 standart iskambil kartını oluştur
     for (Card.Suit suit : Card.Suit.values()) {
         for (Card.Rank rank : Card.Rank.values()) {
             cards.add(new Card(suit, rank));
         }
     }
 }

 public void shuffle() {
     Collections.shuffle(cards);
 }

 public Card dealCard() {
     if (cards.isEmpty()) {
         return null; // Deste boşsa kart yok
     }
     return cards.remove(0); // Desteden en üstteki kartı ver
 }

 public int size() {
     return cards.size();
 }

 public boolean isEmpty() {
     return cards.isEmpty();
 }
}