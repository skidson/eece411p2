package ca.ubc.ece.crawler;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

public class TestDriver {

	public static void main(String[] args) {
		final int RANGE = 2000;
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
	
	private static Vector<Extension> jeffisAwesomeSort(Vector<Extension> ext){
		for(int i = 0; i < ext.size(); i++){
			for(int j = 0; j < ext.size(); j++){
				if(ext.get(i).getCount() > ext.get(j).getCount())
					Collections.swap(ext, i, j);
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
		/*Vector<Extension> temp = new Vector<Extension>();
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
		return(temp);*/
		
		// OLDSORT
		int index = 1;
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
	
	private static class Score {
		public long time;
		public String player;
		
		public Score(long time, String player) {
			this.time = time;
			this.player = player;
		}
	}
}
