package com.gigmatch.provider;

import com.gigmatch.match.dto.JobRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Holds all registered {@link BidderService} beans and provides O(1) lookup by provider ID.
 * Spring auto-discovers all beans implementing BidderService via the injected List.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProviderRegistry {

    private final List<BidderService> bidders;

    private Map<Long, BidderService> byProviderId;
    private Map<String, BidderService> byProviderKey;

    @PostConstruct
    void init() {
        byProviderId = bidders.stream()
                .collect(Collectors.toMap(BidderService::getProviderId, Function.identity()));
        byProviderKey = bidders.stream()
                .collect(Collectors.toMap(BidderService::getProviderKey, Function.identity()));
        log.info("ProviderRegistry initialised with {} bidder(s): {}",
                bidders.size(),
                bidders.stream().map(BidderService::getProviderKey).toList());
    }

    public List<BidderService> getEligibleBidders(JobRequest request) {
        return bidders.stream()
                .filter(b -> b.isEligible(request))
                .toList();
    }

    public BidderService getByProviderId(long providerId) {
        BidderService bs = byProviderId.get(providerId);
        if (bs == null) throw new IllegalArgumentException("No bidder registered for providerId=" + providerId);
        return bs;
    }

    public BidderService getByProviderKey(String key) {
        BidderService bs = byProviderKey.get(key);
        if (bs == null) throw new IllegalArgumentException("No bidder registered for providerKey=" + key);
        return bs;
    }

    public List<BidderService> getAllBidders() {
        return bidders;
    }
}
