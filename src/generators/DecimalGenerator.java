package generators;

import java.util.Random;

/**
 * 
 * @author Brandon Kindred
 *
 */
public class DecimalGenerator implements IFieldGenerator<Double> {

	public DecimalGenerator() {
	}

	public Double generateValue() {
		Random random = new Random();
		return Double.parseDouble(random.nextLong() + "." + random.nextLong());
	}

}
