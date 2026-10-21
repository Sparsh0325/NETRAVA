package com.intellishare.dto.intelligence;

import java.util.List;

public record IntelligenceResponse(String text, String provider, boolean webGrounded, String notice, List<Source> sources) {
    public record Source(String title, String uri) { }
}
