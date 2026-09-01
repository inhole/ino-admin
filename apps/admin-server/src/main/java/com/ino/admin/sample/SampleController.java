package com.ino.admin.sample;

import com.ino.spring.modules.core.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/samples")
public class SampleController {
    private static final List<SampleResponse> SAMPLES = List.of(
            new SampleResponse(1L, "서버 연결"),
            new SampleResponse(2L, "표준 오류 응답"),
            new SampleResponse(3L, "요청 추적 ID")
    );

    @GetMapping
    PageResponse<SampleResponse> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        var from = Math.min(page * size, SAMPLES.size());
        var to = Math.min(from + size, SAMPLES.size());
        var totalPages = (int) Math.ceil((double) SAMPLES.size() / size);
        return new PageResponse<>(SAMPLES.subList(from, to), page, size, SAMPLES.size(), totalPages);
    }

    public record SampleResponse(long id, String name) {}
}
