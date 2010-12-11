package ca.ubc.ece.crawler;

public class Timer implements Runnable {
	private Action action;
	private int delay;
	
	public Timer(Action action, int delay) {
		this.action = action;
		this.delay = delay;
	}
	
	public void run() {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) { /* Forcibly quit */ }
		action.execute();
	}
}
