package com.gigmatch.engine;

import com.gigmatch.match.dto.ProviderBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class MatchingAuctionTest {

    private MatchingAuction auction;

    @BeforeEach
    void setUp() {
        auction = new MatchingAuction();
    }

    @Test
    void emptyBids_returnsEmpty() {
        Optional<AuctionWinner> result = auction.selectWinner(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void singleBidder_paysSelf() {
        ProviderBid bid = new ProviderBid("provider-a", 1L, 1L, 10000, 5, "test");
        ScoredBid scored = new ScoredBid(bid, 0.9, 0.8, 0.72);

        Optional<AuctionWinner> result = auction.selectWinner(List.of(scored));

        assertThat(result).isPresent();
        assertThat(result.get().winner()).isEqualTo(scored);
        assertThat(result.get().clearingPriceCents()).isEqualTo(10000); // pays own quote
    }

    @Test
    void twoBidders_winnerPaysRunnerUpQuote() {
        // Provider A: higher effectiveScore but LOWER quote
        ProviderBid bidA = new ProviderBid("provider-a", 1L, 1L, 8000, 3, "test");
        ScoredBid scoredA = new ScoredBid(bidA, 0.95, 0.85, 0.807); // high quality

        // Provider B: lower effectiveScore and HIGHER quote
        ProviderBid bidB = new ProviderBid("provider-b", 2L, 2L, 12000, 5, "test");
        ScoredBid scoredB = new ScoredBid(bidB, 0.60, 0.60, 0.360); // lower quality

        Optional<AuctionWinner> result = auction.selectWinner(List.of(scoredA, scoredB));

        assertThat(result).isPresent();
        AuctionWinner winner = result.get();
        assertThat(winner.winner().original().providerKey()).isEqualTo("provider-a");
        // Clearing price = runner-up (B)'s raw quote
        assertThat(winner.clearingPriceCents()).isEqualTo(12000);
    }

    @Test
    void qualityScoreFlipsWinner() {
        // Provider A: higher raw quote, but poor quality → lower effectiveScore
        ProviderBid bidA = new ProviderBid("high-quote-low-quality", 1L, 1L, 20000, 7, "test");
        ScoredBid scoredA = new ScoredBid(bidA, 0.50, 0.50, 0.25); // effectiveScore=0.25

        // Provider B: lower raw quote, high quality → higher effectiveScore
        ProviderBid bidB = new ProviderBid("low-quote-high-quality", 2L, 2L, 10000, 4, "test");
        ScoredBid scoredB = new ScoredBid(bidB, 0.95, 0.80, 0.76); // effectiveScore=0.76

        Optional<AuctionWinner> result = auction.selectWinner(List.of(scoredA, scoredB));

        assertThat(result).isPresent();
        // B wins despite lower raw quote because of higher quality score
        assertThat(result.get().winner().original().providerKey()).isEqualTo("low-quote-high-quality");
        // Clearing price = runner-up A's raw quote
        assertThat(result.get().clearingPriceCents()).isEqualTo(20000);
    }

    @Test
    void zeroClearingPrice_whenEffectiveScoreIsZero_excluded() {
        // Providers with effectiveScore > 0 only
        ProviderBid bid1 = new ProviderBid("p1", 1L, 1L, 5000, 3, "test");
        ScoredBid s1 = new ScoredBid(bid1, 0.8, 0.7, 0.56);

        ProviderBid bid2 = new ProviderBid("p2", 2L, 2L, 3000, 2, "test");
        ScoredBid s2 = new ScoredBid(bid2, 0.7, 0.9, 0.63);

        Optional<AuctionWinner> result = auction.selectWinner(List.of(s1, s2));
        assertThat(result).isPresent();
        assertThat(result.get().winner().original().providerKey()).isEqualTo("p2"); // higher effectiveScore
    }

    @Test
    void threeBidders_secondPriceIsCorrect() {
        ScoredBid a = new ScoredBid(new ProviderBid("a", 1L, 1L, 5000, 3, "x"), 0.9, 0.9, 0.81);
        ScoredBid b = new ScoredBid(new ProviderBid("b", 2L, 2L, 7000, 4, "x"), 0.8, 0.7, 0.56);
        ScoredBid c = new ScoredBid(new ProviderBid("c", 3L, 3L, 4000, 5, "x"), 0.7, 0.6, 0.42);

        Optional<AuctionWinner> result = auction.selectWinner(List.of(a, b, c));
        assertThat(result).isPresent();
        assertThat(result.get().winner().original().providerKey()).isEqualTo("a");
        assertThat(result.get().clearingPriceCents()).isEqualTo(7000); // second place B's quote
    }
}
