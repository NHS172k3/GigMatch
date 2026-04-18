package com.gigmatch.provider;

import com.gigmatch.match.MatchLogRepository;
import com.gigmatch.portfolio.dto.PortfolioDto;
import com.gigmatch.provider.dto.CreateProviderRequest;
import com.gigmatch.provider.dto.ProviderDto;
import com.gigmatch.provider.dto.ProviderStatsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderAdminService  providerAdminService;
    private final MatchLogRepository    matchLogRepository;

    @GetMapping
    public ResponseEntity<List<ProviderDto>> list() {
        return ResponseEntity.ok(providerAdminService.findAll());
    }

    @PostMapping
    public ResponseEntity<ProviderDto> create(@Valid @RequestBody CreateProviderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerAdminService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(providerAdminService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProviderDto> update(@PathVariable Long id,
                                               @Valid @RequestBody CreateProviderRequest req) {
        return ResponseEntity.ok(providerAdminService.update(id, req));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ProviderStatsDto> stats(@PathVariable Long id) {
        return ResponseEntity.ok(providerAdminService.getStats(id));
    }

    @PostMapping("/{id}/portfolios")
    public ResponseEntity<PortfolioDto> addPortfolio(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerAdminService.addPortfolio(id, title, description, category, file));
    }

    /** Global dashboard stats */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        long totalMatches   = matchLogRepository.countTotalMatches();
        long totalEarnings  = matchLogRepository.sumTotalEarnings();
        long activeProviders = providerAdminService.findAll().stream()
                .filter(p -> "ACTIVE".equals(p.status())).count();
        return ResponseEntity.ok(Map.of(
            "totalMatches",    totalMatches,
            "totalEarnings",   totalEarnings,
            "activeProviders", activeProviders
        ));
    }
}
