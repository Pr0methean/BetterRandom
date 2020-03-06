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
package io.github.pr0methean.betterrandom.prng.randomness;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import org.testng.annotations.Test;

/**
 * Unit test for the {@link RandomFifoFiller} class.
 *
 * On Windows, this test requires Cygwin.
 *
 * @author Daniel Dyer
 */
public class RandomFifoFillerTest
{
    /**
     * Make sure that the input file is created and that it is the correct size.
     */
    @Test(timeOut = 60000)
    public void testFileCreation() throws Exception
    {
        final String tempPipeName = System.getProperty("java.io.tmpdir") + "/diehard-input";
        File tempPipe = new File(tempPipeName);
        assertTrue(!tempPipe.exists() || tempPipe.delete(),
            "Temporary pipe already exists and can't be deleted! "
                + "(This test cannot run multiple times in parallel.)");
        try {
            Process mkfifo =
                Runtime.getRuntime().exec(new String[]{"/usr/bin/mkfifo", tempPipeName});
            mkfifo.waitFor();
            Process consumer = Runtime.getRuntime().exec(new String[]{
                "/usr/bin/xxd", "-l", "1000", tempPipeName});
            RandomFifoFiller.main(new String[]{
                "io.github.pr0methean.betterrandom.prng.Pcg64Random", tempPipeName});
            consumer.waitFor();
            assertEquals(consumer.exitValue(), 0, "Error consuming the random number stream");
        } finally {
            if (!tempPipe.delete()) {
                tempPipe.deleteOnExit();
            }
        }
    }
}
