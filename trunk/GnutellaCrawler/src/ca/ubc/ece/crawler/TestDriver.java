/*	Name: TestDriver.java
 * 		- storage object for node information
 * 
 * 	Authors:	Stephen Kidson - #15345077
 * 				David Lo - #20123071
 * 				Jeffrey Payan - #18618074
 * 				
 * 	Last updated: November 17, 2010
 */

package ca.ubc.ece.crawler;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

public class TestDriver {

	public static void main(String[] args) {
		final int RANGE = 2;
		long t0, t1;
		Vector<Extension> unsorted = new Vector<Extension>();
		Vector<Extension> sorted = new Vector<Extension>();
		Vector<Score> scores = new Vector<Score>();
		
		for (int i = 0; i < RANGE; i++) {
			Extension ext = new Extension(generateExt());
			if (!ext.containedIn(unsorted))
				unsorted.add(ext);
		}

		System.out.println("\n********************* BEFORE *********************");
		print(unsorted);
		
		System.out.println("\n********************* STEVESORT *********************");
		t0 = System.nanoTime();
		sorted = steveSort((Vector<Extension>)unsorted.clone());
		t1 = System.nanoTime();
		print(sorted);
		System.out.println("Execution time: " + (t1-t0) + "ns");
		scores.add(new Score(t1-t0, "Stephen"));
		
		System.out.println("\n********************* JEFFSORT *********************");
		t0 = System.nanoTime();
		sorted = jeffisAwesomeSort((Vector<Extension>)unsorted.clone());
		t1 = System.nanoTime();
		print(sorted);
		System.out.println("Execution time: " + (t1-t0) + "ns");
		scores.add(new Score(t1-t0, "Jeff"));
		
		System.out.println("\n********************* STATISTICS *********************");
		double fastest = -1, slowest= -1;
		String winner = "", loser= "";
		int j = 0;
		for (int i = 0; i < scores.size(); i++) {
			if (scores.get(i).time < fastest || fastest == -1) {
				fastest = scores.get(i).time;
				winner = scores.get(i).player;
			}
			if (scores.get(i).time > slowest || slowest == -1) {
				slowest = scores.get(i).time;
				loser = scores.get(i).player;	
			}
		}
		System.out.println("Winner is " + winner + "!");
		System.out.println(winner + " beat " + loser + " by " + (slowest-fastest) + "ns!");
		System.out.println(winner + " is " + slowest/fastest + " times better than " + loser);
	}
	
	public static Vector<Extension> jeffisAwesomeSort(Vector<Extension> ext) {
	    int firstOutOfOrder, location;
	    Extension temp;
	    for(firstOutOfOrder = 1; firstOutOfOrder < ext.size(); firstOutOfOrder++) { 
	        if(ext.get(firstOutOfOrder).getCount() > ext.get(firstOutOfOrder - 1).getCount()) {
	            temp = ext.get(firstOutOfOrder);
	            location = firstOutOfOrder;
	            do {
	            	ext.setElementAt(ext.get(location-1), location);
	                location--;
	            } while (location > 0 && ext.get(location-1).getCount() < temp.getCount());
	            ext.setElementAt(temp, location);
	        }
	    }
	    return ext;
	}
	
	private static Vector<Extension> steveSort(Vector<Extension> list) {
		// SHITSORT
		/*Vector<Extension> temp = new Vector<Extension>();
		if (list.size() < 2) return list;
		int index = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getCount() > list.get(index).getCount())
				index = i;
		}
		temp.add(list.get(index));
		list.removeElementAt(index);
		temp.addAll(steveSort(list));
		return(temp);*/
		
		// DIRTYSORT
		Vector<Extension> temp = new Vector<Extension>();
		int cacheIndex = 0;
		int max = 0, num = 0;
		int maxIndex;
		
		while(!list.isEmpty()) {
			maxIndex = 0;
			max = 0;
			for(int i = (cacheIndex%list.size()); i < list.size(); i++) {
				num = list.get(i).getCount();
				if (num > max) {
					max = num;
					cacheIndex = maxIndex;
					maxIndex = i;
				}
			}
			temp.add(list.get(maxIndex));
			list.removeElementAt(maxIndex);
		}
		return(temp);
		
		// OLDSORT
		/*int index = 1;
		while(true) {
			if (index == 0)
				index++;
			
			if (list.get(index).getCount() > list.get(index-1).getCount()) {
				Collections.swap(list, index, index-1);
				index--;
			} else if (index == list.size() - 1) {
				break;
			} else {
				index++;
			}
		}
		return(list);*/
		
	}
	
	private static String generateExt() {
		String[] extensions = {"lol", "wtf", "nou", "fag", "ass", "slt", "cnt", "bch", "pro", "nub", "hoe", "pwn", "sknk", "jew", "hat", "bllz", "nig", "azn"};
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
	
	private static class Score {
		public long time;
		public String player;
		
		public Score(long time, String player) {
			this.time = time;
			this.player = player;
		}
	}
}
