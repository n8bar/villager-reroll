package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RerollValidationTest {
    @Test
    void acceptsExactlyTwoNonNullOffersPerLevel() {
        assertTrue(RerollValidation.hasExactOffers(List.of(1, 2, 3, 4), 2));
    }

    @Test
    void rejectsWrongCountNullsAndInvalidLevel() {
        assertFalse(RerollValidation.hasExactOffers(List.of(1, 2, 3), 2));
        assertFalse(RerollValidation.hasExactOffers(Arrays.asList(1, null), 1));
        assertFalse(RerollValidation.hasExactOffers(List.of(), 0));
    }
}
