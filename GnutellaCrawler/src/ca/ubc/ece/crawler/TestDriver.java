package ca.ubc.ece.crawler;

import java.util.Random;
import java.util.Vector;

public class TestDriver {

	public static void main(String[] args) {
		final int RANGE = 20;
		Vector<Extension> sortMe = new Vector<Extension>();
		for (int i = 0; i < RANGE; i++) {
			Extension ext = new Extension(generateExt());
			if (!ext.containedIn(sortMe))
				sortMe.add(ext);
		}
		
		System.out.println("\n********************* BEFORE *********************");
		print(sortMe);
		
		System.out.println("\n********************* AFTER ********************s*");
		print(quicksort(sortMe));
	}
	
	private static Vector<Extension> quicksort(Vector<Extension> list) {
			if (list.size() > 2)
				return list;
		
			Vector<Extension> less = new Vector<Extension>();
			Vector<Extension> greater = new Vector<Extension>();
			
			int pivot = list.size()/2;
			
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).getCount() <= list.get(pivot).getCount())
					less.add(list.get(i));
				else
					greater.add(list.get(i));
			}
			
			greater = quicksort(greater);
			less = quicksort(less);
			greater.add(list.get(pivot));
			greater.addAll(less);
			
			return(greater);
	}
	
	private static String generateExt() {
		String[] extensions = { "lol", "wtf", "nou", "fag", "ass", "slt", "cnt", "bch"};
		return(extensions[(new Random()).nextInt(extensions.length)]);
	}
	
	private static void print(Vector<Extension> list) {
		System.out.println("File Extension \tNumber of Occurences");
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getName().length() < 5)
				System.out.println("." + list.get(i).getName() + "\t\t\t" + list.get(i).getCount());
			else
				System.out.println("." + list.get(i).getName() + "\t\t" + list.get(i).getCount());
		}
	}
}
