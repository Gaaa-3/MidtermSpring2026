# UNO Rules Supported

Reference: `Final_Project_UNO_rules_reference.md`

---

## Deck Composition — Implemented

Standard 108-card deck:

- 4 colors: Red (R), Yellow (Y), Green (G), Blue (B)
- 1 zero per color
- 2 cards per number 1–9 per color
- 2 Skip per color
- 2 Reverse per color
- 2 Draw Two per color
- 4 Wild
- 4 Wild Draw Four

**Total: 108 cards.** Verified by `GameEngineTest.buildDeck_has108Cards()`.

---

## Legal Play Validation — Implemented

A card is legal if any of these match:

- same color as the up card
- same number as the up card
- same action type as the up card (Skip on Skip, Reverse on Reverse, Draw Two on Draw Two)
- it is a Wild or Wild Draw Four (always playable)
- its color matches the called color after a wild was played

Illegal plays result in a 1-card penalty and turn loss.

---

## Skip — Implemented

- The next player in turn order loses their turn.
- Play continues with the player after the skipped player.
- Works correctly with both clockwise and counterclockwise direction.

---

## Reverse — Implemented

- Turn direction changes (clockwise ↔ counterclockwise).
- **Two-player variant**: Reverse acts like Skip — the same player takes another turn.
  This matches the common two-player UNO convention and is documented here.

---

## Draw Two — Implemented

- The next player draws 2 cards.
- The next player loses their turn.
- Play continues with the player after the affected player.

**Stacking (chaining Draw Twos)**: Not implemented. Each Draw Two is resolved independently.

---

## Wild — Implemented

- Playable on any up card.
- The player who plays it immediately chooses the next active color (R/Y/G/B).
- Human players are prompted; bots choose the color they hold most of.
- The chosen color is used for all subsequent legal-play checks until another card is played.

---

## Wild Draw Four — Implemented

- Playable on any up card.
- The player chooses the next active color.
- The next player draws 4 cards and loses their turn.
- Play continues with the player after the affected player.

**Challenge rule**: Not implemented. Wild Draw Four is treated as always valid.

---

## Draw / Pass Behavior — Implemented

Variant used: **draw one card, then optionally play it immediately if legal, otherwise pass.**

- If a player has no legal card, they draw one card from the deck.
- If the drawn card is legal, they may choose to play it.
- Bots always play the drawn card if legal.
- Human players are prompted.
- If the drawn card is not legal (or the human declines), the turn passes.

---

## UNO Call and Missed-UNO Penalty — Implemented

- When a player plays a card that leaves them with exactly 1 card, the one-card state is detected.
- **Bots**: automatically call UNO.
- **Human players**: prompted immediately. If they decline (answer "n"), they draw 2 penalty cards right away.
- **Missed UNO from previous turn**: at the start of each turn, if any other player has 1 card and their UNO flag is not set, they receive a 2-card penalty.

Timing note: the human prompt fires at the moment the card is played. This is simpler than a "catch" window and is noted here as a project simplification.

---

## Round Scoring — Implemented

Card point values:
- Number cards: face value (0–9)
- Skip: 20
- Reverse: 20
- Draw Two: 20
- Wild: 50
- Wild Draw Four: 50

The round winner receives the sum of all cards in every other player's hand.

---

## Multi-Round Game — Implemented

- The game continues through multiple rounds.
- Default target score: **500 points**.
- The target is configurable via `--target N`.
- After each round, cumulative scores are displayed.
- The game ends when any player's cumulative score reaches or exceeds the target.
- If multiple players exceed the target in the same round, the player with the highest score wins.

---

## Rules Not Implemented

- Wild Draw Four challenge (optional per reference)
- Draw Two stacking
- Starting card action (if the first up card is a Wild or action card, it is redrawn — no effect is applied)
