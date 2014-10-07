package org.sfm.csv.parser;

import java.io.IOException;
import java.io.Reader;

public final class ReaderCsvParser {

	private char[] buffer;

	private int bufferLength;
	private CsvParserState currentState = CsvParserState.NONE;

	private int currentStart = 0;
	private int bufferOffset = 0;

	public ReaderCsvParser(final int bufferSize) {
		buffer = new char[bufferSize];
	}

	/**
	 * parse cvs from input stream assumes character encoding for '"', ',' and
	 * '\n' match utf8
	 * 
	 * @param is
	 * @param handler
	 * @return
	 * @throws IOException
	 */
	public void parse(final Reader is, final CharsCellHandler handler)
			throws IOException {
		while ((bufferLength = is.read(buffer, bufferOffset, buffer.length
				- bufferOffset)) != -1) {
			bufferLength += bufferOffset;

			if (!consumeBytes(handler)) {
				return;
			}
			shiftBuffer();
		}

		if (bufferOffset > 0) {
			handler.newCell(buffer, 0, bufferOffset);
		}
		handler.end();
	}

	private boolean consumeBytes(final CharsCellHandler handler) {
		for (int i = 0; i < bufferLength; i++) {
			char c = buffer[i];
			if (!handleChar(handler, c, i)) {
				return false;
			}
		}
		return true;
	}

	private boolean handleChar(final CharsCellHandler handler, final char c,
			final int i) {
		if (c == '"') {
			quote(i);
		} else if (c == ',') {
			if (currentState != CsvParserState.IN_QUOTE) {
				newCell(handler, i);
			}
		} else if (c == '\n') {
			if (currentState != CsvParserState.IN_QUOTE) {
				newCell(handler, i);
				return handler.endOfRow();
			}
		}
		return true;
	}

	public void quote(final int i) {
		if (currentStart == i) {
			currentState = CsvParserState.IN_QUOTE;
		} else {
			if (currentState == CsvParserState.IN_QUOTE) {
				currentState = CsvParserState.QUOTE;
			} else if (currentState == CsvParserState.QUOTE) {
				currentState = CsvParserState.IN_QUOTE;
			}
		}
	}

	public void newCell(final CharsCellHandler handler, final int i) {
		handler.newCell(buffer, currentStart, i - currentStart);
		currentStart = i + 1;
		currentState = CsvParserState.NONE;
	}

	private void shiftBuffer() {
		// shift buffer consumer data
		bufferOffset = bufferLength - currentStart;

		// if buffer tight double the size
		if (bufferOffset > bufferLength >> 1) {
			// double buffer size
			char[] newbuffer = new char[buffer.length << 1];
			System.arraycopy(buffer, currentStart, newbuffer, 0, bufferOffset);
			buffer = newbuffer;
		} else {
			System.arraycopy(buffer, currentStart, buffer, 0, bufferOffset);
		}
		currentStart = 0;
	}
}
