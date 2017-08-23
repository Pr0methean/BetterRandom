package betterrandom.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import org.testng.annotations.Test;

public class WeakReferenceWithEqualsTest {

  private static final String S1 = "foo";
  private static final String S2 = "bar";

  @Test
  public void testHashCode() throws Exception {
    WeakReferenceWithEquals<String> ref1S1 = new WeakReferenceWithEquals<>(S1);
    WeakReferenceWithEquals<String> ref2S1 = new WeakReferenceWithEquals<>(S1);
    WeakReferenceWithEquals<String> refS2 = new WeakReferenceWithEquals<>(S2);
    assertEquals(ref1S1.hashCode(), ref2S1.hashCode());
    assertNotEquals(ref1S1.hashCode(), refS2.hashCode());
    assertNotEquals(ref2S1.hashCode(), refS2.hashCode());
  }

  @Test
  public void testEquals() throws Exception {
    WeakReferenceWithEquals<String> ref1S1 = new WeakReferenceWithEquals<>(S1);
    WeakReferenceWithEquals<String> ref2S1 = new WeakReferenceWithEquals<>(S1);
    WeakReferenceWithEquals<String> refS2 = new WeakReferenceWithEquals<>(S2);
    assertEquals(ref1S1, ref2S1);
    assertNotEquals(ref1S1, refS2);
    assertNotEquals(ref2S1, refS2);
  }

}