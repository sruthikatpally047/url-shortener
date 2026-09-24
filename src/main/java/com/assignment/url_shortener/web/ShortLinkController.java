package com.assignment.url_shortener.web;

import com.assignment.url_shortener.service.ShortLinkService;
import com.assignment.url_shortener.web.dto.CreateLinkRequest;
import com.assignment.url_shortener.web.dto.AnalyticsResponse;
import com.assignment.url_shortener.web.dto.LinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Tag(name = "Links", description = "Create and inspect short links")
public class ShortLinkController {
    private final ShortLinkService service;

    @GetMapping("/{code}/analytics")
    @Operation(summary = "Get aggregate click analytics", description = "Counts accepted GET redirects, not unique visitors. HEAD and failed redirects do not count.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Aggregate analytics"),
            @ApiResponse(responseCode = "404", description = "Unknown code", content = @Content)})
    public AnalyticsResponse analytics(@PathVariable String code) { return service.analytics(code); }

    @DeleteMapping("/{code}")
    @Operation(summary = "Permanently deactivate a link", description = "Idempotent soft delete. Metadata and analytics remain accessible; the alias remains reserved.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Link disabled", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unknown code", content = @Content)})
    public ResponseEntity<Void> disable(@PathVariable String code) {
        service.disable(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Create a short link")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Short link created"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "409", description = "Alias already allocated", content = @Content),
            @ApiResponse(responseCode = "503", description = "Storage unavailable or retries exhausted", content = @Content)})
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/links/" + response.code())).body(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get short-link metadata")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Link metadata"),
            @ApiResponse(responseCode = "404", description = "Unknown code", content = @Content)})
    public LinkResponse get(@PathVariable String code) { return service.get(code); }
}
