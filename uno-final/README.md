# UNO Final — Full UNO Product

A command-line UNO game built progressively from the midterm through A4, A5, and now the final.

## What's New in the Final

- **GameEngine** — all UNO rules live in one testable class, no console I/O
- **GameState** — explicit state object; no more scattered static fields
- Fuller action cards: Skip, Reverse, Draw Two, Wild, Wild Draw Four all fully implemented
- UNO call and missed-UNO penalty
- Multi-round game continues until a player reaches the target score (default 500)
- Comprehensive JUnit 5 test suite covering every rule feature

---

## Prerequisites

- Java 17+
- Maven 3.8+

---

## Build

```bash
mvn compile
```

## Test

```bash
mvn test
```

Expected output ends with something like:

```
[INFO] Tests run: 75+, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Package

```bash
mvn package
```

Produces `target/uno.jar` (fat JAR with all dependencies).

---

## Run

### With Maven (no packaging needed)

```bash
# 3 bots, play to 500 points
mvn exec:java

# 2 bots, quiet output
mvn exec:java -Dexec.args="--bots 2 --quiet"

# Human + 2 bots, play to 200 points
mvn exec:java -Dexec.args="--human --bots 2 --target 200"
```

### From packaged JAR

```bash
java -jar target/uno.jar --bots 3
java -jar target/uno.jar --human --bots 2 --target 200
java -jar target/uno.jar --bots 3 --quiet --no-persist
java -jar target/uno.jar --seed 42 --bots 3
java -jar target/uno.jar --report
```

---

## CLI Options

| Flag | Default | Description |
|------|---------|-------------|
| `--bots N` | 3 | Number of bot players (2–4 total players required) |
| `--human` | off | Add a human player |
| `--target N` | 500 | Score needed to win the game |
| `--quiet` | off | Suppress per-turn output |
| `--seed N` | time-based | Reproducible random seed |
| `--no-persist` | off | Skip database writes |
| `--report` | — | Show game history and stats |
| `--help` | — | Print usage |

---

## Card Codes (human mode)

```
R5     red 5          YS    yellow skip
BR     blue reverse   G+2   green draw two
W      wild           W4    wild draw four
draw   draw a card
```

---

## Game Flow

1. The game starts and announces the target score.
2. Each round: all players get 7 cards; the top non-wild card is turned up.
3. On your turn: enter a card code or index to play, or type `draw`.
4. If a wild is played, you choose the next color (R/Y/G/B).
5. When you reach 1 card, you are prompted to say UNO — skip it and draw 2 penalty cards.
6. The round ends when one player empties their hand; they score the point value of all other players' cards.
7. Play continues through rounds until someone reaches the target score.

---

## Project Structure

```
uno-final/
├── pom.xml
├── README.md
├── docs/
│   ├── rules-supported.md     UNO rules coverage reference
│   ├── final-report.md        Architecture and design writeup
│   └── database.md            Persistence design (carried from A5)
└── src/
    ├── main/java/uno/
    │   ├── Card.java           Card parsing utilities
    │   ├── Rules.java          Legal-play validation
    │   ├── BotStrategy.java    Bot card selection
    │   ├── GameState.java      All mutable game state
    │   ├── GameEngine.java     Pure game logic (no I/O)
    │   ├── ConsoleView.java    All CLI input/output
    │   ├── Main.java           Entry point and round loop
    │   └── persistence/        Hibernate/JPA persistence (from A5)
    └── test/java/uno/
        ├── CardTest.java
        ├── RulesTest.java
        ├── BotStrategyTest.java
        ├── GameEngineTest.java  Comprehensive rule tests
        └── persistence/         Persistence tests (from A5)
```

---

## Logging

Game events are written to `logs/uno.log`.

| Level | Events |
|-------|--------|
| INFO  | Round start/end, cards played, drawn, UNO calls, wild color, winner |
| DEBUG | Every player turn |
| WARN  | Illegal play attempts, invalid input, UNO penalties, safety limit |

---

## Persistence (optional)

Game history stored in `data/uno.mv.db` using Hibernate + H2.
Run with `--no-persist` to skip. Use `--report` to display stored history.
