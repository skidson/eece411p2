package ca.ubc.ece.crawler;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

public class TestDriver {

	public static void main(String[] args) {
		final int RANGE = 2000;
		long time;
		Vector<Extension> sortMe = new Vector<Extension>();
		for (int i = 0; i < RANGE; i++) {
			Extension ext = new Extension(generateExt());
			if (!ext.containedIn(sortMe))
				sortMe.add(ext);
		}
		
		System.out.println("\n********************* BEFORE *********************");
		time = System.nanoTime();
		print(sortMe);
		System.out.println("Execution time: " + (System.nanoTime() - time) + "ms");
		
		System.out.println("\n********************* STEVESORT *********************");
		time = System.nanoTime();
		print(steveSort(sortMe));
		System.out.println("Execution time: " + (System.nanoTime() - time) + "ns");
		
		System.out.println("\n********************* JEFFSORT *********************");
		time = System.nanoTime();
		print(jeffisAwesomeSort(sortMe));
		System.out.println("Execution time: " + (System.nanoTime() - time) + "ns");
	}
	
	private static Vector<Extension> jeffisAwesomeSort(Vector<Extension> ext){
		for(int i = 0; i < ext.size(); i++){
			for(int j = 0; j < ext.size(); j++){
				if(ext.get(0).getCount() > ext.get(j).getCount())
					Collections.swap(ext, i, j);
			}
		}
		return ext;
	}
	
	private static Vector<Extension> steveSort(Vector<Extension> list) {
		int index = 1;
		while(true) {
			if (index == 0) {
				index++;
			} else if (list.get(index).getCount() > list.get(index-1).getCount()) {
				Collections.swap(list, index, index-1);
				index--;
			} else if (index == list.size() - 1) {
				break;
			} else {
				index++;
			}
		}
		return(list);
	}
	
	private static String generateExt() {
		String[] extensions = {"lol", "wtf", "nou", "fag", "ass", "slt", "cnt", "bch", "pro", "nub", "hoe"};
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
