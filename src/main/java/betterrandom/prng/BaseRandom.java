package betterrandom.prng;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

public abstract class BaseRandom implements Serializable {

  byte[] seed;

  BaseRandom(byte[] seed) {
    this.seed = seed.clone();
  }

  void checkedReadObject(BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    assert seed != null : "@AssumeAssertion(nullness)";
  }

  private void readObject(BaseRandom this,
      ObjectInputStream in) throws IOException, ClassNotFoundException {
    checkedReadObject(in);
  }

  abstract static class BaseEntropyCountingRandom extends BaseRandom {

    final AtomicLong entropyBits = new AtomicLong(0);

    BaseEntropyCountingRandom(byte[] seed) {
      super(seed);
    }

    @Override
    void checkedReadObject(BaseEntropyCountingRandom this,
        ObjectInputStream in)
        throws IOException, ClassNotFoundException {
      super.checkedReadObject(in);
      assert entropyBits != null : "@AssumeAssertion(nullness)";
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      checkedReadObject(in);
    }
  }

  public static class CellularAutomatonRandom extends BaseEntropyCountingRandom {

    CellularAutomatonRandom(byte[] seed) {
      super(seed);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      checkedReadObject(in);
    }
  }

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    CellularAutomatonRandom rng = new CellularAutomatonRandom(new SecureRandom().generateSeed(4));
    CellularAutomatonRandom result;
    try (ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
      objectOutStream.writeObject(rng);
      byte[] serialCopy = byteOutStream.toByteArray();
      // Read the object back in.
      try (ObjectInputStream objectInStream = new ObjectInputStream(
          new ByteArrayInputStream(serialCopy))) {
        result = (CellularAutomatonRandom) (objectInStream.readObject());
      }
    }
    System.out.println(result);
  }
}
