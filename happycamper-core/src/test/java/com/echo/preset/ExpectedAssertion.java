package com.echo.preset;

import com.echo.assertion.AssertionResult;

/**
 * Per-assertion expectation attached to a {@link Preset}'s {@link ExpectedOutputs}.
 *
 * <p>{@link #status} is mandatory — every declared expectation pins a PASSED / FAILED /
 * SKIPPED outcome. {@link #failureCount} is optional: when {@code null}, the consumer
 * checks status only; when non-{@code null}, it is matched exactly against
 * {@link AssertionResult#getFailureCount()}.
 */
public record ExpectedAssertion(AssertionResult.Status status, Integer failureCount) {
}
