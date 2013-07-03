package com.sedric;

import java.util.Arrays;

public class InsertSpace {

	public static void insertSpace(String[] arr, int spaceNum, int startPos, StringBuilder sb) {
		if (spaceNum == 0) {
			for (int i = 0; i < arr.length; i++) {
				sb.append(arr[i]);
			}
			System.out.println(sb);
		} else {
			for (; startPos < arr.length - spaceNum; startPos++) {
				StringBuilder copySb = new StringBuilder(sb.toString());
				for (int i = 0; i < startPos; i++) {
					copySb.append(arr[i]);
				}
				if (spaceNum == 1) {
					for (int i = startPos; i < arr.length; i++) {
						if (i == startPos) {
							copySb.append(arr[i]).append("=");
						} else {
							copySb.append(arr[i]);
						}
					}
					System.out.println(copySb.toString());
				} else {
					copySb.append(arr[startPos]).append("=");
					insertSpace(Arrays.copyOfRange(arr, startPos + 1, arr.length), spaceNum - 1, 0, copySb);
				}
			}
		}
	}

	public static void main(String[] args) {
		String[] arr = new String[] { "a", "b", "c", "d", "e", "f", "g" };
		for (int spaceNum = 0; spaceNum < arr.length; spaceNum++) {
			insertSpace(arr, spaceNum, 0, new StringBuilder());
		}
	}
}
