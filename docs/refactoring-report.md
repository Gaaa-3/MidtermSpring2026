# Refactoring Report

## What behavior did you characterize before refactoring?

The existing `selfTest()` had 9 checks covering basic card utilities and one bot scenario.
Before touching any logic, I expanded it to 68 checks covering:

- `Card.color()` for all four colors and both wild cards (no color)
- `Card.rank()` for all six rank types, including every color variant of SKIP, REVERSE, and DRAW_TWO
- `Card.points()` for every point tier (NUMBER, action, wild) — directly exercising the scoring logic the win calculation uses
- `Rules.isLegal()` for every matching rule: same color, same number, called color, same action type, wild always legal, wild-draw-four always legal, mismatches that must be illegal, zero matching zero across colors, called-color override after wild
- `BotStrategy.selectCard()` for all four priority levels (DRAW_TWO > SKIP > NUMBER > WILD) and the draw case
- `BotStrategy.selectColor()` for most-frequent-color selection
- `next()` for all four direction/wrap combinations: forward, forward wrap, reverse, reverse wrap
- `draw()` for the deck-refill-from-discard path (triggered when deck is empty) and the emergency-wild path (when both deck and discard are empty)
- `ConsoleView.join()` for correct hand formatting (index:card pairs)

These checks run via `--self-test` with no game loop, no I/O, and no randomness.

## What were the worst design problems?

1. **Tripled legal-play logic.** The five-condition legality check was copy-pasted verbatim into three places: `isLegal()`, the game loop's `ok` block, and every pass of `chooseBotCard()`. Any rule change had to be made three times.

2. **Console output mixed with game logic.** Every `System.out.println` was inlined inside `playGame()`, making it impossible to test game logic without reading terminal output.

3. **No home for card knowledge.** `color()`, `rank()`, `number()`, and `points()` were static methods on `Main` with no connection to each other and no clear ownership.

4. **Bot logic read global state directly.** `chooseBotCard()` and `chooseBotColor()` read `upCard` and `calledColor` as static fields, making them untestable in isolation.

## Which refactorings did you perform?

**Extract Class — `Card`**
Moved `color()`, `rank()`, `number()`, `points()` out of `Main`. These four methods belong together: they all interpret the same string-encoded card format.

**Extract Class — `Rules`**
Moved `isLegal()` into a dedicated class. This is now the single place where legal-play logic lives.

**Fix duplication in bot logic**
`chooseBotCard()` (now `BotStrategy.selectCard()`) duplicated the five-condition legality check four times across its four passes. Each pass now calls `Rules.isLegal()` instead.

**Extract Class — `BotStrategy`**
Moved `chooseBotCard()` and `chooseBotColor()` into `BotStrategy`. Both methods now receive `upCard` and `calledColor` as parameters instead of reading global state, making them directly testable.

**Extract Class — `ConsoleView`**
Moved all `System.out.println` calls, `askHuman()`, `askColor()`, and the drawn-card prompt into `ConsoleView`. `Main.playGame()` now calls named view methods rather than embedding print strings inline. The `quiet` flag is owned by `ConsoleView` and applied consistently there.

**Extract Method — `applyEffect(card)`**
The long `if/else if` chain dispatching SKIP, REVERSE, DRAW_TWO, WILD_DRAW_FOUR, and normal turn advance was extracted from `playGame()` into a dedicated `applyEffect(String card)` method. `playGame()` now calls a single named method at the end of each turn. Adding a new card effect (e.g., Swap Hands) is now a one-line addition to `applyEffect`.

**Split characterization tests into named methods**
`selfTest()` was restructured into ten named sub-methods: `testCardColor()`, `testCardRank()`, `testCardPoints()`, `testRulesIsLegal()`, `testBotSelectCard()`, `testBotSelectColor()`, `testNext()`, `testDrawMethod()`, `testScoring()`, and `testConsoleViewJoin()`. Each returns its pass count. When a check fails, the method name in the stack trace immediately identifies which group failed without needing to scan a 400-line method.

**Fix `ConsoleView.join()` string concatenation**
Replaced `+=` in a loop with `StringBuilder`, eliminating repeated string allocation on every hand display.

## What behavior did you intentionally preserve?

All of the following are preserved exactly:

- Legal-play rules (color match, number match, action-type match, called color, wilds)
- Bot priority order: DRAW_TWO > SKIP > NUMBER > WILD
- Bot color selection: most frequent color in hand
- Human input quirk: choosing by card index skips the legality check at input time; an illegal index causes a penalty and turn loss
- Human can type `draw` even while holding a legal card
- Bots automatically play a drawn card if it is legal
- SKIP skips the next player; REVERSE flips direction (acts as SKIP in 2-player); DRAW_TWO forces next player to draw two; WILD_DRAW_FOUR forces next player to draw four
- UNO announcement at one card remaining
- Scoring: sum of opponents' remaining card values
- Safety limit at 3000 turns
- Deck reshuffle from discard when deck is empty
- Wild cards are re-drawn at game start if they come up as the first up-card
- All hands visible in the terminal (no hidden information)
- Seed-controlled randomness produces identical output to the original

## What risks remain?

- `Main` still holds global static state (`upCard`, `calledColor`, `currentPlayer`, `direction`, `deck`, `discard`, `hands`). Extracting these into a `GameState` object would be the next natural step but was out of scope for this refactoring.
- `playGame()` is still a long method. The turn loop, action-card dispatch, and win check could each be extracted further.
- Characterization tests run through `--self-test` only; they do not use a test framework (JUnit), so running them in CI requires a shell wrapper.
