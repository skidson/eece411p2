package ca.ubc.ece.crawler;

public class InterruptAction implements Action {
	Thread thread;
	
	public InterruptAction(Thread thread) {
		this.thread = thread;
	}
	
	public void execute() {
		this.thread.interrupt();
	}
}