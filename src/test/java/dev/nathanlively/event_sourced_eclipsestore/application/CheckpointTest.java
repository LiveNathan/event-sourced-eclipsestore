package dev.nathanlively.event_sourced_eclipsestore.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CheckpointTest {

    @Test
    void initialCheckpointIsZero() {
        assertThat(Checkpoint.INITIAL.value())
                .as("The initial checkpoint should have a value of zero")
                .isZero();
    }

    @Test
    void checkpointWithValueOfOneOrMoreIsValid() {
        int validValue = 1;

        Checkpoint checkpoint = Checkpoint.of(validValue);

        assertThat(checkpoint.value())
                .as("Checkpoint created with value 1 should have value 1")
                .isOne();
    }

    @Test
    void checkpointWithValueLessThanOneAndNotInitialIsIllegal() {
        assertThatIllegalArgumentException()
                .as("Creating a Checkpoint with a negative value should throw an IllegalArgumentException")
                .isThrownBy(() -> Checkpoint.of(-1))
                .withMessage("Checkpoint value must be 1 or more.");
    }

    @Test
    void checkpointWithValueOfZeroViaOfIsIllegal() {
        assertThatIllegalArgumentException()
                .as("Creating a Checkpoint with a value of 0 via of() should throw an IllegalArgumentException")
                .isThrownBy(() -> Checkpoint.of(0))
                .withMessage("Checkpoint value must be 1 or more, or use the constant Checkpoint.INITIAL for 0.");
    }
}
