package io.github.pr0methean.betterrandom.seed;

import static io.github.pr0methean.betterrandom.TestUtils.assertEqualAfterSerialization;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import io.github.pr0methean.betterrandom.util.BinaryUtils;
import java.net.Proxy;
import java.time.Duration;
import javax.net.ssl.SSLSocketFactory;
import org.testng.annotations.Test;

@SuppressWarnings("ThrowableNotThrown")
public class AnuQuantumSeedClientHermeticTest
    extends WebSeedClientHermeticTest<AnuQuantumSeedClient> {

  private static final byte[] RESPONSE_2048
      = ("{\"type\":\"string\",\"length\":2,\"size\":1024," +
      "\"data" +
      "\":[\"112d9366cc501ba4170d49df17775d5dbf774321382430cba4d3c205e54e419a2a5a6a9f74676766e853" +
      "d0234a8292068821fc47dc29cbe2e2c48cde5e6b85f494026911992ec791fdb91df95dbc836b16130126018e17" +
      "060a49a20f02489a1a8a1d5c34d1107811eecd4b2acca00f94a279789b2d2c264aec11c7f63ed71d3841a521b3" +
      "1ef40a53372308dbb14902a793693be52f183102517ac82bca59d1fa9486f5bfecf303a7db7ac39de83325ae30" +
      "3de312968995abef0baefee1382d41dbf230ee01a0736113be6bdf54830ab55b45b4683b818e9d817ec8b2bfb0" +
      "4721497b6c559723650458de71b88bb5c11eb627f8c6141d0563b1e9504c18bc8d6705282b088c262bfc2ba839" +
      "2caea4765da0cba801f905244baec0d12fab9baefedca84f7ec64dd6cc694235f2ab361e7567d30a58ba8431c0" +
      "12f7367e887e8d62ec82835decef4dd4c0bdbfb010fe12d774c06574ef6ca8353ec34646be4a494c90186dfcf5" +
      "458d0a0a9de030e95b55007c559504c4e60379ae40d7c11b589b7b76e58f1426f6478273ee54122f745209f8c7" +
      "279b3bfb220ca870f55c9cfab1a0bdc7ebe408d9fec3a5faa763d3d8eae29d370d8149633c2ac1b6f3245383e5" +
      "0664c4ae268af84111d9326a71416dea5633b67b0ec59ee5624eddb5faad793781382e58dc057f328b60f9a54f" +
      "95fffc7773912cfbe4113da6cbf33fec8746b69799e0a50fc19a5d38f14a4acee0de1eefe1a8c3cbcc6c0b5abf" +
      "7723440ef9539ea6c3c62c78a25aa3df91ca2bf4092746460f3ae94ed9ed0953537914b3aeaeea2a9685737290" +
      "490328da0cf5da4fad961afd623a5d736792d5737958ccfbd1ebc5a0e00684ffc2ca4ea9fa1e782ec554552a12" +
      "a5796b75d22a313572e345bd2c243df1022c1a9549ab569e63957851923c0e3a6f32727bb8d0b49173550623e2" +
      "cc049eda68607a0133a005e067fbc510816f4a9ef62d1dc9dda6d7fbc593929bbae4e7ee9abd36350240878012" +
      "2161d99af23e489b6b45a831298b6bb5de85731c9a49bb289fe2f2d31e9bafdf071157159cbd9117c293b6db77" +
      "69de4716fe8086a23993f443237b689d6ccd352420c04fe9c58619652a6f4b8f5acd41f1ab08fbdc876e03c5a2" +
      "bd90bd0d046e65544e2613f19159effa49fb62da8f8d14bed38cd3132ea3fa1bbb145813afaf491d15e2f63bc9" +
      "5199e7e1879034d71e5cb84cc0bb66ef512e10bd82baf687ac135857b0056d60cdf3c08abd1b6868505fc8add2" +
      "3b84a3bb13466919f2ae8fe1f715a826421064450d4c8497f59c72dd3d4972d6ff239fd89aca798c247f514f79" +
      "de711e1115571a3aaf872235d811a23f1de981476c7745d330756ac17bb55bdb59b804ad030019fbbb0d8930e8" +
      "14e08c3033cc7c874acb74c0e70dc57a81c8dea5b8cbeaafe5feb2e5ab6653684ca2551f7b\"," +
      "\"1126cbe355d0a73c5cfb55d1117109351dca4c1c37845b4ccdc760559c00f31337f7800196533f7165b7e713" +
      "f4b2356ed7a6f2e84dc3ddbc1b836e47fd1965a8b294c0002b88d9bc99d53a17e9e1d7ba5578c59a7da29b4dfb" +
      "8c6fe62ec0f1252e4a488226f5e46ba3f584480eb88f36b43a41378caf4911538360ff07a889b11da748727955" +
      "6051ac63f22dd4f4740a789fe7cf48dd91bd3b3c21899223c85d3a22663559d722e469dbd6a77b09f6b9f48359" +
      "9e17662123ce3e5f37384165f4166aa1e9191d6420efda62febcfbeb228039f62d1dcc8f248af6550857183c3d" +
      "eddb3d0fcfc453b9fa154cd2dc0cdcf51602f64143d7e0b9b417427fa32a1021cb6b0b7aaa43059fde7a7db4ba" +
      "7b684f92548713d574397ba5becce524686c2f56e441e52ff23a2ff98254e34c8745db2b6c4ca1f871161146b1" +
      "7a5a7d08db7d38ad4d01577a33088df68a8538cc8dc7c707a05cebf4f2463b4ed377f0e288c20c88890f043b5d" +
      "89feba2cab8e0e92a6ea35f80add9e23b3a47f321f77767e530186a95c17eb19e764a02cfbb6dde5e6b56f0f77" +
      "241750116c0b1a042291448f8c13d98585b867ef6e1a5234208bb9c5533c1c4ba14e3aa167d472fcf98605070b" +
      "f70e99f1a463fa5e7b72ec1d12ea4ff4defb472b41bd731df8898de37563f648fcccb3c605ec254f9a67d27d7c" +
      "a9e83ccaf194fc56298e51a0ae6fc0414601dc38d522520a4f818797335d7456bcfb531654c8badd2b84104213" +
      "ba651b42a4e265ae23bbc4be47a8f83ce237b74267a58f0339997ca8ea7bce27861f41a82287699144a795b476" +
      "a51a8df6d24a483ab0d3c3adc29d4fc45b86b14f67dec74d6017beabe3735cc37a613c00673895ea7226e1db1b" +
      "7c7d5040a20513d973eb8a4c977bff18e905087f6cb0cdea7e739fe8ca80e2013fe63042f74dd7fe30004f4c99" +
      "a875dfa8b20811dc1b3878eebce61de9330d81264c65cc6d17250ee60abb09297f60b7709ad6d357b04612772a" +
      "d249b916cf39c16166a2577c22abb47681eb361b920b8612d6ecf6f54d62b3163af63218da0c531e06225020be" +
      "e5a3340a1f41ef7d251aac9c8a0f28063e55e68828bec10b43acb56612b92b4d34558b67bf147c382b8dc91580" +
      "3ab75e47c01330c381695141f57c5348fe74781f7062a5a9b69d6a4594a058c2067fb0068ea9de567172bec08d" +
      "74f1e868acc7acde070a2e3bf361998a11bb1b9cce6ef7549e9d448cb5b640866765e2150cb6f59179dc483499" +
      "81552d01d821f67c0a051b06570f39bb8e18b47e3383624ca775c5488487c665c6296cb830dd99b81165915287" +
      "9824e0a35819b06dec8596081517eac28b82dc0b6ae0f4d69385bca3aaef90e5fc1a75537d1438b52de5986c88" +
      "9e20faf7b6f82ac328eff6427d0804d78b1d20202dbc04e3d32a86e38a3873797d4b9b" +
      "\"],\"success\":true}").getBytes(UTF_8);
  private static final byte[] RESPONSE_32 = ("{\"type\":\"string\",\"length\":1," +
      "\"size\":32,\"data\":[\"0808446c6d2723c335e3adcf1186c062c1e15c86fb6f396f78ab162b7e28ad33" +
      "\"],\"success\":true}").getBytes(UTF_8);

  @Override protected byte[] get32ByteResponse() {
    return RESPONSE_32;
  }

  @Override protected AnuQuantumSeedClient getSeedGenerator(Proxy proxy,
      SSLSocketFactory socketFactory) {
    return new AnuQuantumSeedClient(new WebSeedClientConfiguration.Builder()
        .setProxy(proxy)
        .setSocketFactory(socketFactory)
        .build());
  }

  @Test public void testBasicUsage() {
    mockResponse(RESPONSE_32);
    SeedTestUtils.testGenerator(seedGenerator, false, 32);
  }

  @Test public void testOverShortResponse32() {
    mockResponse(RESPONSE_32);
    expectAndGetException(1024, false);
  }

  @Test public void testOverLongResponse32() {
    mockResponse(RESPONSE_32);
    expectAndGetException(16, false);
  }

  @Test public void testOverShortResponse2048() {
    mockResponse(RESPONSE_2048);
    expectAndGetException(3072, false);
  }

  @Test public void testOverLongResponse2048() {
    mockResponse(RESPONSE_2048);
    expectAndGetException(1024, false);
  }

  @Test public void testRandomFuzz() {
    // invocationCount spams the log, so use a loop
    for (int i = 0; i < 10_000; i++) {
      String fuzzOutput = BinaryUtils.convertBytesToHexString(fuzzResponse(RESPONSE_32.length));
      expectAndGetException(32, false, fuzzOutput);
    }
  }

  @Test public void testResponseNoData() {
    mockResponse("{\"type\":\"string\",\"length\":1," +
        "\"size\":32,\"success\":true}");
    expectAndGetException(32, false);
  }

  @Test public void testResponseWrongTypeData() {
    mockResponse("{\"type\":\"string\",\"length\":1," +
        "\"size\":32,\"data\":\"0808446c6d2723c335e3adcf1186c062c1e15c86fb6f396f78ab162b7e28ad33\"" +
        ",\"success\":true}");
    expectAndGetException(32, false);
  }

  @Test public void testInvalidHex() {
    mockResponse("{\"type\":\"string\",\"length\":1," +
        "\"size\":32,\"data\":[\"0808446c6d2723c335e3adcf1186c062c1e15c86fb6f396f78ab162b7e28adxx\"" +
        "],\"success\":true}");
    expectAndGetException(32);
  }

  @Override public void testSerializable() {
    assertEqualAfterSerialization(new AnuQuantumSeedClient());
    assertEqualAfterSerialization(new AnuQuantumSeedClient(
        new WebSeedClientConfiguration.Builder().setRetryDelay(Duration.ZERO).build()));
  }

  @Test public void testSetProxy() {
    seedGenerator = getSeedGenerator(proxy, null);
    mockResponse(RESPONSE_32);
    SeedTestUtils.testGenerator(seedGenerator, false, 32);
    assertNotNull(address, "No connection made when using proxy");
    assertTrue(address.startsWith("https://qrng.anu.edu.au"), "Wrong domain when proxy used");
  }
}
