# Manual MVP Tournament System: Bug & Risk Analysis

## Executive Summary
This analysis identifies critical risks and potential bugs inherent in the current manual MVP implementation of the Tournament Service. The system relies heavily on administrator (organizer) correctness. While acceptable for a closed MVP, several logical gaps pose significant risks to data integrity and tournament fairness if not addressed or carefully managed.

**Scope:** Analysis of `GameService`, `TournamentService`, and related Entities.
**Status:** Verified via Code Review and Proof-of-Concept Tests (`ManualRiskTest`).

---

## 1. Domain Logic Consistency

### 🚨 Critical Risks

#### 1.1. Player Double Booking (Verified)
*   **Issue:** The system allows an organizer to manually pair a player in multiple games within the same round.
*   **Cause:** `GameService.createGame` checks if the specific pair (A vs B) exists, but does not check if Player A or Player B already have *any* game in the target round.
*   **Impact:** A player could play twice in one round, distorting standings and creating an impossible tournament state.
*   **Verification:** `ManualRiskTest.verify_DoubleBookingIsPossible` passed.

#### 1.2. Standings Desynchronization
*   **Issue:** `TournamentPlayer` statistics (score, wins, etc.) are updated incrementally when game results are set. They are effectively a "cache" of game results.
*   **Risk:** If a direct database modification occurs, or if a future bulk update fails partially, the `TournamentPlayer` stats will permanently drift from the actual `Game` results.
*   **Impact:** Incorrect standings and winner determination.

### ⚠️ Acceptable for MVP (with caution)
*   **Manual Winner Selection:** The organizer manually triggers `completeTournament`, which picks the top player. If standings are tied, the system picks the first one returned by the DB sort (based on defined criteria). This is acceptable but relies on the organizer to resolve ties if the automated sort isn't sufficient.

---

## 2. Tournament & Match Lifecycle

### 🚨 Critical Risks

#### 2.1. Modification of Completed Tournaments (Verified)
*   **Issue:** Game results can be modified (and player scores updated) even after a tournament is marked `COMPLETED`.
*   **Cause:** `GameService.setGameResult` checks if the *Game* is editable (not CANCELLED), but fails to check the *Tournament* status.
*   **Impact:** An organizer could accidentally change a past tournament's result, altering historical data and potentially invalidating the declared winner.
*   **Verification:** `ManualRiskTest.verify_PostCompletionModification` passed.

### ⚠️ Acceptable for MVP
*   **Manual Status Transitions:** The organizer manually moves the tournament from DRAFT to REGISTRATION to IN_PROGRESS. This is by design.

---

## 3. Data Integrity & Concurrency

### 🚨 Critical Risks

#### 3.1. Race Condition on Score Updates
*   **Issue:** `TournamentPlayer` entity lacks `@Version` annotation for optimistic locking.
*   **Scenario:** If two admins update results for two different games involving the same player simultaneously (e.g., in a team event or via the double-booking bug), one update to the player's total score could be lost.
*   **Code Evidence:** `GameService.updatePlayerScores` performs a read-modify-write operation on `TournamentPlayer` without locking.

#### 3.2. Lack of Transactional Boundaries for Multi-Game Operations
*   **Issue:** While individual operations are `@Transactional`, there is no safeguard for complex manual workflows. For example, `rollbackScores` modifies player stats. If the subsequent save fails, the rollback might be partial if not handled correctly (though Spring Transaction usually handles this, the logic complexity increases risk).

---

## 4. API Behavior & Validations

### ⚠️ Missing Validations
*   **Impossible Scores:** No validation prevents setting a score that isn't standard chess scoring (though the Enum `GameResult` restricts this, the `whiteScore`/`blackScore` BigDecimal fields are technically mutable if logic changes).
*   **Color Balance:** No warning if a player is manually assigned White 5 times in a row.

---

## Recommendations

### Minimum Safeguards (Recommended for immediate implementation)
1.  **Prevent Post-Completion Edits:** Add a check in `GameService.setGameResult` and `createGame`:
    ```java
    if (tournament.getStatus() == TournamentStatus.COMPLETED) {
        throw new IllegalStateException("Cannot modify games in a completed tournament");
    }
    ```
2.  **Prevent Double Booking:** Add a check in `GameService.createGame`:
    ```java
    // pseudo-code
    boolean playerBusy = gameRepository.existsByTournamentIdAndRoundAndPlayer(tournamentId, round, playerId);
    if (playerBusy) throw new IllegalStateException("Player already has a game in this round");
    ```
3.  **Enable Optimistic Locking:** Add `@Version` to `TournamentPlayer` to prevent lost score updates.

### Long-term
*   Implement automatic recalculation of standings from Game history (Event Sourcing approach) rather than incremental updates to ensure consistency.
