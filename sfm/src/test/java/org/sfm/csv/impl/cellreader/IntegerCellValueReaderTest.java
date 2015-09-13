package org.sfm.csv.impl.cellreader;

import org.junit.Test;
import org.sfm.csv.impl.ParsingException;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IntegerCellValueReaderTest {

	IntegerCellValueReaderImpl reader = new IntegerCellValueReaderImpl();
	@Test
	public void testReadInt() throws UnsupportedEncodingException {
		testReadInt(0);
		testReadInt(12345);
		testReadInt(-12345);
		testReadInt(Integer.MIN_VALUE);
		testReadInt(Integer.MAX_VALUE);
	}
	
	@Test
	public void testInvalidInt() throws UnsupportedEncodingException {
		final char[] chars = "Nan".toCharArray();
		try {
			reader.read(chars, 0, chars.length, null);
			fail("Expect exception");
		} catch(ParsingException e){
			// expected
		}
	}

	@Test
	public void testReadIntWithSpace() {
		assertEquals(12345, readInt(" 1 2 345"));
	}

	@Test
	public void testReadEmptyStringReturnNull() {
		assertNull(reader.read(new char[10], 2, 0, null));
	}

	private void testReadInt(int i) throws UnsupportedEncodingException {
		assertEquals(i, readInt(Integer.toString(i)));
	}

	private int readInt(String string) {
		final char[] chars = ("_" + string + "_").toCharArray();
		return reader.read(chars, 1, chars.length - 2, null).intValue();
	}

}
