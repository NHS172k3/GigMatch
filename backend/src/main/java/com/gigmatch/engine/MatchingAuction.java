package com.gigmatch.engine;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Pure second-price auction logic with no external dependencies.
 *
 * Algorithm:
 *   1. Sort scoredBids by effectiveScore descending.
 *   2. Winner = highest effectiveScore bidder.
 *   3. Clearing price = second-highest bidder's RAW quoteCents (not effectiveScore).
 *      The quality score determines who wins; the price paid is what the runner-up
 *      would have charged the client. This mirrors the industry-standard quality-adjusted
 *      second-price auction (as used in Google's original AdWords model).
 *   4. If only one bidder, winner pays their own quote.
 *   5. If no bidders → Optional.empty().
 */
@Component
public class MatchingAuction {

    public Optional<AuctionWinner> selectWinner(List<ScoredBid> scoredBids) {
        if (scoredBids.isEmpty()) return Optional.empty();

        List<ScoredBid> sorted = scoredBids.stream()
                .sorted(Comparator.comparingDouble(ScoredBid::effectiveScore).reversed())
                .toList();

        ScoredBid winner = sorted.get(0);

        // Clearing price = runner-up's raw quoteCents
        int clearingPrice = sorted.size() > 1
                ? sorted.get(1).original().quoteCents()
                : winner.original().quoteCents();

        return Optional.of(new AuctionWinner(winner, clearingPrice));
    }
}
