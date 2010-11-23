package org.lds.sso.appwrap.io;

import java.io.IOException;
import java.io.OutputStream;

public class ChainingOutputStream extends OutputStream {
	private OutputStream curr;
	private OutputStream next;
	
	public ChainingOutputStream(OutputStream curr, OutputStream next) {
		this.curr = curr;
		this.next = next;
	}
	
	@Override
	public void write(int b) throws IOException {
		curr.write(b);
		next.write(b);
	}
	
	public void close() throws IOException {
		curr.close();
		next.close();
	}
}
