# Extension Readiness

## Which extension would your design support best?

**Adding a new card effect** (e.g., a "Swap Hands" card) is the most straightforward extension.

## Where would that change be implemented?

1. **`Card.java`** — add a new `rank()` branch returning `"SWAP_HANDS"` and a `points()` value.
2. **`Rules.java`** — `isLegal()` needs no change; the card would follow normal color/rank matching.
3. **`Main.applyEffect()`** — add one `else if (rank.equals("SWAP_HANDS"))` branch to swap the current player's hand with another player's hand. This is now a single addition to one method rather than a buried change inside a long game loop.
4. **`ConsoleView.java`** — add a `showSwapHands(String playerA, String playerB)` method for the announcement.

No other classes need to change. Before this refactoring, adding a card effect meant touching the monolithic game loop with no clear separation between the print statements, the rule check, and the effect logic.

## What part of your design still makes change difficult?

- **Global state in `Main`.** `playGame()` reads and writes `upCard`, `calledColor`, `currentPlayer`, `direction`, `deck`, `discard`, and `hands` as static fields. Any extension that needs to inspect or snapshot game state (e.g., a replay log or an undo feature) would have to work around this.
- **The turn loop is still one large method.** A smarter bot strategy that needs to reason about the full turn sequence (e.g., chaining action cards) would need the loop to be broken into smaller, callable steps first.
- **String-encoded cards.** Cards are still plain strings. A richer card type (an enum or a small value class) would make adding new card attributes (point overrides, special flags) cleaner, but that change carries a higher behavior-preservation risk and was deferred.
