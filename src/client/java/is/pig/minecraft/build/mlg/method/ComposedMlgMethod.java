package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.method.strategy.MlgCleanupStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionConditionStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgPreparationStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

public record ComposedMlgMethod(
        boolean negatesAllDamage,
        int getReliabilityScore,
        int getCleanupDifficulty,
        int getPreparationTickOffset,
        MlgViabilityStrategy viability,
        MlgPreparationStrategy preparation,
        MlgExecutionConditionStrategy executionCondition,
        MlgExecutionStrategy execution,
        MlgCleanupStrategy cleanup
) implements MlgMethod {

    @Override
    public boolean isViable(Minecraft client, FallPredictionResult prediction) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean negatesAllDamage = false;
        private int reliabilityScore = 50;
        private int cleanupDifficulty = 1;
        private int preparationTickOffset = 15;
        
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
            this.reliabilityScore = reliabilityScore;
            return this;
        }

        public Builder cleanupDifficulty(int cleanupDifficulty) {
            this.cleanupDifficulty = cleanupDifficulty;
            return this;
        }

        public Builder preparationTickOffset(int preparationTickOffset) {
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
                    cleanup
            );
        }
    }
}
