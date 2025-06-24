package controllers.certs;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.List;

public class StorageTypeTest {

    @Test
    public void get_ReturnsAllCloudStorageTypesAsStrings() {
        List<String> types = StorageType.get();
        assertTrue(types.contains("aws"));
        assertTrue(types.contains("azure"));
        assertEquals(StorageType.CloudStorageType.values().length, types.size());
    }

    @Test
    public void cloudStorageType_EnumContainsExpectedValues() {
        assertNotNull(StorageType.CloudStorageType.valueOf("aws"));
        assertNotNull(StorageType.CloudStorageType.valueOf("azure"));
    }

    @Test
    public void get_ReturnsUnmodifiableListReference() {
        List<String> types1 = StorageType.get();
        List<String> types2 = StorageType.get();
        assertSame(types1, types2);
    }
}
