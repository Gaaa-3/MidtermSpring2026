# Final Report

## UNO Rules Implemented

All major UNO rules from the reference are implemented:

| Rule | Status |
|------|--------|
| Correct deck composition (108 cards, 4 colors) | Implemented |
| Legal play validation (color, number, type, wild) | Implemented |
| Skip | Implemented |
| Reverse (including 2-player skip variant) | Implemented |
| Draw Two | Implemented |
| Wild (player chooses color) | Implemented |
| Wild Draw Four (player chooses color, next draws 4) | Implemented |
| Draw/pass behavior (draw one, may play if legal) | Implemented |
| UNO call and missed-UNO penalty (2 cards) | Implemented |
| Round scoring (face values + action/wild point values) | Implemented |
| Multi-round game to target score (default 500) | Implemented |

Not implemented: Wild Draw Four challenge rule, Draw Two stacking.
See `docs/rules-supported.md` for full detail and variants.

---

## How to Play from the CLI

```bash
# Quick bot-only game
mvn exec:java -Dexec.args="--bots 3 --quiet"

# Human + 2 bots, play to 200 points
mvn exec:java -Dexec.args="--human --bots 2 --target 200"
```

On a human player's turn:
- The up card and your hand are shown (e.g., `0:R5 1:GS 2:W`)
- Type a card code (`R5`, `W4`) or its index (`0`, `1`) to play
- Type `draw` to draw a card
- After playing a wild, enter the color you want (`R`, `Y`, `G`, or `B`)
- If you go to 1 card, you are asked `Say UNO? (y/n):` — answer `y` to avoid a 2-card penalty

After every round the scores are shown. The game ends when a player reaches the target.

---

## Architecture: Game Logic vs CLI

The final project introduces two new classes that separate rule execution from console interaction.

### GameState (`src/main/java/uno/GameState.java`)

A plain data holder for all mutable game state:
- Player names, human/bot flags, hands
- Deck, discard pile
- Current player index, direction
- Up card, called color
- Cumulative scores, UNO-called flags
- Target score

No game logic lives here — it is just the data.

### GameEngine (`src/main/java/uno/GameEngine.java`)

Pure game logic with no console I/O:
- `buildDeck()` — constructs the 108-card deck
- `setupRound(state, rng)` — shuffles, deals 7 cards each, picks starting card and player
- `draw(state)` — draws from deck; reshuffles discard if empty
- `next(state)` — advances current player by direction with wrap-around
- `applyEffect(state, card)` — applies Skip / Reverse / Draw Two / Wild Draw Four effects; returns victim index when cards are drawn
- `scoreRound(state, winnerIndex)` — sums opponents' hand values and adds to winner's score
- `applyUnoPenalty(state, playerIndex)` — draws 2 penalty cards for a missed UNO

Because `GameEngine` has no scanner, no `System.out`, and no static state, every method can be called directly in a unit test with a constructed `GameState`.

### ConsoleView (`src/main/java/uno/ConsoleView.java`)

All CLI interaction:
- Display methods: turn state, plays, draws, effects, scores, round/game winners
- Input methods: `askHuman`, `askPlayDrawn`, `askColor`, `askUno`

### Main (`src/main/java/uno/Main.java`)

Thin orchestration layer:
- Parses CLI arguments
- Constructs `GameState` and `ConsoleView`
- Runs the multi-round loop (`while (!state.isGameOver())`)
- Each round calls `playRound(state, roundNumber, rng, view)`
- `playRound` uses `GameEngine` for all state changes and `ConsoleView` for all display/input
- Wires persistence (optional, falls back gracefully)

The rule execution path: `Main.playRound` → `GameEngine.*` with no direct I/O calls in the engine.

---

## Tests Added

The test suite is in `src/test/java/uno/`:

### `CardTest` (from A4/A5, unchanged)
26 tests for `Card.color`, `Card.rank`, `Card.points`.

### `RulesTest` (from A4/A5, unchanged)
20 tests for `Rules.isLegal`: legal plays (same color, number, type, called color, wild) and illegal plays.

### `BotStrategyTest` (updated)
11 tests including the new REVERSE priority and all priority-order cases.

### `GameEngineTest` (new — 47 tests)

Organized by rule feature:

- **Deck composition** (9 tests): 108 cards, four colors each with correct counts, Skip/Reverse/Draw Two/Wild/W4 counts
- **Skip** (3 tests): next player loses turn in 3-player, reverse direction, wrap-around
- **Reverse** (4 tests): direction changes, two-player acts like skip, direction flag
- **Draw Two** (3 tests): next player draws 2, loses turn, victim index returned
- **Wild** (2 tests): always legal, called color determines subsequent legality
- **Wild Draw Four** (4 tests): draws 4, loses turn, victim index, always legal
- **Draw/pass** (4 tests): draw from deck, refill from discard, emergency wild, round setup
- **UNO call and penalty** (3 tests): penalty draws 2, flag reset, 1-card state detection
- **Scoring** (4 tests): opponent hand sum, winner's score updated, no self-counting, correct values
- **Multi-round target** (5 tests): isGameOver at/below/above target, winner index, score accumulation
- **ConsoleView helper** (2 tests): join formatting

### `GameRepositoryTest` and `PersistenceTestSupport` (from A5, unchanged)
10 persistence tests — isolated in-memory H2 per test.

---

## Limitations

- Wild Draw Four challenge rule is not implemented (see `docs/rules-supported.md`).
- Draw Two stacking (chaining multiple Draw Twos) is not implemented.
- No graphical interface; text only.
- Bot strategy is deterministic priority-based (not probabilistic).
- UNO missed-call window is immediate (prompt fires at card play time), not at the start of the next player's turn.
