// ============================================================================
//   Copyright 2006-2012 Daniel W. Dyer
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
// ============================================================================
package betterrandom.prng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.testng.annotations.Test;

/**
 * Unit test for the cellular automaton RNG.
 *
 * @author Daniel Dyer
 */
public class CellularAutomatonRandomTest {
  @Test(timeOut = 15000)
  public void testSerializable() throws IOException, ClassNotFoundException {
    // Serialise an RNG.
    CellularAutomatonRandom rng = new CellularAutomatonRandom();
    CellularAutomatonRandom result;
    try (
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
      objectOutStream.writeObject(rng);
      byte[] serialCopy = byteOutStream.toByteArray();
      // Read the object back in.
      try (ObjectInputStream objectInStream = new ObjectInputStream(
          new ByteArrayInputStream(serialCopy))) {
        result = (CellularAutomatonRandom) (objectInStream.readObject());
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    result.nextLong();
  }
}
