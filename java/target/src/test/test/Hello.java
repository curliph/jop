package test;

import util.*;

public class Hello {

	public static void main(String[] agrgs) {

		System.out.println("Hello World from JOP!");
		for (;;) {
			Timer.wd();
			int i = Timer.getTimeoutMs(1000);
			while (!Timer.timeout(i)) {
				;
			}
		}
	}
}
