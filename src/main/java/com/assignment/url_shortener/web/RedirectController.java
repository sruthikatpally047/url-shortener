package com.assignment.url_shortener.web;

import com.assignment.url_shortener.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "Redirects")
@ApiResponses({
        @ApiResponse(responseCode = "302", description = "Temporary redirect", content = @Content,
                headers = @Header(name = "Location", description = "Original destination", schema = @Schema(type = "string", format = "uri"))),
        @ApiResponse(responseCode = "404", description = "Unknown code", content = @Content),
        @ApiResponse(responseCode = "410", description = "Expired or disabled", content = @Content),
        @ApiResponse(responseCode = "503", description = "Storage unavailable", content = @Content)
})
public class RedirectController {
    private final ShortLinkService service;

    @RequestMapping(value = "/r/{code}", method = RequestMethod.HEAD)
    @Operation(summary = "Check a redirect without recording a click")
    public ResponseEntity<Void> preview(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.preview(code)))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @GetMapping("/r/{code}")
    @Operation(summary = "Follow a short link", description = "Returns a 302 redirect. Use curl without -L to inspect the response.")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.resolve(code)))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
}
