import io.github.pr0methean.betterrandom.prng.Pcg128Random;

public class Dumper {
  public static void main(String[] args) {
    Pcg128Random random = new Pcg128Random();
    for (int i=0; i<64; i++) {
      System.out.println(
          String.format("%64s", Long.toBinaryString(random.nextLong()))
              .replace(' ', '0'));
    }
  }
}
