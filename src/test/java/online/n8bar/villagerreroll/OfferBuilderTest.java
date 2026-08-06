package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferBuilderTest {
    @Test
    void fisherYatesUsesBoundedIndicesAndPreservesMembers() {
        List<Integer> values = new ArrayList<>(List.of(1, 2, 3, 4));
        List<Integer> bounds = new ArrayList<>();
        OfferBuilder.shuffle(values, bound -> {
            bounds.add(bound);
            return 0;
        });
        assertEquals(List.of(2, 3, 4, 1), values);
        assertEquals(List.of(4, 3, 2), bounds);
    }
}
