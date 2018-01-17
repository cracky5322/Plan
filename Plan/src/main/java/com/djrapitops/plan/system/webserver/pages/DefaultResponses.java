/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.system.webserver.pages;

import com.djrapitops.plan.system.webserver.response.PromptAuthorizationResponse;
import com.djrapitops.plan.system.webserver.response.Response;
import com.djrapitops.plan.system.webserver.response.api.SuccessResponse;
import com.djrapitops.plan.system.webserver.response.errors.NotFoundResponse;

/**
 * Enum containing default responses that don't need to be cached because they're always the same.
 *
 * @author Rsl1122
 */
public enum DefaultResponses {
    NOT_FOUND(
            new NotFoundResponse("Make sure you're accessing a link given by a command, Examples:</p>"
                    + "<p>/player/PlayerName<br>" +
                    "/server/ServerName</p>")
    ),
    BASIC_AUTH(PromptAuthorizationResponse.getBasicAuthResponse()),
    SUCCESS(new SuccessResponse());

    private final Response response;

    DefaultResponses(Response response) {
        this.response = response;
    }

    public Response get() {
        return response;
    }
}