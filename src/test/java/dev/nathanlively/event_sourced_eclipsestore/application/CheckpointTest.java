package dev.nathanlively.event_sourced_eclipsestore.application;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CheckpointTest {

    @Nested
    class Value {

        @Test
        void initialCheckpointHasValueZero() {
            assertThat(Checkpoint.INITIAL.value())
                    .as("The initial checkpoint should have a value of zero")
                    .isZero();
        }
    }

    @Nested
    class Creation {

        @Test
        void ofWithValueOneCreatesCheckpointWithValueOne() {
            int validValue = 1;

            Checkpoint checkpoint = Checkpoint.of(validValue);

            assertThat(checkpoint.value())
                    .as("Checkpoint created with value 1 should have value 1")
                    .isOne();
        }
    }

    @Nested
    class Validation {

        @Test
        void ofWithNegativeValueThrowsException() {
            int negativeValue = -1;

            assertThatIllegalArgumentException()
                    .as("Creating a Checkpoint with a negative value should throw an IllegalArgumentException")
                    .isThrownBy(() -> Checkpoint.of(negativeValue))
                    .withMessage("Checkpoint value must be 1 or more.");
        }

        @Test
        void ofWithZeroValueThrowsException() {
            int zeroValue = 0;

            assertThatIllegalArgumentException()
                    .as("Creating a Checkpoint with a value of 0 via of() should throw an IllegalArgumentException")
                    .isThrownBy(() -> Checkpoint.of(zeroValue))
                    .withMessage("Checkpoint value must be 1 or more, or use the constant Checkpoint.INITIAL for 0.");
        }
    }

    @Nested
    class Comparison {

        @Test
        void newerThanReturnsTrueForGreaterValue() {
            Checkpoint older = Checkpoint.INITIAL;
            Checkpoint newer = Checkpoint.of(1);

            assertThat(newer.newerThan(older))
                    .as("A checkpoint with value 1 should be newer than the initial checkpoint (0)")
                    .isTrue();
        }

        @Test
        void newerThanReturnsFalseForEqualValues() {
            Checkpoint checkpoint = Checkpoint.of(1);

            assertThat(checkpoint.newerThan(checkpoint))
                    .as("A checkpoint should not be newer than itself")
                    .isFalse();
        }

        @Test
        void newerThanReturnsFalseForLowerValue() {
            Checkpoint older = Checkpoint.INITIAL;
            Checkpoint newer = Checkpoint.of(1);

            assertThat(older.newerThan(newer))
                    .as("The initial checkpoint (0) should not be newer than a checkpoint with value 1")
                    .isFalse();
        }
    }

    @Nested
    class Equality {

        @Test
        void checkpointsWithSameValueAreEqual() {
            int checkpointValue = 10;
            Checkpoint checkpoint1 = Checkpoint.of(checkpointValue);
            Checkpoint checkpoint2 = Checkpoint.of(checkpointValue);

            assertThat(checkpoint1)
                    .as("Two checkpoints with the same value (10) should be equal")
                    .isEqualTo(checkpoint2)
                    .as("Two checkpoints with the same value (10) should have the same hash code")
                    .hasSameHashCodeAs(checkpoint2);
        }

        @Test
        void initialCheckpointIsEqualToItself() {
            assertThat(Checkpoint.INITIAL)
                    .as("The INITIAL constant should be equal to itself")
                    .isEqualTo(Checkpoint.INITIAL);
        }
    }

    @Nested
    class Observability {

        @Test
        void toStringFormatsInitialWithSpecialSuffix() {
            assertThat(Checkpoint.INITIAL.toString())
                    .as("The INITIAL checkpoint toString should include its special status")
                    .isEqualTo("Checkpoint(0/INITIAL)");
        }

        @Test
        void toStringFormatsStandardValue() {
            int checkpointValue = 5;
            Checkpoint checkpoint = Checkpoint.of(checkpointValue);

            assertThat(checkpoint.toString())
                    .as("Standard checkpoint toString should show the value in parentheses")
                    .isEqualTo("Checkpoint(5)");
        }
    }
}
