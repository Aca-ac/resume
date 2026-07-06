package com.resume.common;

import org.springframework.security.core.Authentication;

public abstract class BaseController {

    protected Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
