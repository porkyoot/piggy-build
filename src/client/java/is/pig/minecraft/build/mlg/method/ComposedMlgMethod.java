package is.pig.minecraft.build.mlg.method;
import is.pig.minecraft.api.*;

import is.pig.minecraft.build.mlg.method.strategy.MlgCleanupStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionConditionStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgPreparationStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgTickOffsetStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.api.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

public record ComposedMlgMethod(
        boolean negatesAllDamage,
        java.util.function.ToIntBiFunction<net.minecraft.client.Minecraft, is.pig.minecraft.api.FallPredictionResult> reliabilityScoreFunction,
        int getCleanupDifficulty,
        MlgTickOffsetStrategy preparationTickOffset,
        MlgViabilityStrategy viability,
        MlgPreparationStrategy preparation,
        MlgExecutionConditionStrategy executionCondition,
        MlgExecutionStrategy execution,
        MlgCleanupStrategy cleanup,
        float getSelfDamage,
        float getFallDamageMultiplier,
        boolean requiresBounceSettlement,
        boolean isPositionDependent,
        int getItemConsumptionCost
) implements MlgMethod {

    @Override
    public boolean isViable(Minecraft client, FallPredictionResult prediction) {
        if (client.player != null) {
            float expectedFallDamage = 0.0f;
            
            if (getSelfDamage() > 0) {
                expectedFallDamage += getSelfDamage();
            }
            
            if (!negatesAllDamage() && getFallDamageMultiplier() > 0.0f) {
                float baseDamage = Math.max(0, prediction.fallDistance() - 3.0f);
                expectedFallDamage += baseDamage * getFallDamageMultiplier();
            }

            if (expectedFallDamage > 0) {
                float actualDamageTaken = is.pig.minecraft.build.mlg.prediction.HealthPrediction.applyDamageResistances(client.player, expectedFallDamage);
                if (actualDamageTaken >= (client.player.getHealth() + client.player.getAbsorptionAmount())) {
                    return false;
                }
            }
        }

        if (viability == null) return true;
        return viability.isViable(client, prediction);
    }

    @Override
    public boolean canExecute(Minecraft client, FallPredictionResult prediction) {
        if (executionCondition == null) return true; // Default fallback wrapper
        return executionCondition.canExecute(client, prediction);
    }

    @Override
    public void queuePreparationActions(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
        if (preparation != null) {
            preparation.queuePreparation(queue, client, prediction);
        }
    }

    @Override
    public void queueExecutionActions(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
        if (execution != null) {
            execution.queueExecution(queue, client, prediction);
        }
    }

    @Override
    public void queueCleanupActions(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
        if (cleanup != null) {
            cleanup.queueCleanup(queue, client, prediction);
        }
    }

    @Override
    public boolean isCleanupFinished(Minecraft client, FallPredictionResult prediction) {
        if (cleanup == null) return true;
        return cleanup.isFinished(client, prediction);
    }
    
    @Override
    public int getReliabilityScore(Minecraft client, FallPredictionResult prediction) {
        if (reliabilityScoreFunction == null) return 50;
        return reliabilityScoreFunction.applyAsInt(client, prediction);
    }

    public int getPreparationTickOffset(Minecraft client, FallPredictionResult prediction) {
        if (preparationTickOffset == null) return 15;
        return preparationTickOffset.getOffset(client, prediction);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean negatesAllDamage = false;
        private java.util.function.ToIntBiFunction<net.minecraft.client.Minecraft, is.pig.minecraft.api.FallPredictionResult> reliabilityScore = (c, p) -> 50;
        private int cleanupDifficulty = 1;
        private MlgTickOffsetStrategy preparationTickOffset;
        private float selfDamage = 0.0f;
        private float fallDamageMultiplier = 0.0f;
        private boolean requiresBounceSettlement = false;
        private boolean isPositionDependent = true;
        private int itemConsumptionCost = 0;
        
        private MlgViabilityStrategy viability;
        private MlgPreparationStrategy preparation;
        private MlgExecutionConditionStrategy executionCondition;
        private MlgExecutionStrategy execution;
        private MlgCleanupStrategy cleanup;

        public Builder negatesAllDamage(boolean negatesAllDamage) {
            this.negatesAllDamage = negatesAllDamage;
            return this;
        }

        public Builder reliabilityScore(int reliabilityScore) {
            this.reliabilityScore = (c, p) -> reliabilityScore;
            return this;
        }

        public Builder dynamicReliabilityScore(java.util.function.ToIntBiFunction<net.minecraft.client.Minecraft, is.pig.minecraft.api.FallPredictionResult> reliabilityScore) {
            this.reliabilityScore = reliabilityScore;
            return this;
        }

        public Builder cleanupDifficulty(int cleanupDifficulty) {
            this.cleanupDifficulty = cleanupDifficulty;
            return this;
        }

        public Builder preparationTickOffset(MlgTickOffsetStrategy preparationTickOffset) {
            this.preparationTickOffset = preparationTickOffset;
            return this;
        }

        public Builder viability(MlgViabilityStrategy viability) {
            this.viability = viability;
            return this;
        }

        public Builder preparation(MlgPreparationStrategy preparation) {
            this.preparation = preparation;
            return this;
        }

        public Builder executionCondition(MlgExecutionConditionStrategy executionCondition) {
            this.executionCondition = executionCondition;
            return this;
        }

        public Builder execution(MlgExecutionStrategy execution) {
            this.execution = execution;
            return this;
        }

        public Builder cleanup(MlgCleanupStrategy cleanup) {
            this.cleanup = cleanup;
            return this;
        }

        public Builder selfDamage(float selfDamage) {
            this.selfDamage = selfDamage;
            return this;
        }

        public Builder fallDamageMultiplier(float fallDamageMultiplier) {
            this.fallDamageMultiplier = fallDamageMultiplier;
            return this;
        }

        public Builder requiresBounceSettlement(boolean requiresBounceSettlement) {
            this.requiresBounceSettlement = requiresBounceSettlement;
            return this;
        }

        public Builder isPositionDependent(boolean isPositionDependent) {
            this.isPositionDependent = isPositionDependent;
            return this;
        }

        public Builder itemConsumptionCost(int itemConsumptionCost) {
            this.itemConsumptionCost = itemConsumptionCost;
            return this;
        }

        public ComposedMlgMethod build() {
            return new ComposedMlgMethod(
                    negatesAllDamage,
                    reliabilityScore,
                    cleanupDifficulty,
                    preparationTickOffset,
                    viability,
                    preparation,
                    executionCondition,
                    execution,
                    cleanup,
                    selfDamage,
                    fallDamageMultiplier,
                    requiresBounceSettlement,
                    isPositionDependent,
                    itemConsumptionCost
            );
        }
    }
}
