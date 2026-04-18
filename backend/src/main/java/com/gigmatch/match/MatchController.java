package com.gigmatch.match;

import com.gigmatch.engine.MatchingEngine;
import com.gigmatch.match.dto.JobRequest;
import com.gigmatch.match.dto.MatchLogDto;
import com.gigmatch.match.dto.MatchResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchController {

    private final MatchingEngine    matchingEngine;
    private final MatchLogRepository matchLogRepository;

    /**
     * POST /api/v1/matches
     * Triggers a real-time auction for the given job request.
     * Always returns 200 — check {@code hasMatch} in the response.
     */
    @PostMapping("/matches")
    public ResponseEntity<MatchResult> runMatch(@Valid @RequestBody JobRequest request) {
        MatchResult result = matchingEngine.runMatch(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/match-logs?page=0&size=20&providerId=
     * Returns paginated match log entries, newest first.
     */
    @GetMapping("/match-logs")
    public ResponseEntity<Page<MatchLogDto>> getMatchLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    Long providerId) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<MatchLog> logs = providerId != null
                ? matchLogRepository.findByWinnerProviderIdOrderByCreatedAtDesc(providerId, pageable)
                : matchLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        return ResponseEntity.ok(logs.map(MatchLogDto::from));
    }
}
