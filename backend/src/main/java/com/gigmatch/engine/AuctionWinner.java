package com.gigmatch.engine;

/**
 * Result of the second-price auction.
 *
 * @param winner              the highest-effectiveScore bid
 * @param clearingPriceCents  the runner-up's raw quoteCents (second-price rule)
 *                            If only one bidder, winner pays their own quote.
 */
public record AuctionWinner(ScoredBid winner, int clearingPriceCents) {}
