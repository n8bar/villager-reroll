package online.n8bar.villagerreroll;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RerollPlannerTest {
    @Test void preservedTierKeepsExactObjectsAndChronologicalOrdering() {
        Object old1=new Object(), old2=new Object(), old3=new Object();
        Object fresh1=new Object(), fresh2=new Object();
        List<Object> result=RerollPlanner.splice(List.of(old1,old2,old3),new int[]{2,1},
                Map.of(1,Optional.of(List.of(fresh1,fresh2)),2,Optional.empty()),2);
        assertEquals(List.of(fresh1,fresh2,old3),result);
        assertSame(old3,result.get(2));
    }
}
