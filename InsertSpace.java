package com.sedric.math;

public class InsertSpace {

	public static void insertSpace(String[] arr, int spaceNum, int startPosition, StringBuilder sb) {
		int length = arr.length;
		if (spaceNum == 1) {
			for(int i = startPosition; i < length; i++) {
				if (i == startPosition) {
					sb.append(arr[i]).append("    ");
				} else {
					sb.append(arr[i]);
				}
			}
			System.out.println(sb.toString());
		} else {
			sb.append(arr[startPosition]).append("    ");
			if (spaceNum >= 1) {
				insertSpace(arr, spaceNum - 1, startPosition + 1, sb);
			}
		}
	}

	public static void main(String[] args) {
		String[] arr = new String[] { "a", "b", "c", "d", "e" };
		for(int spaceNum = 1; spaceNum < arr.length; spaceNum++) {
			for(int startPosition = 0; startPosition < arr.length - spaceNum; startPosition++) {
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < startPosition; i++) {
					sb.append(arr[i]);
				}
				insertSpace(arr, spaceNum, startPosition, sb);
			}
		}

	}
}
