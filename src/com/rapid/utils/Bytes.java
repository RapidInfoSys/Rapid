/*

Copyright (C) 2015 - Gareth Edwards / Rapid Information Systems

gareth.edwards@rapid-is.co.uk


This file is part of the Rapid Application Platform

RapidSOA is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. The terms require you
to include the original copyright, and the license notice in all redistributions.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
in a file named "COPYING".  If not, see <http://www.gnu.org/licenses/>.

*/

package com.rapid.utils;

public class Bytes {

	// public static finals

	public static final byte[] DOUBLE_BREAK_BYTES = {13,10,13,10}; //  used to check boundary on upload

	public static int findPattern(byte[] bytes, byte[] pattern, int position, int maxCheck) {

		int i,j;

		// do we have bytes in our arrays and is the position low enough to find something
		if (bytes.length > 0 && pattern.length > 0 && position >=0 && position <= bytes.length - pattern.length) {

			// loop from start position to max check (default to bytes.length)
			for (i = position; i < position + maxCheck; i++) {

				// if we matched on the first byte of the pattern and have enough left to find the whole thing
				if (bytes[i] == pattern[0] && i + pattern.length < bytes.length) {

					// check remaining bytes in pattern
					for (j = 1; j < pattern.length; j++) {

						// bail early if we fail to match the pattern
						if (bytes[i + j] != pattern[j]) break;

					}

					// if we got to the end of the pattern we're there!
					if (j == pattern.length) return i;

				}

			}

		}

		return -1;

	}

	//	override to start at position and check everything
	public static int findPattern(byte[] bytes, byte[] pattern, int position) {
		return findPattern(bytes, pattern, position, bytes.length);
	}

	// override to start at the beginning and check everything
	public static int findPattern(byte[] bytes, byte[] pattern) {
		return findPattern(bytes, pattern, 0, bytes.length);
	}

	// converts a hex representation of bytes to a byte array - useful for checking file signatures
	public static byte[] fromHexString(String hexString) {
		// length of string
		int length = hexString.length();
		// byte array, half length of string
		byte[] bytes = new byte[length / 2];
		// loop length 2 increments at a time
		for (int i = 0; i < length; i += 2) {
			// use chars and left shift to determine bytes
			bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		}
		return bytes;
	}


}
