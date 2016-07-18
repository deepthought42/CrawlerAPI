package com.minion.generators;

import java.util.Random;

/**
 * 
 * @author Brandon Kindred
 *
 * @param <T>
 */
public class IntegerGenerator implements IFieldGenerator<Long> {

	public IntegerGenerator() {
	}

	public Long generateValue() {
		Random random = new Random();
		return random.nextLong();
	}
}
