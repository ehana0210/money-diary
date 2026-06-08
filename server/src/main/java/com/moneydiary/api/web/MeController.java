package com.moneydiary.api.web;

import com.moneydiary.api.security.FirebaseUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Protected endpoint used to confirm that Firebase ID token verification works.
 * Returns the authenticated user's UID and email.
 */
@RestController
public class MeController {

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal FirebaseUser user) {
        Map<String, String> body = new HashMap<>();
        body.put("uid", user.uid());
        body.put("email", user.email());
        return body;
    }
}
