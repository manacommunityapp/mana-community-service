package com.manacommunity.api.dailyhelp.ranking;

import java.util.concurrent.ThreadLocalRandom;

/**
 * High-performance, zero-dependency Beta Distribution Sampler for Contextual Multi-Armed Bandits.
 * Uses Marsaglia-Tsang Gamma method and CDF inversion for sub-microsecond Thompson Sampling.
 */
public final class BetaDistributionSampler {

    private BetaDistributionSampler() {}

    /**
     * Samples from Beta(alpha, beta) distribution.
     * Guaranteed to return a double in (0.0, 1.0).
     */
    public static double sample(double alpha, double beta) {
        if (alpha <= 0.0 || beta <= 0.0) {
            throw new IllegalArgumentException("Alpha and Beta must be strictly positive");
        }

        // Fast-path: When beta == 1.0, inverse CDF is simply U^(1/alpha)
        if (Math.abs(beta - 1.0) < 1e-6) {
            double u = ThreadLocalRandom.current().nextDouble();
            return Math.pow(Math.max(1e-9, u), 1.0 / alpha);
        }

        // General case: If X ~ Gamma(alpha, 1) and Y ~ Gamma(beta, 1), then X / (X + Y) ~ Beta(alpha, beta)
        double gammaA = sampleGamma(alpha);
        double gammaB = sampleGamma(beta);

        double sum = gammaA + gammaB;
        if (sum <= 0.0) return 0.5;
        return Math.max(0.001, Math.min(0.999, gammaA / sum));
    }

    /**
     * Marsaglia and Tsang method for Gamma(shape, 1) variates.
     */
    private static double sampleGamma(double shape) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        if (shape < 1.0) {
            // For shape < 1: use Gamma(shape + 1) * U^(1/shape)
            double u = rand.nextDouble();
            return sampleGamma(shape + 1.0) * Math.pow(Math.max(1e-9, u), 1.0 / shape);
        }

        double d = shape - (1.0 / 3.0);
        double c = 1.0 / Math.sqrt(9.0 * d);

        while (true) {
            double z = rand.nextGaussian();
            double v = 1.0 + c * z;
            if (v <= 0.0) continue;

            v = v * v * v;
            double u = rand.nextDouble();

            if (u < 1.0 - 0.0331 * z * z * z * z) {
                return d * v;
            }
            if (Math.log(Math.max(1e-9, u)) < 0.5 * z * z + d * (1.0 - v + Math.log(v))) {
                return d * v;
            }
        }
    }
}
